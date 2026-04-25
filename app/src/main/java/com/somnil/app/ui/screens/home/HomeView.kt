package com.somnil.app.ui.screens.home

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.pm.PackageManager
import androidx.compose.ui.platform.LocalContext
import com.somnil.app.domain.model.ConnectionState
import com.somnil.app.domain.model.DetectionState
import com.somnil.app.ui.components.SomnilCard
import com.somnil.app.ui.components.SignalBarsView
import com.somnil.app.ui.theme.*

/**
 * HomeView - mirrors iOS HomeView.
 * Main screen with device connection and quick actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeView(
    onNavigateToLiveMonitor: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var hasPermissions by remember { mutableStateOf(false) }
    val permissions = remember {
        arrayOf(
            android.Manifest.permission.BLUETOOTH_SCAN,
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.POST_NOTIFICATIONS
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasPermissions = result.values.all { it }
    }

    LaunchedEffect(Unit) {
        hasPermissions = permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val currentState by viewModel.currentState.collectAsStateWithLifecycle()
    val currentSession by viewModel.currentSession.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val discoveredDevices by viewModel.discoveredDevices.collectAsStateWithLifecycle()
    val trainingPhase by viewModel.trainingPhase.collectAsStateWithLifecycle()
    val isTrainingCompleted by viewModel.isTrainingCompleted.collectAsStateWithLifecycle()

    var showDevicePicker by remember { mutableStateOf(false) }

    // Refresh training phase when composable enters composition
    LaunchedEffect(Unit) {
        viewModel.refreshTrainingPhase()
    }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = { Text("Somnil", style = MaterialTheme.typography.headlineLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundDark,
                    titleContentColor = TextPrimary
                ),
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "设置", tint = TextSecondary)
                    }
                }
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

            // Permission request card (Android 12+ BLE)
            if (!hasPermissions) {
                SomnilCard {
                    Column(modifier = Modifier.padding(4.dp)) {
                        Text(
                            "需要蓝牙权限才能连接 Somnil 设备",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "我们将请求以下权限：蓝牙扫描、蓝牙连接、位置（用于 BLE）、通知",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                permissionLauncher.launch(permissions)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("授予权限", color = Color.White)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "这些权限仅用于连接 Somnil 睡眠监测设备",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Logo and tagline
            LogoSection()

            // Connection card
            ConnectionCard(
                connectionState = connectionState,
                isScanning = isScanning,
                onScanClick = { viewModel.startScanning() },
                onDisconnectClick = { viewModel.disconnect() },
                onClick = { if (!connectionState.isConnected) showDevicePicker = true },
                onNavigateToSettings = onNavigateToSettings
            )

            // Issue 5: Training progress card (show when not completed)
            if (!isTrainingCompleted) {
                TrainingProgressCard(
                    trainingPhase = trainingPhase,
                    onStartTraining = onNavigateToSettings
                )
            }

            // Quick actions
            QuickActionsSection(
                isConnected = connectionState.isConnected,
                isMonitoring = currentState == DetectionState.MONITORING,
                onStartMonitoring = { viewModel.startMonitoring() },
                onStopMonitoring = { viewModel.stopMonitoring() },
                onNavigateToLiveMonitor = onNavigateToLiveMonitor
            )

            // Current session summary (if active)
            if (currentSession != null && currentSession!!.isActive) {
                SessionSummaryCard(
                    session = currentSession!!,
                    currentState = currentState
                )
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    // Device picker bottom sheet
    if (showDevicePicker) {
        DevicePickerSheet(
            devices = discoveredDevices,
            isScanning = isScanning,
            onDeviceClick = { device ->
                viewModel.connect(device)
                showDevicePicker = false
            },
            onRescanClick = { viewModel.startScanning() },
            onDismiss = { showDevicePicker = false }
        )
    }
}

@Composable
private fun LogoSection() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 16.dp)
    ) {
        // Gradient logo circle
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(LogoGradient)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.NightsStay,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "睡眠焦虑检测与干预",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}

@Composable
private fun ConnectionCard(
    connectionState: ConnectionState,
    isScanning: Boolean,
    onScanClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onClick: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    SomnilCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Status indicator with text label
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(getStatusColor(connectionState))
                )

                Text(
                    text = connectionState.displayText,
                    style = MaterialTheme.typography.bodyLargeMono,
                    color = TextPrimary
                )

                // Issue 15: connection state text label
                Text(
                    text = when (connectionState) {
                        is ConnectionState.Connected -> "已连接"
                        is ConnectionState.Connecting -> "连接中"
                        is ConnectionState.Scanning -> "扫描中"
                        is ConnectionState.Reconnecting -> "重连中"
                        is ConnectionState.Disconnected -> "未连接"
                        is ConnectionState.Error -> "错误"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    modifier = Modifier
                        .background(InputBackground, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )

                Spacer()

                if (connectionState is ConnectionState.Connected) {
                    SignalBarsView(rssi = connectionState.rssi)
                }
            }

            // RSSI display
            if (connectionState is ConnectionState.Connected) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("信号强度", color = TextSecondary)
                    Text(
                        "${connectionState.rssi} dBm",
                        style = MaterialTheme.typography.bodySmallMono,
                        color = AccentBlue
                    )
                }
            }

            HorizontalDivider(color = CardBorder)

            // Connect/Disconnect button
            Button(
                onClick = {
                    if (connectionState.isConnected) onDisconnectClick()
                    else onScanClick()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (connectionState.isConnected) Warning else AccentPurple
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    if (connectionState.isConnected) Icons.Default.WifiOff else Icons.Default.Bluetooth,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (connectionState.isConnected) "断开连接" else "搜索设备",
                    color = Color.White
                )
            }

            // Issue 1: Guide text below connect button
            if (!connectionState.isConnected) {
                Text(
                    text = "首次使用？请先打开 Somnil 硬件电源，等待指示灯闪烁后再点击",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(InputBackground, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                )
            }

            // Issue 6: BLE connection failure guidance
            if (connectionState is ConnectionState.Error) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Warning.copy(alpha = 0.1f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "❌ 连接失败：${(connectionState as ConnectionState.Error).reason}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Warning
                    )
                    Text("请按以下步骤：", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text("1. 打开手机「设置」→「蓝牙」", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text("2. 找到「Somnil」，确保开关是打开", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text("3. 确认手机蓝牙已开启", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text("4. 回到 App 重新点击连接", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = onScanClick,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("重新连接", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionsSection(
    isConnected: Boolean,
    isMonitoring: Boolean,
    onStartMonitoring: () -> Unit,
    onStopMonitoring: () -> Unit,
    onNavigateToLiveMonitor: () -> Unit
) {
    Column {
        Text(
            text = "快速操作",
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Start Monitoring button
            QuickActionButton(
                icon = Icons.Default.PlayArrow,
                title = "开始监测",
                color = Success,
                isEnabled = isConnected && !isMonitoring,
                onClick = onStartMonitoring,
                modifier = Modifier.weight(1f)
            )

            // Stop Monitoring button
            QuickActionButton(
                icon = Icons.Default.Stop,
                title = "停止监测",
                color = Warning,
                isEnabled = isMonitoring,
                onClick = onStopMonitoring,
                modifier = Modifier.weight(1f)
            )
        }

        // Issue 4: Show disabled reason when not connected
        if (!isConnected) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Warning.copy(alpha = 0.1f))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⚠️ 请先连接 Somnil 设备", color = Warning)
                }
                TextButton(onClick = onNavigateToLiveMonitor) {
                    Text("点击这里连接设备", color = AccentBlue)
                }
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    color: Color,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = isEnabled,
        modifier = modifier.height(80.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isEnabled) color else InputBackground,
            contentColor = if (isEnabled) Color.White else TextSecondary,
            disabledContainerColor = InputBackground,
            disabledContentColor = TextSecondary
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null)
            Text(title, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun SessionSummaryCard(
    session: com.somnil.app.domain.model.SleepSession,
    currentState: DetectionState
) {
    val duration = System.currentTimeMillis() - session.startTime
    val hours = duration / 3600000
    val minutes = (duration % 3600000) / 60000

    SomnilCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "当前睡眠",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("已监测", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Text(
                        "${hours}h ${minutes}m",
                        style = MaterialTheme.typography.titleMedium,
                        color = AccentBlue
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("焦虑事件", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Text(
                        "${session.anxietyEventCount}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Warning
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    when (currentState) {
                        DetectionState.MONITORING -> Icons.Default.ShowChart
                        DetectionState.ANXIETY_DETECTED -> Icons.Default.Warning
                        DetectionState.INTERVENTION_ACTIVE -> Icons.Default.AutoAwesome
                        else -> Icons.Default.Bedtime
                    },
                    contentDescription = null,
                    tint = getStateColor(currentState),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    currentState.displayText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = getStateColor(currentState)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DevicePickerSheet(
    devices: List<com.somnil.app.domain.model.DiscoveredDevice>,
    isScanning: Boolean,
    onDeviceClick: (com.somnil.app.domain.model.DiscoveredDevice) -> Unit,
    onRescanClick: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = BackgroundMid
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "选择设备",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isScanning) {
                // Issue 2: Scanning empty state with guidance
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardBackground)
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(color = AccentBlue, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("📡 正在搜索设备...", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "确保以下几点：",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "✅ 硬件已开机，指示灯正在闪烁",
                            "✅ 设备在手机附近（1米内）",
                            "✅ 等待 5-10 秒让设备被发现"
                        ).forEach { text ->
                            Text(text, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onRescanClick,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("重新搜索")
                        }
                        OutlinedButton(
                            onClick = { /* TODO: open help */ },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("仍搜不到？查看帮助", color = TextSecondary)
                        }
                    }
                }
            } else if (devices.isEmpty()) {
                // Issue 2: No devices found with guidance
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardBackground)
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.BluetoothDisabled,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(50.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("未发现 Somnil 设备", color = TextPrimary, style = MaterialTheme.typography.titleMedium)

                    Spacer(modifier = Modifier.height(20.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "✅ 硬件已开机，指示灯正在闪烁",
                            "✅ 设备在手机附近（1米内）",
                            "✅ 等待 5-10 秒让设备被发现"
                        ).forEach { text ->
                            Text(text, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onRescanClick,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("重新搜索")
                        }
                        OutlinedButton(
                            onClick = { /* TODO: open help */ },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("仍搜不到？查看帮助", color = TextSecondary)
                        }
                    }
                }
            } else {
                // Issue 3: Device list with signal strength labels
                Text(
                    text = "信号格数越多越好，推荐选择信号最强的设备",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                devices.forEach { device ->
                    Card(
                        onClick = { onDeviceClick(device) },
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(device.name, color = TextPrimary)
                                Text(
                                    "${device.rssi} dBm",
                                    style = MaterialTheme.typography.bodySmallMono,
                                    color = TextSecondary
                                )
                                // Issue 16: Signal bars description
                                Text(
                                    text = "${device.signalBars}格${if (device.signalBars >= 4) "（最佳）" else if (device.signalBars >= 2) "（可用）" else "（较弱）"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = when {
                                        device.signalBars >= 4 -> Success
                                        device.signalBars >= 2 -> AccentBlue
                                        else -> Warning
                                    }
                                )
                            }
                            SignalBarsView(rssi = device.rssi)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

private fun getStatusColor(state: ConnectionState): Color {
    return when (state) {
        is ConnectionState.Connected -> Success
        is ConnectionState.Scanning, is ConnectionState.Connecting, is ConnectionState.Reconnecting -> AccentBlue
        is ConnectionState.Error -> Warning
        else -> TextSecondary
    }
}

private fun getStateColor(state: DetectionState): Color {
    return when (state) {
        DetectionState.MONITORING -> AccentPurple
        DetectionState.ANXIETY_DETECTED -> Warning
        DetectionState.INTERVENTION_ACTIVE -> Success
        else -> TextSecondary
    }
}

// Issue 5: Training progress entry card
@Composable
private fun TrainingProgressCard(
    trainingPhase: com.somnil.app.domain.model.TrainingPhase,
    onStartTraining: () -> Unit
) {
    SomnilCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🎯",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "首次使用需要完成 3 天训练",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "当前进度：Day ${trainingPhase.completedDays.coerceAtLeast(1)}/3",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                // Progress indicator
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (i in 1..3) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    if (i <= trainingPhase.completedDays) Success
                                    else CardBorder
                                )
                        )
                    }
                }
            }

            Button(
                onClick = onStartTraining,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentPurple
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("立即开始训练 →", color = Color.White)
            }
        }
    }
}