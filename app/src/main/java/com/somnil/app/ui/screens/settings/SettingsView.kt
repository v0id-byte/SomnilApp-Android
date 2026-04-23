package com.somnil.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.somnil.app.domain.model.*
import com.somnil.app.ui.components.SomnilCard
import com.somnil.app.ui.theme.*

/**
 * SettingsView - mirrors iOS SettingsView.
 * Detection parameters, intervention types, Home Assistant MQTT, HA device.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsView(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val mqttState by viewModel.mqttState.collectAsStateWithLifecycle()
    val audioState by viewModel.audioState.collectAsStateWithLifecycle()

    val staltaThreshold = remember { mutableStateOf(settings.staltaThreshold) }
    val selectedSensitivity = remember { mutableStateOf(settings.sensitivity) }
    val haHost = remember { mutableStateOf("homeassistant.local") }
    val haPort = remember { mutableStateOf("1883") }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = { Text("设置", style = MaterialTheme.typography.headlineLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundDark,
                    titleContentColor = TextPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Detection Parameters
            DetectionParametersCard(
                threshold = staltaThreshold.value,
                onThresholdChange = {
                    staltaThreshold.value = it
                    viewModel.updateThreshold(it)
                }
            )

            // Sensitivity Selection
            SensitivityCard(
                selectedSensitivity = selectedSensitivity.value,
                onSelect = {
                    selectedSensitivity.value = it
                    viewModel.updateSensitivity(it)
                },
                currentThreshold = staltaThreshold.value
            )

            // Intervention Types
            InterventionCard(
                settings = settings,
                onToggle = { type, enabled ->
                    viewModel.toggleIntervention(type, enabled)
                }
            )

            // Audio Preview
            AudioPreviewCard(
                isPlaying = audioState.isPlaying,
                currentMode = audioState.mode,
                volume = audioState.volume,
                onPlayMode = { viewModel.playAudio(it) },
                onStop = { viewModel.stopAudio() },
                onVolumeChange = { viewModel.setVolume(it) }
            )

            // Home Assistant MQTT
            HomeAssistantCard(
                mqttState = mqttState,
                host = haHost.value,
                port = haPort.value,
                onConnect = {
                    viewModel.connectMQTT(haHost.value, haPort.value.toIntOrNull() ?: 1883)
                },
                onDisconnect = { viewModel.disconnectMQTT() }
            )

            // About
            AboutCard()

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun DetectionParametersCard(
    threshold: Double,
    onThresholdChange: (Double) -> Unit
) {
    SomnilCard {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = AccentPurple)
                Text(
                    text = "检测参数",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary
                )
            }

            // STA/LTA Threshold slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("STA/LTA 阈值", color = TextPrimary)
                    Text(
                        text = String.format("%.2f", threshold),
                        style = MaterialTheme.typography.bodyMediumMono,
                        color = AccentBlue
                    )
                }

                Slider(
                    value = threshold.toFloat(),
                    onValueChange = { onThresholdChange(it.toDouble()) },
                    valueRange = 1.2f..3.0f,
                    steps = 17,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentPurple,
                        activeTrackColor = AccentPurple
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("1.2", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text("3.0", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            }

            Text(
                text = "阈值越低，检测越敏感，焦虑事件记录越多",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun SensitivityCard(
    selectedSensitivity: SensitivityLevel,
    onSelect: (SensitivityLevel) -> Unit,
    currentThreshold: Double
) {
    SomnilCard {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Speed, contentDescription = null, tint = AccentPurple)
                Text(
                    text = "检测灵敏度",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SensitivityLevel.entries.forEach { level ->
                    SensitivityButton(
                        level = level,
                        isSelected = selectedSensitivity == level,
                        onClick = { onSelect(level) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("有效阈值", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Text(
                    text = String.format("%.2f", currentThreshold * selectedSensitivity.multiplier),
                    style = MaterialTheme.typography.bodySmallMono,
                    color = AccentBlue
                )
            }
        }
    }
}

@Composable
private fun SensitivityButton(
    level: SensitivityLevel,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) AccentPurple else InputBackground)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = level.displayName,
            style = MaterialTheme.typography.headlineSmall,
            color = if (isSelected) Color.White else TextSecondary
        )
        Text(
            text = level.description,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) Color.White.copy(alpha = 0.8f) else TextMuted
        )
    }
}

@Composable
private fun InterventionCard(
    settings: DetectionSettings,
    onToggle: (InterventionType, Boolean) -> Unit
) {
    SomnilCard {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AccentPurple)
                Text(
                    text = "干预方式",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary
                )
            }

            InterventionType.entries.forEach { type ->
                InterventionRow(
                    type = type,
                    isEnabled = settings.interventionEnabled[type] ?: false,
                    onToggle = { onToggle(type, it) }
                )
            }

            Text(
                text = "焦虑检测触发后，将自动启用已开启的干预方式",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun InterventionRow(
    type: InterventionType,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                when (type) {
                    InterventionType.SOUND -> Icons.Default.VolumeUp
                    InterventionType.TEMPERATURE -> Icons.Default.Thermostat
                    InterventionType.AROMATHERAPY -> Icons.Default.Spa
                },
                contentDescription = null,
                tint = if (isEnabled) AccentBlue else TextSecondary,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text("${type.displayName}干预", color = TextPrimary)
                Text(
                    text = type.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        Switch(
            checked = isEnabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AccentPurple
            )
        )
    }
}

@Composable
private fun AudioPreviewCard(
    isPlaying: Boolean,
    currentMode: com.somnil.app.service.AudioPlayerManager.AudioMode,
    volume: Float,
    onPlayMode: (com.somnil.app.service.AudioPlayerManager.AudioMode) -> Unit,
    onStop: () -> Unit,
    onVolumeChange: (Float) -> Unit
) {
    SomnilCard {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = AccentPurple)
                Text(
                    text = "音频预览",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary
                )
                Spacer()
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isPlaying) Success else TextSecondary)
                )
                Text(
                    text = if (isPlaying) currentMode.displayName else "已停止",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Text(
                text = "可在此预览粉红噪声和 Theta 波双耳节拍效果（通过蓝牙耳机）",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            // Audio mode buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                com.somnil.app.service.AudioPlayerManager.AudioMode.entries
                    .filter { it != com.somnil.app.service.AudioPlayerManager.AudioMode.OFF }
                    .forEach { mode ->
                        val isActive = isPlaying && currentMode == mode
                        Button(
                            onClick = {
                                if (isActive) onStop() else onPlayMode(mode)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isActive) AccentPurple else InputBackground,
                                contentColor = if (isActive) Color.White else TextSecondary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(mode.displayName, style = MaterialTheme.typography.labelMedium)
                        }
                    }
            }

            // Volume slider
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.VolumeDown, contentDescription = null, tint = TextSecondary)
                Slider(
                    value = volume,
                    onValueChange = onVolumeChange,
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentPurple,
                        activeTrackColor = AccentPurple
                    ),
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = AccentPurple)
            }

            Text(
                text = "音量：${(volume * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun HomeAssistantCard(
    mqttState: Boolean,
    host: String,
    port: String,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    SomnilCard {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Cloud, contentDescription = null, tint = AccentPurple)
                    Text(
                        text = "Home Assistant 集成",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (mqttState) Success else Warning)
                    )
                    Text(
                        text = if (mqttState) "已连接" else "未连接",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (mqttState) Success else Warning
                    )
                }
            }

            Button(
                onClick = if (mqttState) onDisconnect else onConnect,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (mqttState) Warning else AccentPurple
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (mqttState) "断开连接" else "连接")
            }
        }
    }
}

@Composable
private fun AboutCard() {
    SomnilCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = AccentPurple)
                Text(
                    text = "关于",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary
                )
            }

            AboutRow("版本", "1.0.0")
            AboutRow("构建", "2026.04.23")
        }
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
    }
}