package com.somnil.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.somnil.app.domain.model.ConnectionState
import com.somnil.app.domain.model.DetectionState
import com.somnil.app.domain.model.DiscoveredDevice
import com.somnil.app.domain.model.SleepSession
import com.somnil.app.service.BLEManager
import com.somnil.app.service.DataProcessor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val bleManager: BLEManager,
    private val dataProcessor: DataProcessor
) : ViewModel() {

    val connectionState: StateFlow<ConnectionState> = bleManager.connectionState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ConnectionState.Disconnected)

    val isScanning: StateFlow<Boolean> = bleManager.isScanning
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = bleManager.discoveredDevices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentState: StateFlow<DetectionState> = dataProcessor.currentState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DetectionState.IDLE)

    val currentSession: StateFlow<SleepSession?> = dataProcessor.currentSession
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun startScanning() {
        bleManager.startScanning()
    }

    fun stopScanning() {
        bleManager.stopScanning()
    }

    fun connect(device: DiscoveredDevice) {
        bleManager.connect(device)
    }

    fun disconnect() {
        bleManager.disconnect()
    }

    fun startMonitoring() {
        dataProcessor.startMonitoring()
    }

    fun stopMonitoring() {
        dataProcessor.stopMonitoring()
    }
}