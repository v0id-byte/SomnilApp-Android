package com.somnil.app.domain.model

/**
 * NightlySummary - mirrors iOS NightlySummary.
 * Records a single night's sleep analysis for pressure tracking.
 *
 * @param date epoch millis of the night (start of night, 00:00 local)
 * @param medianBetaPower Beta 13-30Hz power median for the night (from FFT)
 * @param anxietyArousalCount Number of anxiety micro-arousals detected during the night
 * @param sleepEfficiency Ratio of actual sleep time to time in bed (0.0 - 1.0)
 */
data class NightlySummary(
    val date: Long,              // epoch millis (start of day)
    val medianBetaPower: Double,  // Beta 13-30Hz band power median
    val anxietyArousalCount: Int, // Anxiety micro-arousal count for the night
    val sleepEfficiency: Double   // Sleep efficiency (0.0 - 1.0)
) {
    companion object {
        fun empty(date: Long) = NightlySummary(
            date = date,
            medianBetaPower = 0.0,
            anxietyArousalCount = 0,
            sleepEfficiency = 0.0
        )
    }
}