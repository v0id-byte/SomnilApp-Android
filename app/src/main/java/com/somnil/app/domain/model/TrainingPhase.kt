package com.somnil.app.domain.model

import java.util.UUID

/**
 * Training phase data for Somnil 3-day adaptation.
 * Mirrors iOS TrainingPhase model.
 */
data class TrainingPhase(
    val id: String = UUID.randomUUID().toString(),
    val startDate: Long = System.currentTimeMillis(),
    val completedDays: Int = 0,
    val totalSessions: Int = 0,
    val successfulAdaptations: Int = 0
) {
    companion object {
        const val REQUIRED_TRAINING_DAYS = 3
    }

    /** Progress through 3-day training (0.0 to 1.0) */
    val progress: Double
        get() = completedDays.toDouble() / REQUIRED_TRAINING_DAYS

    val isCompleted: Boolean
        get() = completedDays >= REQUIRED_TRAINING_DAYS

    val daysRemaining: Int
        get() = maxOf(0, REQUIRED_TRAINING_DAYS - completedDays)

    val statusText: String
        get() = when {
            isCompleted -> "训练完成 ✓"
            completedDays == 0 -> "第 1 天（尚未开始）"
            completedDays == 1 -> "第 2 天"
            else -> "第 3 天"
        }

    val recommendationText: String
        get() = if (isCompleted) {
            "已完成个性化适配，可关闭训练模式"
        } else {
            "建议每晚睡前完成一个睡眠周期（约 7-8 小时）"
        }

    /** Description for each training day */
    fun dayDescription(day: Int): String = when (day) {
        1 -> "首次适配，记录基础偏好"
        2 -> "强化训练，巩固干预效果"
        3 -> "完成适配，确定最优方案"
        else -> ""
    }
}

/**
 * Night training data record - mirrors iOS NightTrainingData.
 */
data class NightTrainingData(
    val id: String = UUID.randomUUID().toString(),
    val date: Long = System.currentTimeMillis(),
    val avgSTALTA: Double = 0.0,
    val stdSTALTA: Double = 0.0,
    val anxietyCount: Int = 0,
    val totalDurationSeconds: Long = 0
) {
    val formattedDate: String
        get() {
            val formatter = java.text.SimpleDateFormat("MM月dd日", java.util.Locale.getDefault())
            return formatter.format(java.util.Date(date))
        }
}
