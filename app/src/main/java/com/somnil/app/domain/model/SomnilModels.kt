package com.somnil.app.domain.model

import java.util.UUID

/**
 * BLE packet model - mirrors iOS SomnilPacket.
 * 20-byte protocol: AA [8ch x 2bytes] [status] [checksum] [footer]
 */
data class SomnilPacket(
    val channelData: List<Short>, // 8 channels, 2 bytes each = 16 bytes
    val statusByte: UByte,
    val checksum: UByte,
    val isValid: Boolean,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        const val PACKET_LENGTH = 20
        const val HEADER_BYTE: UByte = 0xAAU
        const val FOOTER_BYTE: UByte = 0x55U

        fun fromByteArray(data: ByteArray): SomnilPacket? {
            if (data.size != PACKET_LENGTH) return null
            if (data[0].toUByte() != HEADER_BYTE) return null

            val channelData = mutableListOf<Short>()
            for (i in 0 until 8) {
                val low = data[i * 2].toUShort()
                val high = data[i * 2 + 1].toUShort()
                channelData.add(((high shl 8) or low).toShort())
            }

            val statusByte = data[17].toUByte()
            val checksum = data[18].toUByte()
            val footer = data[19].toUByte()

            // Simple checksum: sum of first 18 bytes mod 256
            val calculatedChecksum = data.take(18).sumOf { it.toUByte().toInt() }.toUByte()
            val isValid = footer == FOOTER_BYTE && checksum == calculatedChecksum

            return SomnilPacket(channelData, statusByte, checksum, isValid)
        }
    }

    /** Bit 0 of statusByte indicates anxiety detected */
    val isAnxietyDetected: Boolean
        get() = isValid && (statusByte.toInt() and 0x01) != 0
}

/**
 * Sleep session model - mirrors iOS SleepSession.
 */
data class SleepSession(
    val id: String = UUID.randomUUID().toString(),
    val startTime: Long,
    val endTime: Long? = null,
    val stageDurations: Map<SleepStage, Double> = emptyMap(),
    val anxietyEventCount: Int = 0,
    val sleepQualityScore: Int = 0, // 0-100
    val avgSTALTA: Double = 0.0,
    val notes: String = ""
) {
    val isActive: Boolean
        get() = endTime == null

    val totalDuration: Double
        get() = stageDurations.values.sum()

    val dominantStage: SleepStage
        get() = stageDurations.maxByOrNull { it.value }?.key ?: SleepStage.UNKNOWN
}

/**
 * Discovered BLE device model - mirrors iOS DiscoveredDevice.
 */
data class DiscoveredDevice(
    val id: String,
    val name: String,
    val rssi: Int,
    val peripheral: Any? = null // Android BluetoothDevice
) {
    val signalBars: Int
        get() = when {
            rssi > -50 -> 4
            rssi > -60 -> 3
            rssi > -70 -> 2
            rssi > -80 -> 1
            else -> 0
        }
}

/**
 * Detection settings model - mirrors iOS DetectionSettings.
 */
data class DetectionSettings(
    val staltaThreshold: Double = 1.8,
    val sensitivity: SensitivityLevel = SensitivityLevel.MEDIUM,
    val interventionEnabled: Map<InterventionType, Boolean> = mapOf(
        InterventionType.SOUND to true,
        InterventionType.TEMPERATURE to false,
        InterventionType.AROMATHERAPY to false
    )
) {
    val effectiveThreshold: Double
        get() = staltaThreshold * sensitivity.multiplier
}

/**
 * MQTT config for Home Assistant integration.
 */
data class MQTTConfig(
    val brokerHost: String = "homeassistant.local",
    val brokerPort: Int = 1883,
    val username: String = "",
    val password: String = "",
    val sleepStateTopic: String = "somnil/sleep_state"
)
