package com.somnil.app.data.model

/**
 * Represents a single step in the smart home setup guide.
 */
data class GuideStep(
    val stepNumber: Int,
    val totalSteps: Int,
    val title: String,
    val instruction: String,
    val screenshotHint: String? = null,
    /** For MQTT/manual step: whether to show a copy-to-clipboard button with this text */
    val copyableText: String? = null
)

/**
 * Represents a smart home ecosystem with its guide steps.
 */
data class EcosystemGuide(
    val id: String,
    val name: String,
    val description: String,
    val iconEmoji: String,
    val priority: EcosystemPriority,
    val steps: List<GuideStep>
)

enum class EcosystemPriority {
    HIGH,
    MEDIUM
}
