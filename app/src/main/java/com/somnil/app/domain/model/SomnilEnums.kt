package com.somnil.app.domain.model

/**
 * Sleep stage enumeration - mirrors iOS SleepStage enum.
 * Colors match iOS hex values converted to Android color resources.
 */
enum class SleepStage(
    val displayName: String,
    val colorHex: String
) {
    AWAKE("清醒", "FF4757"),
    N1("N1", "FF6B81"),
    N2("N2", "7B68EE"),
    N3("N3", "1E3A8A"),
    REM("REM", "00D4FF"),
    UNKNOWN("未知", "4A4A4A");

    companion object {
        fun fromString(value: String): SleepStage {
            return entries.find { it.displayName == value } ?: UNKNOWN
        }
    }
}

/**
 * Detection state - mirrors iOS DetectionState enum.
 */
enum class DetectionState(
    val displayText: String,
    val icon: String,
    val colorHex: String
) {
    IDLE("待机", "moon.zzz", "C0C0D0"),
    CALIBRATING("校准中", "circle.dotted", "00D4FF"),
    MONITORING("监测中", "waveform", "7B68EE"),
    ANXIETY_DETECTED("焦虑!", "exclamationmark.triangle", "FF4757"),
    INTERVENTION_ACTIVE("干预中", "sparkles", "00FF88"),
    ERROR("错误", "xmark.circle", "FF4757")
}

/**
 * Intervention type - mirrors iOS InterventionType enum.
 */
enum class InterventionType(
    val displayName: String,
    val description: String,
    val icon: String
) {
    SOUND("声", "白噪音/自然音", "speaker.wave.3"),
    TEMPERATURE("温", "温热敷", "thermometer"),
    AROMATHERAPY("香", "芳香疗法", "leaf")
}

/**
 * Sensitivity level - mirrors iOS SensitivityLevel enum.
 */
enum class SensitivityLevel(
    val displayName: String,
    val description: String,
    val multiplier: Double
) {
    LOW("低", "减少误报", 0.7),
    MEDIUM("中", "平衡", 1.0),
    HIGH("高", "更敏感", 1.3)
}

/**
 * BLE connection state - mirrors iOS ConnectionState enum.
 */
sealed class ConnectionState(
    val displayText: String,
    val isConnected: Boolean = false
) {
    data object Disconnected : ConnectionState("未连接", false)
    data object Scanning : ConnectionState("搜索中...", false)
    data class Connecting(val deviceName: String) : ConnectionState("连接 $deviceName...", false)
    data class Reconnecting(val deviceName: String) : ConnectionState("重新连接 $deviceName...", false)
    data class Connected(val deviceName: String, val rssi: Int) : ConnectionState(deviceName, true)
}

/**
 * HA Device BLE command - mirrors iOS HADEVICEBLEConstants.Command enum.
 */
enum class HADEVICCommand(val displayName: String, val colorHex: String, val byteValue: UByte) {
    WHITE("白光", "FFFFFF", 0x01U),
    PINK("粉光", "FF69B4", 0x02U),
    BROWN("棕光", "CD853F", 0x03U),
    STOP("停止", "808080", 0x10U)
}
