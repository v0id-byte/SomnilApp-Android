package com.somnil.app.service

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import androidx.core.content.ContextCompat
import com.somnil.app.domain.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * BLE Manager for Somnil device - mirrors iOS BLEManager.
 * Nordic UART Service protocol (same UUIDs as iOS).
 */
@Singleton
class BLEManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        // Nordic UART Service UUIDs (same as iOS)
        val SERVICE_UUID: UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
        val TX_CHAR_UUID: UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")
        val RX_CHAR_UUID: UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")

        const val SOMNIL_PREFIX = "Somnil-001"
        const val OPENBCI_PREFIX = "OpenBCI-Ganglion-"
    }

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val adapter: BluetoothAdapter? = bluetoothManager?.adapter
    private var scanner: BluetoothLeScanner? = null
    private var peripheral: BluetoothDevice? = null
    private var gatt: BluetoothGatt? = null
    private var txChar: BluetoothGattCharacteristic? = null
    private var rxChar: BluetoothGattCharacteristic? = null

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = _discoveredDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _lastPacket = MutableSharedFlow<SomnilPacket>(replay = 1)
    val lastPacket: SharedFlow<SomnilPacket> = _lastPacket.asSharedFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var autoReconnectJob: Job? = null
    private var rssiJob: Job? = null
    private var lastPeripheralAddress: String? = null

    // Reconnect state machine
    private var reconnectAttempt = 0
    private val maxReconnectAttempts = 5
    private val baseReconnectDelayMs = 1000L  // 1 second base delay
    private val maxReconnectDelayMs = 30000L  // 30 seconds max delay

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val name = device.name ?: result.scanRecord?.deviceName ?: return

            if (!name.contains(SOMNIL_PREFIX) && !name.contains(OPENBCI_PREFIX)) return

            val discovered = DiscoveredDevice(
                id = device.address,
                name = name,
                rssi = result.rssi,
                peripheral = device
            )
            _discoveredDevices.update { devices ->
                devices.filter { it.id != discovered.id } + discovered
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    cancelAutoReconnect()
                    _connectionState.value = ConnectionState.Connected(
                        peripheral?.name ?: "Device",
                        -50
                    )
                    startRSSIUpdates()
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    stopRSSIUpdates()
                    if (status != 0) {
                        // Unexpected disconnect - start reconnect state machine
                        startAutoReconnect()
                    } else {
                        // Intentional disconnect
                        _connectionState.value = ConnectionState.Disconnected
                    }
                    txChar = null
                    rxChar = null
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) return
            val service = gatt.getService(SERVICE_UUID) ?: return
            txChar = service.getCharacteristic(TX_CHAR_UUID)
            rxChar = service.getCharacteristic(RX_CHAR_UUID)

            // Enable notifications on TX characteristic (device -> phone)
            txChar?.let {
                gatt.setCharacteristicNotification(it, true)
                val descriptor = it.getDescriptor(
                    UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
                )
                descriptor?.let { d ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        gatt.writeDescriptor(d, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                    } else {
                        @Suppress("DEPRECATION")
                        d.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        @Suppress("DEPRECATION")
                        gatt.writeDescriptor(d)
                    }
                }
            }
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            handlePacket(value)
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            characteristic.value?.let { handlePacket(it) }
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                _connectionState.value = ConnectionState.Connected(
                    peripheral?.name ?: "Device",
                    rssi
                )
            }
        }
    }

    private fun handlePacket(data: ByteArray) {
        val packet = SomnilPacket.fromByteArray(data) ?: return
        scope.launch {
            _lastPacket.emit(packet)
        }
    }

    fun startScanning() {
        if (!hasBluetoothPermission()) {
            _errorMessage.value = "Bluetooth permission denied"
            return
        }
        if (adapter?.isEnabled != true) {
            _errorMessage.value = "Bluetooth is turned off"
            return
        }

        scanner = adapter?.bluetoothLeScanner
        _isScanning.value = true
        _connectionState.value = ConnectionState.Scanning
        _discoveredDevices.value = emptyList()

        scanner?.startScan(
            listOf(ParcelUuid(SERVICE_UUID)),
            ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build(),
            scanCallback
        )

        scope.launch {
            delay(30_000)
            stopScanning()
        }
    }

    fun stopScanning() {
        scanner?.stopScan(scanCallback)
        _isScanning.value = false
        if (_connectionState.value is ConnectionState.Scanning) {
            _connectionState.value = ConnectionState.Disconnected
        }
    }

    fun connect(device: DiscoveredDevice) {
        stopScanning()
        lastPeripheralAddress = device.id

        peripheral = if (device.peripheral is BluetoothDevice) {
            device.peripheral as BluetoothDevice
        } else {
            adapter?.getRemoteDevice(device.id)
        }

        peripheral?.let { dev ->
            _connectionState.value = ConnectionState.Connecting(device.name)
            gatt = dev.connectGatt(context, false, gattCallback, BluetoothProfile.GATT)
        }
    }

    fun disconnect() {
        cancelAutoReconnect()
        stopRSSIUpdates()
        lastPeripheralAddress = null
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        peripheral = null
        _connectionState.value = ConnectionState.Disconnected
    }

    fun sendCommand(data: ByteArray) {
        val char = rxChar
        if (char == null) {
            _errorMessage.value = "Device not connected"
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt?.writeCharacteristic(char, data, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
        } else {
            @Suppress("DEPRECATION")
            char.value = data
            @Suppress("DEPRECATION")
            gatt?.writeCharacteristic(char)
        }
    }

    /**
     * Start auto-reconnect with exponential backoff.
     * Retries: 1s, 2s, 4s, 8s, 16s (max 5 attempts, up to 30s delay).
     * Mirrors iOS BLEManager reconnect logic.
     */
    private fun startAutoReconnect() {
        // Cancel any existing reconnect attempt
        cancelAutoReconnect()

        // Don't reconnect if already disconnected by user
        if (lastPeripheralAddress == null) {
            return
        }

        _connectionState.value = ConnectionState.Reconnecting(peripheral?.name ?: "Device")

        // Reset attempt counter for new disconnect
        reconnectAttempt = 0

        autoReconnectJob = scope.launch {
            attemptReconnect()
        }
    }

    /**
     * Attempt to reconnect with exponential backoff.
     */
    private suspend fun attemptReconnect() {
        while (reconnectAttempt < maxReconnectAttempts && isActive) {
            // Calculate delay with exponential backoff: 1s, 2s, 4s, 8s, 16s
            val delayMs = (baseReconnectDelayMs * (1 shl reconnectAttempt)).coerceAtMost(maxReconnectDelayMs)


            delay(delayMs)

            if (!isActive || lastPeripheralAddress == null) {
                return
            }

            reconnectAttempt++

            // Try to connect
            _connectionState.value = ConnectionState.Reconnecting(
                peripheral?.name ?: "Device"
            )

            // Attempt connection
            lastPeripheralAddress?.let { addr ->
                val device = adapter?.getRemoteDevice(addr)
                if (device != null) {
                    peripheral = device
                    try {
                        gatt = device.connectGatt(
                            context,
                            false,
                            gattCallback,
                            BluetoothProfile.GATT
                        )
                        // If we get here, connection was initiated
                        // State will be updated by onConnectionStateChange callback
                        return
                    } catch (e: Exception) {
                        // Connection failed, continue with backoff
                    }
                }
            }
        }

        // All retries exhausted or cancelled
        if (reconnectAttempt >= maxReconnectAttempts) {
            _errorMessage.value = "Connection failed after $maxReconnectAttempts attempts"
        }
        _connectionState.value = ConnectionState.Disconnected
        reconnectAttempt = 0
    }

    private fun cancelAutoReconnect() {
        autoReconnectJob?.cancel()
        autoReconnectJob = null
        reconnectAttempt = 0
    }

    private fun startRSSIUpdates() {
        rssiJob?.cancel()
        rssiJob = scope.launch {
            while (currentCoroutineContext().isActive) {
                delay(1000)
                if (_connectionState.value is ConnectionState.Connected) {
                    @Suppress("DEPRECATION")
                    gatt?.readRemoteRssi()
                }
            }
        }
    }

    private fun stopRSSIUpdates() {
        rssiJob?.cancel()
        rssiJob = null
    }

    private fun hasBluetoothPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    }
}
