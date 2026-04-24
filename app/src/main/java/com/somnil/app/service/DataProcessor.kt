package com.somnil.app.service

import com.somnil.app.domain.model.*
import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.math.PI
import kotlin.math.sin

/**
 * DataProcessor - mirrors iOS DataProcessor service.
 * Handles real-time EEG packet processing, STA/LTA calculation, and anxiety detection.
 * Uses real FFT for EEG band power calculation (mirrors iOS vDSP FFT).
 */
@Singleton
class DataProcessor @Inject constructor(
    private val bleManager: BLEManager,
    private val pressureHistoryManager: PressureHistoryManager
) {
    companion object {
        // Detection constants (mirrors iOS DetectionConstants)
        const val DEFAULT_STALTA_THRESHOLD = 1.8
        const val MIN_STALTA_THRESHOLD = 1.2
        const val MAX_STALTA_THRESHOLD = 3.0
        const val STA_WINDOW_SECONDS = 0.5
        const val LTA_WINDOW_SECONDS = 30.0
        const val SAMPLE_RATE_HZ = 250.0

        // FFT constants
        const val FFT_SIZE = 128  // 128-point FFT at 250Hz = 1.95Hz resolution
        const val FFT_WINDOW_SECONDS = FFT_SIZE / SAMPLE_RATE_HZ  // 0.512s

        // Frequency band bins for 250Hz / 128 = 1.953125 Hz per bin
        // Beta: 13-30Hz → bins 7-15 (13/1.95 ≈ 7, 30/1.95 ≈ 15)
        // Alpha: 8-13Hz → bins 4-7
        // Theta: 4-8Hz → bins 2-4
        // Delta: 0.5-4Hz → bins 0-2
        // VLF: 0.003-0.03Hz → bins 0-1
        // EMG: 30-100Hz (we use 30-50Hz for practical purposes)
        const val BETA_BIN_START = 7   // 13Hz
        const val BETA_BIN_END = 16   // 30Hz (exclusive, so 16 to include bin 15)
        const val ALPHA_BIN_START = 4  // 8Hz
        const val ALPHA_BIN_END = 7     // 13Hz
        const val THETA_BIN_START = 2    // 4Hz
        const val THETA_BIN_END = 4    // 8Hz
        const val DELTA_BIN_START = 0    // 0Hz
        const val DELTA_BIN_END = 2     // 4Hz
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // FFT instance (thread-safe for single-threaded use)
    private val fft = DoubleFFT_1D(FFT_SIZE.toLong())

    // FFT buffer for real FFT (complex: real + imaginary interleaved)
    private val fftBuffer = DoubleArray(FFT_SIZE * 2)

    // Sample accumulation buffers (one per channel, 8 channels)
    private val sampleBuffers = Array(8) { ArrayDeque<Double>(FFT_SIZE) }
    private val lock = Any()

    // Current detection state
    private val _currentState = MutableStateFlow(DetectionState.IDLE)
    val currentState: StateFlow<DetectionState> = _currentState.asStateFlow()

    // Current session
    private val _currentSession = MutableStateFlow<SleepSession?>(null)
    val currentSession: StateFlow<SleepSession?> = _currentSession.asStateFlow()

    // STA/LTA values
    private val _currentSTALTA = MutableStateFlow(0.0)
    val currentSTALTA: StateFlow<Double> = _currentSTALTA.asStateFlow()

    // Current packet
    private val _currentPacket = MutableStateFlow<SomnilPacket?>(null)
    val currentPacket: StateFlow<SomnilPacket?> = _currentPacket.asStateFlow()

    // Current sleep stage
    private val _currentSleepStage = MutableStateFlow(SleepStage.UNKNOWN)
    val currentSleepStage: StateFlow<SleepStage> = _currentSleepStage.asStateFlow()

    // Settings
    private val _settings = MutableStateFlow(DetectionSettings())
    val settings: StateFlow<DetectionSettings> = _settings.asStateFlow()

    // Band power history
    private val _betaHistory = MutableStateFlow<List<Double>>(emptyList())
    val betaHistory: StateFlow<List<Double>> = _betaHistory.asStateFlow()
    private val _alphaHistory = MutableStateFlow<List<Double>>(emptyList())
    val alphaHistory: StateFlow<List<Double>> = _alphaHistory.asStateFlow()
    private val _thetaHistory = MutableStateFlow<List<Double>>(emptyList())
    val thetaHistory: StateFlow<List<Double>> = _thetaHistory.asStateFlow()
    private val _deltaHistory = MutableStateFlow<List<Double>>(emptyList())
    val deltaHistory: StateFlow<List<Double>> = _deltaHistory.asStateFlow()
    private val _emgHistory = MutableStateFlow<List<Double>>(emptyList())
    val emgHistory: StateFlow<List<Double>> = _emgHistory.asStateFlow()

    // Session beta power collection (for nightly summary)
    private val sessionBetaPowers = mutableListOf<Double>()

    // Internal buffers
    private val staBuffer = ArrayDeque<Double>(100)
    private val ltaBuffer = ArrayDeque<Double>(6000)
    private var baselineEnergy = 0.0

    private var collectionJob: Job? = null

    init {
        // Subscribe to BLE packets
        scope.launch {
            bleManager.lastPacket.collect { packet ->
                processPacket(packet)
            }
        }
    }

    fun startMonitoring() {
        if (_currentState.value.isConnected) return

        _currentState.value = DetectionState.CALIBRATING

        // Start new session
        _currentSession.value = SleepSession(
            startTime = System.currentTimeMillis()
        )

        // Reset buffers
        staBuffer.clear()
        ltaBuffer.clear()
        _betaHistory.value = emptyList()
        _alphaHistory.value = emptyList()
        _thetaHistory.value = emptyList()
        _deltaHistory.value = emptyList()
        _emgHistory.value = emptyList()

        // Clear FFT sample buffers
        synchronized(lock) {
            sampleBuffers.forEach { it.clear() }
        }

        // Clear session beta power collection
        sessionBetaPowers.clear()

        // Start calibration (30 seconds like iOS)
        scope.launch {
            delay(30_000)
            _currentState.value = DetectionState.MONITORING
            baselineEnergy = calculateMeanEnergy()
        }
    }

    fun stopMonitoring() {
        collectionJob?.cancel()
        collectionJob = null
        _currentState.value = DetectionState.IDLE

        // Finalize session
        _currentSession.value?.let { session ->
            val finalizedSession = session.copy(
                endTime = System.currentTimeMillis(),
                sleepQualityScore = calculateSleepQuality()
            )
            _currentSession.value = finalizedSession

            // Record NightlySummary to PressureHistoryManager (only on complete session end)
            scope.launch {
                recordNightlySummary(finalizedSession)
            }
        }
    }

    /**
     * Record a NightlySummary when monitoring stops (end of complete sleep session).
     * Only records on stopMonitoring(), not on mid-night arousals.
     * Mirrors iOS stopMonitoring() recording logic.
     */
    private suspend fun recordNightlySummary(session: SleepSession) {
        // Compute median beta power from collected session values
        val medianBeta = if (sessionBetaPowers.isNotEmpty()) {
            sessionBetaPowers.sorted().let { sorted ->
                val mid = sorted.size / 2
                if (sorted.size % 2 == 0) {
                    (sorted[mid - 1] + sorted[mid]) / 2.0
                } else {
                    sorted[mid]
                }
            }
        } else {
            0.0
        }

        // Compute sleep efficiency from session duration and stage durations
        val totalDurationMs = (session.endTime ?: System.currentTimeMillis()) - session.startTime
        val totalDurationSec = totalDurationMs / 1000.0
        val sleepTimeSec = session.stageDurations
            .filterKeys { it != SleepStage.AWAKE }
            .values
            .sum()
        val efficiency = if (totalDurationSec > 0) sleepTimeSec / totalDurationSec else 0.0

        val summary = NightlySummary(
            date = getStartOfDay(session.startTime),
            medianBetaPower = medianBeta,
            anxietyArousalCount = session.anxietyEventCount,
            sleepEfficiency = efficiency.coerceIn(0.0, 1.0)
        )

        pressureHistoryManager.recordNightlySummary(summary)
    }

    private fun getStartOfDay(timestamp: Long): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun updateSettings(newSettings: DetectionSettings) {
        _settings.value = newSettings
    }

    private fun processPacket(packet: SomnilPacket) {
        if (!packet.isValid) return

        _currentPacket.value = packet

        // Add samples to FFT buffers
        synchronized(lock) {
            for (channel in 0 until minOf(packet.channelData.size, 8)) {
                sampleBuffers[channel].addLast(packet.channelData[channel].toDouble())
                // Keep only latest FFT_SIZE samples
                while (sampleBuffers[channel].size > FFT_SIZE) {
                    sampleBuffers[channel].removeFirst()
                }
            }
        }

        // Calculate energy for STA
        val energy = calculatePacketEnergy(packet)
        staBuffer.addLast(energy)
        ltaBuffer.addLast(energy)

        // Maintain buffer sizes
        while (staBuffer.size > (STA_WINDOW_SECONDS * SAMPLE_RATE_HZ).toInt()) {
            staBuffer.removeFirst()
        }
        while (ltaBuffer.size > (LTA_WINDOW_SECONDS * SAMPLE_RATE_HZ).toInt()) {
            ltaBuffer.removeFirst()
        }

        // Calculate STA/LTA ratio
        val sta = if (staBuffer.isNotEmpty()) staBuffer.average() else 0.0
        val lta = if (ltaBuffer.isNotEmpty()) ltaBuffer.average() else 0.0

        _currentSTALTA.value = if (lta > 0) sta / lta else 0.0

        // Calculate band powers using real FFT
        val beta = calculateBetaPower(packet)
        val alpha = calculateAlphaPower(packet)
        val theta = calculateThetaPower(packet)
        val delta = calculateDeltaPower(packet)
        val emg = calculateEMGPower(packet)

        _betaHistory.value = (_betaHistory.value + beta).takeLast(100)
        _alphaHistory.value = (_alphaHistory.value + alpha).takeLast(100)
        _thetaHistory.value = (_thetaHistory.value + theta).takeLast(100)
        _deltaHistory.value = (_deltaHistory.value + delta).takeLast(100)
        _emgHistory.value = (_emgHistory.value + emg).takeLast(100)

        // Collect session beta powers for nightly summary
        if (beta > 0) {
            sessionBetaPowers.add(beta)
        }

        // Check anxiety detection
        checkAnxietyThreshold()

        // Update sleep stage based on EEG
        updateSleepStage(beta, alpha, theta, delta, emg)
    }

    private fun calculatePacketEnergy(packet: SomnilPacket): Double {
        return packet.channelData.map { (it * it).toDouble() }.sum()
    }

    private fun calculateMeanEnergy(): Double {
        return if (ltaBuffer.isNotEmpty()) ltaBuffer.average() else 0.0
    }

    /**
     * Apply Hann window to samples.
     * Hann window: w(n) = 0.5 * (1 - cos(2*PI*n / (N-1)))
     */
    private fun applyHannWindow(samples: DoubleArray): DoubleArray {
        val n = samples.size
        return DoubleArray(n) { i ->
            val multiplier = 0.5 * (1 - cos(2 * PI * i / (n - 1)))
            samples[i] * multiplier
        }
    }

    /**
     * Calculate band power using FFT.
     * Mirrors iOS vDSP FFT implementation.
     */
    private fun calculateBandPower(channelIndex: Int, binStart: Int, binEnd: Int): Double {
        synchronized(lock) {
            val buffer = sampleBuffers[channelIndex]
            if (buffer.size < FFT_SIZE) {
                return 0.0
            }

            // Get latest FFT_SIZE samples
            val samples = buffer.toList().takeLast(FFT_SIZE).toDoubleArray()

            // Apply Hann window
            val windowedSamples = applyHannWindow(samples)

            // Copy to FFT buffer (interleaved real/imaginary)
            for (i in 0 until FFT_SIZE) {
                fftBuffer[i * 2] = windowedSamples[i]      // Real part
                fftBuffer[i * 2 + 1] = 0.0              // Imaginary part
            }

            // Perform forward FFT
            fft.realForward(fftBuffer)

            // Calculate power spectrum: |X(k)|^2 = real^2 + imag^2
            var totalPower = 0.0
            for (bin in binStart until minOf(binEnd, FFT_SIZE / 2)) {
                val real = fftBuffer[bin * 2]
                val imag = fftBuffer[bin * 2 + 1]
                totalPower += real * real + imag * imag
            }

            // Normalize by FFT size and window energy
            val windowEnergy = FFT_SIZE * FFT_SIZE / 4  // Sum of Hann window squared
            return totalPower / windowEnergy
        }
    }

    /**
     * Calculate Beta band power (13-30Hz) using FFT.
     * Mirrors iOS calculateBetaPower using vDSP FFT.
     */
    private fun calculateBetaPower(packet: SomnilPacket): Double {
        // Use first 4 channels (matching iOS EEG channels)
        var totalPower = 0.0
        for (channel in 0 until minOf(4, packet.channelData.size)) {
            totalPower += calculateBandPower(channel, BETA_BIN_START, BETA_BIN_END)
        }
        return totalPower / 4.0
    }

    /**
     * Calculate Alpha band power (8-13Hz) using FFT.
     */
    private fun calculateAlphaPower(packet: SomnilPacket): Double {
        var totalPower = 0.0
        for (channel in 0 until minOf(4, packet.channelData.size)) {
            totalPower += calculateBandPower(channel, ALPHA_BIN_START, ALPHA_BIN_END)
        }
        return totalPower / 4.0
    }

    /**
     * Calculate Theta band power (4-8Hz) using FFT.
     */
    private fun calculateThetaPower(packet: SomnilPacket): Double {
        var totalPower = 0.0
        for (channel in 0 until minOf(4, packet.channelData.size)) {
            totalPower += calculateBandPower(channel, THETA_BIN_START, THETA_BIN_END)
        }
        return totalPower / 4.0
    }

    /**
     * Calculate Delta band power (0.5-4Hz) using FFT.
     */
    private fun calculateDeltaPower(packet: SomnilPacket): Double {
        var totalPower = 0.0
        for (channel in 0 until minOf(4, packet.channelData.size)) {
            totalPower += calculateBandPower(channel, DELTA_BIN_START, DELTA_BIN_END)
        }
        return totalPower / 4.0
    }

    /**
     * Calculate EMG (muscle artifact) power using high-frequency content.
     * Uses simple variance for high-frequency power (30-50Hz equivalent).
     */
    private fun calculateEMGPower(packet: SomnilPacket): Double {
        // Use last 2 channels for EMG detection (matching iOS)
        // Calculate as variance of recent samples (proxy for high-frequency power)
        synchronized(lock) {
            var totalEmg = 0.0
            for (channel in 6 until minOf(8, packet.channelData.size)) {
                val buffer = sampleBuffers[channel]
                if (buffer.size >= 8) {
                    val recentSamples = buffer.toList().takeLast(8)
                    val mean = recentSamples.average()
                    val variance = recentSamples.map { (it - mean) * (it - mean) }.average()
                    totalEmg += variance
                }
            }
            return totalEmg / maxOf(1, minOf(2, packet.channelData.size) - 6)
        }
    }

    private fun checkAnxietyThreshold() {
        val threshold = _settings.value.effectiveThreshold

        when {
            _currentSTALTA.value > threshold -> {
                if (_currentState.value != DetectionState.ANXIETY_DETECTED) {
                    _currentState.value = DetectionState.ANXIETY_DETECTED
                    // Increment anxiety count
                    _currentSession.value = _currentSession.value?.copy(
                        anxietyEventCount = (_currentSession.value?.anxietyEventCount ?: 0) + 1
                    )
                    // Trigger intervention
                    triggerIntervention()
                }
            }
            _currentState.value == DetectionState.ANXIETY_DETECTED -> {
                _currentState.value = DetectionState.MONITORING
            }
        }
    }

    private fun triggerIntervention() {
        val enabled = _settings.value.interventionEnabled
        scope.launch {
            _currentState.value = DetectionState.INTERVENTION_ACTIVE

            // Trigger enabled interventions
            if (enabled[InterventionType.SOUND] == true) {
                // Audio intervention handled by AudioManager
            }
            if (enabled[InterventionType.TEMPERATURE] == true) {
                // Temperature intervention via HA device
            }
            if (enabled[InterventionType.AROMATHERAPY] == true) {
                // Aromatherapy intervention via HA device
            }

            // Duration: 5 seconds like iOS
            delay(5_000)

            if (_currentState.value == DetectionState.INTERVENTION_ACTIVE) {
                _currentState.value = DetectionState.MONITORING
            }
        }
    }

    private fun updateSleepStage(beta: Double, alpha: Double, theta: Double, delta: Double, emg: Double) {
        // Simple heuristic - real implementation would be ML-based
        // Using band powers for sleep stage classification
        _currentSleepStage.value = when {
            emg > maxOf(beta, theta) * 2 -> SleepStage.AWAKE
            beta > maxOf(alpha, theta, delta) * 1.5 -> SleepStage.REM
            delta > maxOf(beta, alpha, theta) * 3 -> SleepStage.N3
            theta > maxOf(beta, alpha, delta) * 1.5 -> SleepStage.N1
            else -> SleepStage.N2
        }
    }

    private fun calculateSleepQuality(): Int {
        val session = _currentSession.value ?: return 0
        // Simple quality calculation
        val baseScore = 70
        val anxietyPenalty = session.anxietyEventCount * 5
        return (baseScore - anxietyPenalty).coerceIn(0, 100)
    }
}

private val DetectionState.isConnected: Boolean
    get() = this == DetectionState.MONITORING || this == DetectionState.CALIBRATING