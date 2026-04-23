package com.somnil.app.ui.screens.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.somnil.app.domain.model.*
import com.somnil.app.service.BLEManager
import com.somnil.app.service.DataProcessor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class LiveMonitorViewModel @Inject constructor(
    private val bleManager: BLEManager,
    private val dataProcessor: DataProcessor
) : ViewModel() {

    val connectionState: StateFlow<ConnectionState> = bleManager.connectionState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ConnectionState.Disconnected)

    val currentState: StateFlow<DetectionState> = dataProcessor.currentState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DetectionState.IDLE)

    val currentSTALTA: StateFlow<Double> = dataProcessor.currentSTALTA
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val currentPacket: StateFlow<SomnilPacket?> = dataProcessor.currentPacket
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentSleepStage: StateFlow<SleepStage> = dataProcessor.currentSleepStage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SleepStage.UNKNOWN)

    val settings: StateFlow<DetectionSettings> = dataProcessor.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DetectionSettings())

    val betaHistory: StateFlow<List<Double>> = dataProcessor.betaHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val vlfHistory: StateFlow<List<Double>> = dataProcessor.vlfHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val emgHistory: StateFlow<List<Double>> = dataProcessor.emgHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}