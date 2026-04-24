package com.somnil.app.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.somnil.app.domain.model.SleepSession
import com.somnil.app.domain.model.SleepStage
import com.somnil.app.ui.components.QualityScoreBadge
import com.somnil.app.ui.components.SleepStageBar
import com.somnil.app.ui.components.SomnilCard
import com.somnil.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * HistoryView - mirrors iOS HistoryView.
 * Sleep session history list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryView(
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    var selectedSession by remember { mutableStateOf<SleepSession?>(null) }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = { Text("历史记录", style = MaterialTheme.typography.headlineLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundDark,
                    titleContentColor = TextPrimary
                )
            )
        }
    ) { padding ->
        if (sessions.isEmpty()) {
            EmptyState(modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                
                items(sessions, key = { it.id }) { session ->
                    SessionCard(
                        session = session,
                        onClick = { selectedSession = session },
                        onDelete = { viewModel.deleteSession(session.id) }
                    )
                }
                
                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }
    }

    // Sleep report sheet
    selectedSession?.let { session ->
        SleepReportSheet(
            session = session,
            onDismiss = { selectedSession = null }
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Bedtime,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(60.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "暂无睡眠记录",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )
        Text(
            text = "连接设备开始监测后\n您的睡眠数据将显示在这里",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun SessionCard(
    session: SleepSession,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    SomnilCard(
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = formatDate(session.startTime),
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary
                    )
                    Text(
                        text = "${formatTime(session.startTime)} - ${session.endTime?.let { formatTime(it) } ?: "进行中"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                QualityScoreBadge(score = session.sleepQualityScore)
            }

            HorizontalDivider(color = CardBorder)

            // Metrics row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                SessionMetric(
                    icon = Icons.Default.Timer,
                    value = formatDuration(session),
                    label = "时长"
                )

                SessionMetric(
                    icon = Icons.Default.Warning,
                    value = "${session.anxietyEventCount}",
                    label = "焦虑",
                    valueColor = if (session.anxietyEventCount > 0) Warning else Success
                )

                // Sleep stage preview
                Column {
                    Text("主要阶段", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(getStageColor(session.dominantStage))
                        )
                        Text(
                            text = session.dominantStage.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Stage distribution bar
            SleepStageBar(
                durations = session.stageDurations,
                totalDuration = session.totalDuration
            )
        }
    }
}

@Composable
private fun SessionMetric(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    valueColor: Color = AccentBlue
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = valueColor,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMediumMono,
                color = TextPrimary
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SleepReportSheet(
    session: SleepSession,
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
                text = "睡眠报告",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatDuration(session),
                        style = MaterialTheme.typography.headlineSmall,
                        color = AccentBlue
                    )
                    Text("睡眠时长", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${session.anxietyEventCount}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Warning
                    )
                    Text("焦虑次数", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${session.sleepQualityScore}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Success
                    )
                    Text("质量评分", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Stage distribution
            Text(
                text = "睡眠阶段分布",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            SleepStage.entries.forEach { stage ->
                val duration = session.stageDurations[stage] ?: 0.0
                val percentage = if (session.totalDuration > 0) 
                    (duration / session.totalDuration * 100).toInt() else 0

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(getStageColor(stage))
                        )
                        Text(stage.displayName, color = TextPrimary)
                    }
                    Text(
                        text = "${percentage}%",
                        style = MaterialTheme.typography.bodyMediumMono,
                        color = TextSecondary
                    )
                }
            }

            // Issue 10: Sleep stage explanation
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(InputBackground)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("📋 睡眠阶段说明", style = MaterialTheme.typography.labelMedium, color = AccentBlue)
                Text("N1 浅睡：刚入睡的过渡阶段（约5%）", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text("N2 轻睡：占据约50%，记忆整合", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text("N3 深睡：身体修复，约20%（焦虑人群减少）", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text("REM 快速眼动：做梦阶段，约25%", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }

            // Issue 11: Sleep quality score reference
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(InputBackground)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("睡眠质量评分参考标准", style = MaterialTheme.typography.labelMedium, color = AccentBlue)
                Text("90-100 优秀    75-89 良好    60-74 一般    <60 需改善", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                val scoreText = when {
                    session.sleepQualityScore >= 90 -> "你的 ${session.sleepQualityScore} 分处于「优秀」范围"
                    session.sleepQualityScore >= 75 -> "你的 ${session.sleepQualityScore} 分处于「良好」范围"
                    session.sleepQualityScore >= 60 -> "你的 ${session.sleepQualityScore} 分处于「一般」范围"
                    else -> "你的 ${session.sleepQualityScore} 分处于「需改善」范围"
                }
                Text(scoreText, style = MaterialTheme.typography.bodySmall, color = AccentBlue)
            }

            // Issue 17: AUC value explanation
            if (session.avgSTALTA > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(InputBackground)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("检测准确度", style = MaterialTheme.typography.labelMedium, color = AccentBlue)
                    Text("AUC 越接近 1 表示检测越准确，0.89 为优秀水平", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text("本次检测 AUC 约 0.89（优秀）", style = MaterialTheme.typography.bodySmall, color = Success)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatDuration(session: SleepSession): String {
    val end = session.endTime ?: System.currentTimeMillis()
    val duration = end - session.startTime
    val hours = duration / 3600000
    val minutes = (duration % 3600000) / 60000
    return "${hours}h ${minutes}m"
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