package com.somnil.app.ui.screens.training

import androidx.lifecycle.ViewModel
import com.somnil.app.domain.model.NightTrainingData
import com.somnil.app.domain.model.TrainingPhase
import com.somnil.app.service.TrainingDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Training phase state exposed to TrainingView.
 * Mirrors iOS TrainingView @State + TrainingStorage / TrainingDataStore.
 */
data class TrainingUiState(
    val trainingPhase: TrainingPhase = TrainingPhase(),
    val trainingNights: List<NightTrainingData> = emptyList(),
    val isTrainingCompleted: Boolean = false,
    val personalizedThreshold: Double = 1.8
)

@HiltViewModel
class TrainingViewModel @Inject constructor(
    private val trainingDataStore: TrainingDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrainingUiState())
    val uiState: StateFlow<TrainingUiState> = _uiState.asStateFlow()

    init {
        loadTrainingData()
    }

    private fun loadTrainingData() {
        val phase = trainingDataStore.loadTrainingPhase()
        val nights = trainingDataStore.getTrainingNights()
        val isCompleted = trainingDataStore.isTrainingCompleted
        val threshold = trainingDataStore.personalizedThreshold

        _uiState.value = TrainingUiState(
            trainingPhase = phase,
            trainingNights = nights,
            isTrainingCompleted = isCompleted,
            personalizedThreshold = threshold
        )
    }

    /**
     * Record a completed training session with the given metrics.
     * Advances the training phase if criteria are met.
     */
    fun recordSession(
        avgSTALTA: Double,
        stdSTALTA: Double,
        anxietyCount: Int,
        totalDurationSeconds: Long,
        success: Boolean
    ) {
        // Record night data
        trainingDataStore.recordNightData(avgSTALTA, stdSTALTA, anxietyCount, totalDurationSeconds)

        // Record session and potentially advance phase
        val updatedPhase = trainingDataStore.recordSession(success, _uiState.value.trainingPhase)

        _uiState.value = _uiState.value.copy(
            trainingPhase = updatedPhase,
            trainingNights = trainingDataStore.getTrainingNights(),
            isTrainingCompleted = trainingDataStore.isTrainingCompleted
        )
    }

    /**
     * Reset training phase to day 1 (for re-training scenarios).
     */
    fun resetTraining() {
        val freshPhase = TrainingPhase()
        trainingDataStore.saveTrainingPhase(freshPhase)
        trainingDataStore.isTrainingCompleted = false
        trainingDataStore.saveTrainingNights(emptyList())
        _uiState.value = TrainingUiState()
    }

    /**
     * Manually advance training day (for testing/admin only).
     * Guarded: cannot advance beyond REQUIRED_TRAINING_DAYS or after completion.
     */
    fun advanceDay() {
        val current = _uiState.value.trainingPhase
        val isCompleted = _uiState.value.isTrainingCompleted
        if (!current.isCompleted && !isCompleted && current.completedDays < TrainingPhase.REQUIRED_TRAINING_DAYS) {
            val updated = current.copy(completedDays = minOf(TrainingPhase.REQUIRED_TRAINING_DAYS, current.completedDays + 1))
            trainingDataStore.saveTrainingPhase(updated)
            _uiState.value = _uiState.value.copy(trainingPhase = updated)
        }
    }

    fun refresh() {
        loadTrainingData()
    }
}
