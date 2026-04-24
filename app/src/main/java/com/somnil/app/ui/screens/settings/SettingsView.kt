package com.somnil.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement.spacedBy
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.somnil.app.domain.model.*
import com.somnil.app.ui.components.SomnilCard
import com.somnil.app.ui.theme.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

/**
 * SettingsView - mirrors iOS SettingsView.
 * Detection parameters, intervention types, Home Assistant MQTT, HA device.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsView(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateToSmartHome: () -> Unit = {}
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val mqttState by viewModel.mqttState.collectAsStateWithLifecycle()
    val audioState by viewModel.audioState.collectAsStateWithLifecycle()
    val latencyResult by viewModel.latencyResult.collectAsStateWithLifecycle()

    val staltaThreshold = remember { mutableStateOf(settings.staltaThreshold) }
    val selectedSensitivity = remember { mutableStateOf(settings.sensitivity) }
    val haHost = remember { mutableStateOf("homeassistant.local") }
    val haPort = remember { mutableStateOf("1883") }
    val haUsername = remember { mutableStateOf("") }
    val haPassword = remember { mutableStateOf("") }

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
                username = haUsername.value,
                password = haPassword.value,
                onHostChange = { haHost.value = it },
                onPortChange = { haPort.value = it },
                onUsernameChange = { haUsername.value = it },
                onPasswordChange = { haPassword.value = it },
                onConnect = {
                    viewModel.connectMQTT(haHost.value, haPort.value.toIntOrNull() ?: 1883)
                },
                onDisconnect = { viewModel.disconnectMQTT() }
            )

            // Smart Home Setup Guide
            SmartHomeGuideCard(
                onClick = onNavigateToSmartHome
            )

            // Debug: Latency Measurement Panel
            DebugLatencyCard(
                latencyResult = latencyResult,
                onTestLatency = { viewModel.testLatency() },
                onClearResult = { viewModel.clearLatencyResult() }
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

            // Issue 12: Threshold slider explanation
            Text(
                text = "📌 阈值说明",
                style = MaterialTheme.typography.labelMedium,
                color = AccentBlue
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(InputBackground)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("• 值越低 = 越敏感 = 更多干预（可能误报）", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text("• 值越高 = 越宽松 = 更少干预（可能漏报）", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text("建议保持默认 1.5，无需频繁调整", style = MaterialTheme.typography.bodySmall, color = AccentBlue)
            }
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

            // Issue 13: Intervention descriptions
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(InputBackground)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("🔊 声音干预：检测到焦虑时自动播放引导音频", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text("🌡️ 温度干预（需设备支持）：温控设备自动调节", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text("🌸 香薰干预（需设备支持）：香薰机自动释放", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
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

            // Issue 14: Audio mode descriptions
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(InputBackground)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val pinkDesc = "🎵 粉红噪声：柔和白噪声，掩盖环境噪音，适合助眠"
                val thetaDesc = "🌊 Theta 波：4-8Hz 低频音，诱导深度放松"

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = pinkDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { onPlayMode(com.somnil.app.service.AudioPlayerManager.AudioMode.PINK_NOISE) }) {
                        Text("▶ 试听", color = AccentBlue)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = thetaDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { onPlayMode(com.somnil.app.service.AudioPlayerManager.AudioMode.THETA_WAVE) }) {
                        Text("▶ 试听", color = AccentBlue)
                    }
                }
            }

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
    onDisconnect: () -> Unit,
    onHostChange: (String) -> Unit = {},
    onPortChange: (String) -> Unit = {},
    onUsernameChange: (String) -> Unit = {},
    onPasswordChange: (String) -> Unit = {},
    username: String = "",
    password: String = ""
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

            // Issue 7: HA address guidance
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(InputBackground)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("📍 Home Assistant 地址", style = MaterialTheme.typography.labelMedium, color = AccentBlue)
                Text("在浏览器打开 Home Assistant，复制地址栏的 URL 填入", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text("示例：http://192.168.1.100:8123", style = MaterialTheme.typography.bodySmallMono, color = TextSecondary)
            }

            // Broker address field
            OutlinedTextField(
                value = host,
                onValueChange = onHostChange,
                label = { Text("Broker 地址 *") },
                placeholder = { Text("homeassistant.local 或 IP 地址") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentPurple,
                    unfocusedBorderColor = CardBorder,
                    focusedLabelColor = AccentPurple,
                    unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = AccentPurple
                ),
                shape = RoundedCornerShape(10.dp)
            )

            // Port field
            OutlinedTextField(
                value = port,
                onValueChange = onPortChange,
                label = { Text("端口 *") },
                placeholder = { Text("1883") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentPurple,
                    unfocusedBorderColor = CardBorder,
                    focusedLabelColor = AccentPurple,
                    unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = AccentPurple
                ),
                shape = RoundedCornerShape(10.dp)
            )

            // Username field
            OutlinedTextField(
                value = username,
                onValueChange = onUsernameChange,
                label = { Text("用户名") },
                placeholder = { Text("如未设置则留空") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentPurple,
                    unfocusedBorderColor = CardBorder,
                    focusedLabelColor = AccentPurple,
                    unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = AccentPurple
                ),
                shape = RoundedCornerShape(10.dp)
            )

            // Password field
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text("密码") },
                placeholder = { Text("如未设置则留空") },
                singleLine = true,
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentPurple,
                    unfocusedBorderColor = CardBorder,
                    focusedLabelColor = AccentPurple,
                    unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = AccentPurple
                ),
                shape = RoundedCornerShape(10.dp)
            )

            // Issue 8: Required field note
            Text(
                text = "* 为必填项。用户名密码如未在 HA 设置则留空",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            // MQTT connection failure guidance (Issue 18)
            if (!mqttState) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Warning.copy(alpha = 0.1f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("❌ 连接失败？", style = MaterialTheme.typography.labelMedium, color = Warning)
                    Text("请按以下步骤排查：", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text("1. 确认 Home Assistant 已启动且 MQTT 插件已安装", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text("2. 确认 Broker 地址和端口正确", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text("3. 确认手机和 HA 在同一局域网内", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text("4. 如使用了认证，确认用户名密码正确", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
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

/**
 * Card with a button that opens the smart home ecosystem guide.
 */
@Composable
private fun SmartHomeGuideCard(
    onClick: () -> Unit
) {
    SomnilCard {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Devices, contentDescription = null, tint = AccentPurple)
                Text(
                    text = "智能家居配置",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary
                )
            }

            Text(
                text = "通过 Home Assistant、Apple Home、小米米家等平台连接 Somnil，一步一步引导配置。",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentPurple
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Devices, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("开始配置")
            }
        }
    }
}

/**
 * Debug latency measurement card.
 * Mirrors iOS debug panel test latency button.
 * Issue 19: Added developer feature label.
 */
@Composable
private fun DebugLatencyCard(
    latencyResult: com.somnil.app.ui.screens.settings.DebugLatencyResult?,
    onTestLatency: () -> Unit,
    onClearResult: () -> Unit
) {
    SomnilCard {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Issue 19: Developer feature label
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Warning.copy(alpha = 0.1f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🔧", style = MaterialTheme.typography.labelMedium)
                Text(
                    text = "开发者功能（普通用户无需操作）",
                    style = MaterialTheme.typography.labelMedium,
                    color = Warning
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Speed, contentDescription = null, tint = AccentPurple)
                Text(
                    text = "调试：延迟测量",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary
                )
            }

            Text(
                text = "记录 EEG 样本时间戳 → 执行分类 → 测量端到端延迟",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Button(
                onClick = onTestLatency,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentBlue
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("测试延迟")
            }

            if (latencyResult != null && latencyResult.isComplete) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(InputBackground)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "测量结果",
                        style = MaterialTheme.typography.labelMedium,
                        color = AccentBlue
                    )
                    LatencyRow("样本时间戳", latencyResult.sampleTimestamp.toString())
                    LatencyRow("分类时间戳", latencyResult.classificationTimestamp.toString())
                    LatencyRow("总延迟", "${latencyResult.totalLatencyMs} ms")

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = onClearResult,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = InputBackground,
                            contentColor = TextSecondary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("清除结果", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun LatencyRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Text(
            value,
            style = MaterialTheme.typography.bodySmallMono,
            color = TextPrimary
        )
    }
}