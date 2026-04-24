package com.somnil.app.service

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.somnil.app.domain.model.NightlySummary
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

private val Context.nightlyDataStore: DataStore<Preferences> by preferencesDataStore(name = "somnil_nightly_history")

/**
 * PressureHistoryManager - mirrors iOS PressureHistoryManager.
 * Manages the NightlySummary history and computes pressure baseline.
 *
 * Storage: DataStore ("somnil_nightly_history")
 * Baseline: Median of last 14 days medianBetaPower values
 * Alert: isPressureBuilding = 3 consecutive days > baseline × 1.3
 */
@Singleton
class PressureHistoryManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_HISTORY = stringPreferencesKey("nightly_history")
        private val KEY_LAST_BASELINE = stringPreferencesKey("last_baseline")
        private const val MAX_HISTORY_DAYS = 30  // Keep up to 30 days
        private const val BASELINE_WINDOW_DAYS = 14
        private const val PRESSURE_THRESHOLD_MULTIPLIER = 1.3
        private const val CONSECUTIVE_DAYS_REQUIRED = 3
    }

    /**
     * Record a nightly summary for the given night.
     * If a record for tonight already exists, update it.
     * Only called at the end of a complete sleep session (stopMonitoring), not on mid-night arousals.
     */
    suspend fun recordNightlySummary(summary: NightlySummary) {
        val history = loadHistory().toMutableList()

        // Find existing entry for the same date (midnight-normalized)
        val existingIndex = history.indexOfFirst { normalizeToMidnight(it.date) == normalizeToMidnight(summary.date) }

        if (existingIndex >= 0) {
            history[existingIndex] = summary
        } else {
            history.add(summary)
        }

        // Sort by date ascending and trim to MAX_HISTORY_DAYS
        history.sortBy { it.date }
        val trimmed = history.takeLast(MAX_HISTORY_DAYS)

        saveHistory(trimmed)
    }

    /**
     * Compute baseline: median of last 14 days medianBetaPower.
     * Returns null if fewer than 2 data points available.
     */
    suspend fun computeBaseline(): Double? {
        val history = loadHistory()
        val recentPowerValues = history
            .sortedBy { it.date }
            .takeLast(BASELINE_WINDOW_DAYS)
            .map { it.medianBetaPower }

        if (recentPowerValues.size < 2) return null

        return median(recentPowerValues)
    }

    /**
     * Determine if pressure is building.
     * Returns true when the last 3 nights ALL exceed baseline × 1.3.
     */
    suspend fun isPressureBuilding(): Boolean {
        val baseline = computeBaseline() ?: return false
        val threshold = baseline * PRESSURE_THRESHOLD_MULTIPLIER

        val history = loadHistory()
            .sortedBy { it.date }
            .takeLast(CONSECUTIVE_DAYS_REQUIRED)

        if (history.size < CONSECUTIVE_DAYS_REQUIRED) return false

        return history.all { it.medianBetaPower > threshold }
    }

    /**
     * Get the full history for UI display.
     */
    suspend fun getHistory(): List<NightlySummary> {
        return loadHistory().sortedBy { it.date }
    }

    /**
     * Get the computed threshold value (baseline × 1.3) for UI display.
     */
    suspend fun getPressureThreshold(): Double? {
        val baseline = computeBaseline() ?: return null
        return baseline * PRESSURE_THRESHOLD_MULTIPLIER
    }

    // ─── Private helpers ─────────────────────────────────────────────────────────

    private suspend fun loadHistory(): List<NightlySummary> {
        return context.nightlyDataStore.data
            .map { preferences ->
                val json = preferences[KEY_HISTORY] ?: "[]"
                parseHistory(json)
            }
            .first()
    }

    private suspend fun saveHistory(history: List<NightlySummary>) {
        context.nightlyDataStore.edit { preferences ->
            preferences[KEY_HISTORY] = serializeHistory(history)
        }
    }

    private fun serializeHistory(history: List<NightlySummary>): String {
        val array = JSONArray()
        history.forEach { summary ->
            val obj = JSONObject().apply {
                put("date", summary.date)
                put("medianBetaPower", summary.medianBetaPower)
                put("anxietyArousalCount", summary.anxietyArousalCount)
                put("sleepEfficiency", summary.sleepEfficiency)
            }
            array.put(obj)
        }
        return array.toString()
    }

    private fun parseHistory(json: String): List<NightlySummary> {
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                NightlySummary(
                    date = obj.getLong("date"),
                    medianBetaPower = obj.getDouble("medianBetaPower"),
                    anxietyArousalCount = obj.getInt("anxietyArousalCount"),
                    sleepEfficiency = obj.getDouble("sleepEfficiency")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun normalizeToMidnight(timestamp: Long): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    /**
     * Compute median of a list of doubles.
     * If even count, average the two middle values.
     */
    private fun median(values: List<Double>): Double {
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 0) {
            (sorted[mid - 1] + sorted[mid]) / 2.0
        } else {
            sorted[mid]
        }
    }
}