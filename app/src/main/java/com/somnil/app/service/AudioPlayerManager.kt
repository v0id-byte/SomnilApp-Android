package com.somnil.app.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sin
import kotlin.random.Random

/**
 * AudioPlayerManager - mirrors iOS AudioPlayerManager.
 * Generates pink noise, theta wave binaural beats for intervention.
 */
@Singleton
class AudioPlayerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    enum class AudioMode(val displayName: String) {
        OFF("关闭"),
        PINK_NOISE("粉红噪音"),
        THETA_WAVE("Theta波"),
        WHITE_NOISE("白噪音")
    }

    private val _currentMode = MutableStateFlow(AudioMode.OFF)
    val currentMode: StateFlow<AudioMode> = _currentMode.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _volume = MutableStateFlow(0.5f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val sampleRate = 44100
    private val bufferSize = 4096

    fun play(mode: AudioMode) {
        stop()
        _currentMode.value = mode
        
        if (mode == AudioMode.OFF) return

        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        val format = AudioFormat.Builder()
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .build()

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(attributes)
            .setAudioFormat(format)
            .setBufferSizeInBytes(bufferSize * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.play()
        _isPlaying.value = true

        playbackJob = scope.launch {
            when (mode) {
                AudioMode.PINK_NOISE -> playPinkNoise()
                AudioMode.THETA_WAVE -> playThetaWave()
                AudioMode.WHITE_NOISE -> playWhiteNoise()
                else -> {}
            }
        }
    }

    fun stop() {
        playbackJob?.cancel()
        playbackJob = null
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
        _isPlaying.value = false
    }

    fun setVolume(vol: Float) {
        _volume.value = vol.coerceIn(0f, 1f)
        audioTrack?.setVolume(_volume.value)
    }

    private suspend fun playPinkNoise() {
        val buffer = ShortArray(bufferSize)
        var b0 = 0.0; var b1 = 0.0; var b2 = 0.0
        var b3 = 0.0; var b4 = 0.0; var b5 = 0.0; var b6 = 0.0

        while (isActive) {
            for (i in buffer.indices) {
                val white = Random.nextDouble(-1.0, 1.0)
                b0 = 0.99886 * b0 + white * 0.0555179
                b1 = 0.99332 * b1 + white * 0.0750759
                b2 = 0.96900 * b2 + white * 0.1538520
                b3 = 0.86650 * b3 + white * 0.3104856
                b4 = 0.55000 * b4 + white * 0.5329522
                b5 = -0.7616 * b5 - white * 0.0168980
                val pink = (b0 + b1 + b2 + b3 + b4 + b5 + b6 + white * 0.5362) * 0.11
                b6 = white * 0.115926
                buffer[i] = (pink * Short.MAX_VALUE * _volume.value).toInt().toShort()
            }
            audioTrack?.write(buffer, 0, buffer.size)
        }
    }

    private suspend fun playThetaWave() {
        val buffer = ShortArray(bufferSize)
        val thetaFreq = 6.0 // 6Hz theta
        val carrierFreq = 200.0 // 200Hz carrier
        var phase = 0.0
        val phaseIncrement = 2 * Math.PI * thetaFreq / sampleRate

        while (isActive) {
            for (i in buffer.indices) {
                // Binaural beat: left ear carrier, right ear carrier + theta
                val left = sin(2 * Math.PI * carrierFreq * i / sampleRate)
                val right = sin(2 * Math.PI * (carrierFreq + thetaFreq) * i / sampleRate + phase)
                val sample = ((left + right) / 2 * 0.5 * _volume.value).toFloat()
                buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
                phase += phaseIncrement
            }
            audioTrack?.write(buffer, 0, buffer.size)
        }
    }

    private suspend fun playWhiteNoise() {
        val buffer = ShortArray(bufferSize)
        while (isActive) {
            for (i in buffer.indices) {
                buffer[i] = (Random.nextDouble(-1.0, 1.0) * Short.MAX_VALUE * _volume.value).toInt().toShort()
            }
            audioTrack?.write(buffer, 0, buffer.size)
        }
    }
}