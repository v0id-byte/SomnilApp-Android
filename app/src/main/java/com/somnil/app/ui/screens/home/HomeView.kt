package com.somnil.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.somnil.app.domain.model.ConnectionState
import com.somnil.app.domain.model.DetectionState
import com.somnil.app.ui.components.SomnilCard
import com.somnil.app.ui.components.SignalBarsView
import com.somnil.app.ui.theme.*

/**
 * HomeView - mirrors iOS HomeView.
 * Main screen with device connection and quick actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeView(
    onNavigateToLiveMonitor: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val currentState by viewModel.currentState.collectAsStateWithLifecycle()
    val currentSession by viewModel.currentSession.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val discoveredDevices by viewModel.discoveredDevices.collectAsStateWithLifecycle()

    var showDevicePicker by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = { Text("Somnil", style = MaterialTheme.typography.headlineLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundDark,
                    titleContentColor = TextPrimary
                ),
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "设置", tint = TextSecondary)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Logo and tagline
            LogoSection()

            // Connection card
            ConnectionCard(
                connectionState = connectionState,
                isScanning = isScanning,
                onScanClick = { viewModel.startScanning() },
                onDisconnectClick = { viewModel.disconnect() },
                onClick = { if (!connectionState.isConnected) showDevicePicker = true }
            )

            // Quick actions
            QuickActionsSection(
                isConnected = connectionState.isConnected,
                isMonitoring = currentState == DetectionState.MONITORING,
                onStartMonitoring = { viewModel.startMonitoring() },
                onStopMonitoring = { viewModel.stopMonitoring() },
                onNavigateToLiveMonitor = onNavigateToLiveMonitor
            )

            // Current session summary (if active)
            if (currentSession != null && currentSession!!.isActive) {
                SessionSummaryCard(
                    session = currentSession!!,
                    currentState = currentState
                )
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    // Device picker bottom sheet
    if (showDevicePicker) {
        DevicePickerSheet(
            devices = discoveredDevices,
            isScanning = isScanning,
            onDeviceClick = { device ->
                viewModel.connect(device)
                showDevicePicker = false
            },
            onRescanClick = { viewModel.startScanning() },
            onDismiss = { showDevicePicker = false }
        )
    }
}

@Composable
private fun LogoSection() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 16.dp)
    ) {
        // Gradient logo circle
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(LogoGradient)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.NightsStay,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "睡眠焦虑检测与干预",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}

@Composable
private fun ConnectionCard(
    connectionState: ConnectionState,
    isScanning: Boolean,
    onScanClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onClick: () -> Unit
) {
    SomnilCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Status indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(getStatusColor(connectionState))
                )

                Text(
                    text = connectionState.displayText,
                    style = MaterialTheme.typography.bodyLargeMono,
                    color = TextPrimary
                )

                Spacer()

                if (connectionState is ConnectionState.Connected) {
                    SignalBarsView(rssi = connectionState.rssi)
                }
            }

            // RSSI display
            if (connectionState is ConnectionState.Connected) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("信号强度", color = TextSecondary)
                    Text(
                        "${connectionState.rssi} dBm",
                        style = MaterialTheme.typography.bodySmallMono,
                        color = AccentBlue
                    )
                }
            }

            HorizontalDivider(color = CardBorder)

            // Connect/Disconnect button
            Button(
                onClick = {
                    if (connectionState.isConnected) onDisconnectClick()
                    else onScanClick()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (connectionState.isConnected) Warning else AccentPurple
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    if (connectionState.isConnected) Icons.Default.WifiOff else Icons.Default.Bluetooth,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (connectionState.isConnected) "断开连接" else "搜索设备",
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun QuickActionsSection(
    isConnected: Boolean,
    isMonitoring: Boolean,
    onStartMonitoring: () -> Unit,
    onStopMonitoring: () -> Unit,
    onNavigateToLiveMonitor: () -> Unit
) {
    Column {
        Text(
            text = "快速操作",
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Start Monitoring button
            QuickActionButton(
                icon = Icons.Default.PlayArrow,
                title = "开始监测",
                color = Success,
                isEnabled = isConnected && !isMonitoring,
                onClick = onStartMonitoring,
                modifier = Modifier.weight(1f)
            )

            // Stop Monitoring button
            QuickActionButton(
                icon = Icons.Default.Stop,
                title = "停止监测",
                color = Warning,
                isEnabled = isMonitoring,
                onClick = onStopMonitoring,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    color: Color,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = isEnabled,
        modifier = modifier.height(80.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isEnabled) color else InputBackground,
            contentColor = if (isEnabled) Color.White else TextSecondary,
            disabledContainerColor = InputBackground,
            disabledContentColor = TextSecondary
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null)
            Text(title, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun SessionSummaryCard(
    session: com.somnil.app.domain.model.SleepSession,
    currentState: DetectionState
) {
    val duration = System.currentTimeMillis() - session.startTime
    val hours = duration / 3600000
    val minutes = (duration % 3600000) / 60000

    SomnilCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "当前睡眠",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("已监测", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Text(
                        "${hours}h ${minutes}m",
                        style = MaterialTheme.typography.titleMedium,
                        color = AccentBlue
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("焦虑事件", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Text(
                        "${session.anxietyEventCount}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Warning
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    when (currentState) {
                        DetectionState.MONITORING -> Icons.Default.ShowChart
                        DetectionState.ANXIETY_DETECTED -> Icons.Default.Warning
                        DetectionState.INTERVENTION_ACTIVE -> Icons.Default.AutoAwesome
                        else -> Icons.Default.Bedtime
                    },
                    contentDescription = null,
                    tint = getStateColor(currentState),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    currentState.displayText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = getStateColor(currentState)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DevicePickerSheet(
    devices: List<com.somnil.app.domain.model.DiscoveredDevice>,
    isScanning: Boolean,
    onDeviceClick: (com.somnil.app.domain.model.DiscoveredDevice) -> Unit,
    onRescanClick: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = BackgroundMid
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "选择设备",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isScanning) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(color = AccentBlue, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("搜索设备中...", color = TextSecondary)
                }
            } else if (devices.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.BluetoothDisabled,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(50.dp)
                    )
                    Text("未发现 Somnil 设备", color = TextPrimary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = onRescanClick) {
                        Text("重新搜索")
                    }
                }
            } else {
                devices.forEach { device ->
                    Card(
                        onClick = { onDeviceClick(device) },
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(device.name, color = TextPrimary)
                                Text(
                                    "${device.rssi} dBm",
                                    style = MaterialTheme.typography.bodySmallMono,
                                    color = TextSecondary
                                )
                            }
                            SignalBarsView(rssi = device.rssi)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

private fun getStatusColor(state: ConnectionState): Color {
    return when (state) {
        is ConnectionState.Connected -> Success
        is ConnectionState.Scanning, is ConnectionState.Connecting, is ConnectionState.Reconnecting -> AccentBlue
        else -> TextSecondary
    }
}

private fun getStateColor(state: DetectionState): Color {
    return when (state) {
        DetectionState.MONITORING -> AccentPurple
        DetectionState.ANXIETY_DETECTED -> Warning
        DetectionState.INTERVENTION_ACTIVE -> Success
        else -> TextSecondary
    }
}