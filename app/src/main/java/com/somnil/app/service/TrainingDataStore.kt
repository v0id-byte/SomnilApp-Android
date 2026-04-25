package com.somnil.app.service

import android.content.Context
import android.content.SharedPreferences
import com.somnil.app.domain.model.NightTrainingData
import com.somnil.app.domain.model.TrainingPhase
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists TrainingPhase and NightTrainingData using SharedPreferences.
 * Mirrors iOS TrainingStorage / TrainingDataStore.
 */
@Singleton
class TrainingDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "somnil_training_prefs"
        private const val KEY_TRAINING_PHASE = "training_phase"
        private const val KEY_TRAINING_COMPLETED = "training_completed"
        private const val KEY_PERSONALIZED_THRESHOLD = "personalized_threshold"
        private const val KEY_TRAINING_NIGHTS = "training_nights"
        private const val KEY_TRAINING_START_DATE = "training_start_date"
        private const val KEY_LAST_SESSION_NIGHT = "last_session_night"
    }

    // ─── Training Phase ──────────────────────────────────────────────────────

    fun loadTrainingPhase(): TrainingPhase {
        val json = prefs.getString(KEY_TRAINING_PHASE, null) ?: return TrainingPhase()
        return try {
            val obj = JSONObject(json)
            TrainingPhase(
                id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                startDate = obj.optLong("startDate", System.currentTimeMillis()),
                completedDays = obj.optInt("completedDays", 0),
                totalSessions = obj.optInt("totalSessions", 0),
                successfulAdaptations = obj.optInt("successfulAdaptations", 0)
            )
        } catch (e: Exception) {
            TrainingPhase()
        }
    }

    fun saveTrainingPhase(phase: TrainingPhase) {
        val obj = JSONObject().apply {
            put("id", phase.id)
            put("startDate", phase.startDate)
            put("completedDays", phase.completedDays)
            put("totalSessions", phase.totalSessions)
            put("successfulAdaptations", phase.successfulAdaptations)
        }
        prefs.edit().putString(KEY_TRAINING_PHASE, obj.toString()).apply()
    }

    var isTrainingCompleted: Boolean
        get() = prefs.getBoolean(KEY_TRAINING_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_TRAINING_COMPLETED, value).apply()

    var personalizedThreshold: Double
        get() = prefs.getFloat(KEY_PERSONALIZED_THRESHOLD, 1.8f).toDouble()
        set(value) = prefs.edit().putFloat(KEY_PERSONALIZED_THRESHOLD, value.toFloat()).apply()

    // ─── Night Training Data ─────────────────────────────────────────────────

    fun getTrainingNights(): List<NightTrainingData> {
        val json = prefs.getString(KEY_TRAINING_NIGHTS, null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                NightTrainingData(
                    id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                    date = obj.optLong("date", 0),
                    avgSTALTA = obj.optDouble("avgSTALTA", 0.0),
                    stdSTALTA = obj.optDouble("stdSTALTA", 0.0),
                    anxietyCount = obj.optInt("anxietyCount", 0),
                    totalDurationSeconds = obj.optLong("totalDurationSeconds", 0)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveTrainingNights(nights: List<NightTrainingData>) {
        val array = JSONArray()
        nights.forEach { night ->
            val obj = JSONObject().apply {
                put("id", night.id)
                put("date", night.date)
                put("avgSTALTA", night.avgSTALTA)
                put("stdSTALTA", night.stdSTALTA)
                put("anxietyCount", night.anxietyCount)
                put("totalDurationSeconds", night.totalDurationSeconds)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_TRAINING_NIGHTS, array.toString()).apply()
    }

    /**
     * Record a night of training data.
     * If a record for today already exists, update it.
     */
    fun recordNightData(
        avgSTALTA: Double,
        stdSTALTA: Double,
        anxietyCount: Int,
        totalDurationSeconds: Long
    ) {
        val nights = getTrainingNights().toMutableList()
        val today = getStartOfDay(System.currentTimeMillis())

        val existingIndex = nights.indexOfFirst { getStartOfDay(it.date) == today }
        val newNight = NightTrainingData(
            date = today,
            avgSTALTA = avgSTALTA,
            stdSTALTA = stdSTALTA,
            anxietyCount = anxietyCount,
            totalDurationSeconds = totalDurationSeconds
        )

        if (existingIndex >= 0) {
            nights[existingIndex] = newNight
        } else {
            nights.add(newNight)
        }
        saveTrainingNights(nights)

        if (prefs.getLong(KEY_TRAINING_START_DATE, 0L) == 0L) {
            prefs.edit().putLong(KEY_TRAINING_START_DATE, today).apply()
        }
    }

    /**
     * Record a completed training session and advance training phase if criteria met.
     * Guards against: same-day double recording, recording after training completed,
     * and unbounded day advancement.
     */
    fun recordSession(success: Boolean, currentPhase: TrainingPhase): TrainingPhase {
        // Guard: prevent any recording after training is complete
        if (isTrainingCompleted) return currentPhase

        val today = getStartOfDay(System.currentTimeMillis())
        val lastSessionNight = prefs.getLong(KEY_LAST_SESSION_NIGHT, 0L)

        // Guard: prevent same-day double recording (page refresh bypass)
        if (lastSessionNight == today) return currentPhase

        var phase = currentPhase.copy(
            totalSessions = currentPhase.totalSessions + 1,
            successfulAdaptations = if (success) currentPhase.successfulAdaptations + 1 else currentPhase.successfulAdaptations
        )

        // Advance day only when: enough real days have passed AND enough successful sessions
        val daysPassed = getDaysPassed(phase.startDate)
        // Use min(completedDays, daysPassed) to prevent advancing beyond actual elapsed days
        val effectiveDays = minOf(phase.completedDays, daysPassed)
        if (effectiveDays < TrainingPhase.REQUIRED_TRAINING_DAYS &&
            phase.completedDays < TrainingPhase.REQUIRED_TRAINING_DAYS &&
            phase.successfulAdaptations >= effectiveDays + 1) {
            val newCompletedDays = minOf(TrainingPhase.REQUIRED_TRAINING_DAYS, effectiveDays + 1)
            phase = phase.copy(completedDays = newCompletedDays)
        }

        prefs.edit().putLong(KEY_LAST_SESSION_NIGHT, today).apply()
        saveTrainingPhase(phase)
        if (phase.isCompleted) {
            isTrainingCompleted = true
        }
        return phase
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

    private fun getDaysPassed(startDate: Long): Int {
        val today = getStartOfDay(System.currentTimeMillis())
        val start = getStartOfDay(startDate)
        val diff = today - start
        return (diff / (24 * 60 * 60 * 1000)).toInt()
    }
}
