package com.somnil.app.ui.screens.live

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.somnil.app.domain.model.ConnectionState
import com.somnil.app.domain.model.DetectionState
import com.somnil.app.domain.model.SleepStage
import com.somnil.app.ui.components.LiveBadge
import com.somnil.app.ui.components.SomnilCard
import com.somnil.app.ui.theme.*

/**
 * LiveMonitorView - mirrors iOS LiveMonitorView.
 * Real-time EEG monitoring with STA/LTA gauge, waveform, and band power.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveMonitorView(
    viewModel: LiveMonitorViewModel = hiltViewModel()
) {
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val currentState by viewModel.currentState.collectAsStateWithLifecycle()
    val currentSTALTA by viewModel.currentSTALTA.collectAsStateWithLifecycle()
    val currentPacket by viewModel.currentPacket.collectAsStateWithLifecycle()
    val currentSleepStage by viewModel.currentSleepStage.collectAsStateWithLifecycle()
    val betaHistory by viewModel.betaHistory.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = { Text("实时监测", style = MaterialTheme.typography.headlineMedium) },
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
            // Status Header
            StatusHeader(
                currentState = currentState,
                connectionState = connectionState,
                timestamp = currentPacket?.timestamp
            )

            // BLE Connection Banner
            BLEConnectionBanner(connectionState = connectionState)

            // STA/LTA Gauge Card
            STALTAGaugeCard(
                currentSTALTA = currentSTALTA,
                threshold = settings.effectiveThreshold
            )

            // Real-time Waveform Card
            WaveformCard(
                channelData = currentPacket?.channelData ?: emptyList()
            )

            // Band Power Card
            BandPowerCard(
                betaHistory = betaHistory,
                vlfHistory = viewModel.vlfHistory.collectAsStateWithLifecycle().value,
                emgHistory = viewModel.emgHistory.collectAsStateWithLifecycle().value
            )

            // Current State Card
            StateCard(
                currentState = currentState,
                currentSleepStage = currentSleepStage
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun StatusHeader(
    currentState: DetectionState,
    connectionState: ConnectionState,
    timestamp: Long?
) {
    SomnilCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(getStateColor(currentState))
                )
                Text(
                    text = currentState.displayText,
                    style = MaterialTheme.typography.headlineSmall,
                    color = getStateColor(currentState)
                )
            }

            timestamp?.let {
                Text(
                    text = formatTimestamp(it),
                    style = MaterialTheme.typography.bodySmallMono,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun BLEConnectionBanner(connectionState: ConnectionState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(BackgroundMid)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (connectionState.isConnected) Icons.Default.Bluetooth else Icons.Default.BluetoothDisabled,
                contentDescription = null,
                tint = if (connectionState.isConnected) AccentBlue else TextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = connectionState.displayText,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        if (connectionState.isConnected) {
            LiveBadge()
        }
    }
}

@Composable
private fun STALTAGaugeCard(
    currentSTALTA: Double,
    threshold: Double
) {
    SomnilCard {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "STA/LTA 比值",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Gauge
            Box(
                modifier = Modifier.size(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 20.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2
                    val startAngle = 135f
                    val sweepAngle = 270f

                    // Background arc
                    drawArc(
                        color = InputBackground,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Value arc
                    val valueRatio = (currentSTALTA / 3.0).coerceIn(0.0, 1.0)
                    val valueSweep = (valueRatio * sweepAngle).toFloat()
                    
                    drawArc(
                        brush = Brush.horizontalGradient(
                            if (currentSTALTA > threshold) listOf(Warning, WarningLight)
                            else listOf(AccentPurple, AccentBlue)
                        ),
                        startAngle = startAngle,
                        sweepAngle = valueSweep,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                // Value text
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = String.format("%.2f", currentSTALTA),
                        style = MaterialTheme.typography.displayMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "阈值: ${String.format("%.2f", threshold)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun WaveformCard(channelData: List<Short>) {
    SomnilCard {
        Column {
            Text(
                text = "脑电波形 (CH1)",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(InputBackground),
                contentAlignment = Alignment.Center
            ) {
                if (channelData.isNotEmpty()) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val path = Path()
                        val dataPoints = channelData.take(100)
                        val stepX = size.width / dataPoints.size

                        dataPoints.forEachIndexed { index, value ->
                            val x = index * stepX
                            val y = size.height / 2 + (value / 32768.0 * size.height / 2).toFloat()
                            
                            if (index == 0) path.moveTo(x, y)
                            else path.lineTo(x, y)
                        }

                        drawPath(
                            path = path,
                            brush = Brush.verticalGradient(
                                colors = listOf(AccentBlue, AccentPurple)
                            ),
                            style = Stroke(width = 2f, cap = StrokeCap.Round)
                        )
                    }
                } else {
                    Text("等待数据...", color = TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun BandPowerCard(
    betaHistory: List<Double>,
    vlfHistory: List<Double>,
    emgHistory: List<Double>
) {
    SomnilCard {
        Column {
            Text(
                text = "频段功率",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                BandPowerIndicator(
                    label = "Beta",
                    value = betaHistory.lastOrNull() ?: 0.0,
                    color = StageAwake,
                    range = "13-30 Hz",
                    modifier = Modifier.weight(1f)
                )

                BandPowerIndicator(
                    label = "VLF",
                    value = vlfHistory.lastOrNull() ?: 0.0,
                    color = StageN2,
                    range = "0.003-0.03 Hz",
                    modifier = Modifier.weight(1f)
                )

                BandPowerIndicator(
                    label = "EMG",
                    value = emgHistory.lastOrNull() ?: 0.0,
                    color = StageREM,
                    range = "30-100 Hz",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BandPowerIndicator(
    label: String,
    value: Double,
    color: Color,
    range: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(InputBackground)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = color
        )
        Text(
            text = String.format("%.2f", value),
            style = MaterialTheme.typography.bodyMediumMono,
            color = TextPrimary
        )
        Text(
            text = range,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}

@Composable
private fun StateCard(
    currentState: DetectionState,
    currentSleepStage: SleepStage
) {
    SomnilCard {
        Column {
            Text(
                text = "当前状态",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("睡眠阶段", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(getStageColor(currentSleepStage))
                        )
                        Text(
                            text = currentSleepStage.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("检测状态", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
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
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = currentState.displayText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = getStateColor(currentState)
                        )
                    }
                }
            }
        }
    }
}

private fun formatTimestamp(ts: Long): String {
    val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(ts))
}

private fun getStateColor(state: DetectionState): Color {
    return when (state) {
        DetectionState.MONITORING -> AccentPurple
        DetectionState.ANXIETY_DETECTED -> Warning
        DetectionState.INTERVENTION_ACTIVE -> Success
        DetectionState.CALIBRATING -> AccentBlue
        else -> TextSecondary
    }
}

private fun getStageColor(stage: SleepStage): Color {
    return when (stage) {
        SleepStage.AWAKE -> StageAwake
        SleepStage.N1 -> StageN1
        SleepStage.N2 -> StageN2
        SleepStage.N3 -> StageN3
        SleepStage.REM -> StageREM
        SleepStage.UNKNOWN -> StageUnknown
    }
}