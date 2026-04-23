package com.somnil.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.somnil.app.domain.model.*
import com.somnil.app.service.AudioPlayerManager
import com.somnil.app.service.DataProcessor
import com.somnil.app.service.MQTTManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AudioState(
    val isPlaying: Boolean = false,
    val mode: AudioPlayerManager.AudioMode = AudioPlayerManager.AudioMode.OFF,
    val volume: Float = 0.5f
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataProcessor: DataProcessor,
    private val mqttManager: MQTTManager,
    private val audioPlayer: AudioPlayerManager
) : ViewModel() {

    val settings: StateFlow<DetectionSettings> = dataProcessor.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DetectionSettings())

    val mqttState: StateFlow<Boolean> = mqttManager.isConnected
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _audioState = MutableStateFlow(AudioState())
    val audioState: StateFlow<AudioState> = _audioState.asStateFlow()

    init {
        // Observe audio player state
        viewModelScope.launch {
            combine(
                audioPlayer.isPlaying,
                audioPlayer.currentMode,
                audioPlayer.volume
            ) { isPlaying, mode, volume ->
                AudioState(isPlaying, mode, volume)
            }.collect { state ->
                _audioState.value = state
            }
        }
    }

    fun updateThreshold(threshold: Double) {
        dataProcessor.updateSettings(settings.value.copy(staltaThreshold = threshold))
    }

    fun updateSensitivity(sensitivity: SensitivityLevel) {
        dataProcessor.updateSettings(settings.value.copy(sensitivity = sensitivity))
    }

    fun toggleIntervention(type: InterventionType, enabled: Boolean) {
        val newMap = settings.value.interventionEnabled.toMutableMap()
        newMap[type] = enabled
        dataProcessor.updateSettings(settings.value.copy(interventionEnabled = newMap))
    }

    fun playAudio(mode: AudioPlayerManager.AudioMode) {
        audioPlayer.play(mode)
    }

    fun stopAudio() {
        audioPlayer.stop()
    }

    fun setVolume(volume: Float) {
        audioPlayer.setVolume(volume)
    }

    fun connectMQTT(host: String, port: Int) {
        mqttManager.updateConfig(host, port, "", "", "somnil/sleep_state")
        mqttManager.connect()
    }

    fun disconnectMQTT() {
        mqttManager.disconnect()
    }
}