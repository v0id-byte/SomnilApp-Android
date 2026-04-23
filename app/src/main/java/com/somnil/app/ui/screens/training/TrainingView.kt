package com.somnil.app.ui.screens.training

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.somnil.app.domain.model.TrainingPhase
import com.somnil.app.ui.theme.*

/**
 * Training tab view - mirrors iOS TrainingView.
 * Shows 3-day training phase progress, daily cards, stats, and recommendations.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingView(
    viewModel: TrainingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "训练期",
                        style = MaterialTheme.typography.headlineLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundDark,
                    titleContentColor = TextPrimary
                ),
                actions = {
                    // Reset button (visible only after completion)
                    if (uiState.trainingPhase.isCompleted) {
                        IconButton(onClick = { viewModel.resetTraining() }) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "重新训练",
                                tint = TextSecondary
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ── Progress Header Card ──────────────────────────────────────────
            ProgressHeaderCard(
                phase = uiState.trainingPhase,
                isTrainingCompleted = uiState.isTrainingCompleted,
                personalizedThreshold = uiState.personalizedThreshold
            )

            // ── Day Cards ────────────────────────────────────────────────────
            DayCardsSection(phase = uiState.trainingPhase)

            // ── Recommendation Card ───────────────────────────────────────────
            RecommendationCard(phase = uiState.trainingPhase)

            // ── Quick Stats Card ─────────────────────────────────────────────
            QuickStatsCard(phase = uiState.trainingPhase)

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

// ─── Progress Header Card ────────────────────────────────────────────────────

@Composable
private fun ProgressHeaderCard(
    phase: TrainingPhase,
    isTrainingCompleted: Boolean,
    personalizedThreshold: Double
) {
    val animatedProgress by animateFloatAsState(
        targetValue = phase.progress.toFloat(),
        animationSpec = spring(),
        label = "progress"
    )

    SomnilCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Circular progress
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(130.dp)
            ) {
                // Background circle
                Canvas(modifier = Modifier.size(130.dp)) {
                    drawArc(
                        color = CardBorder,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round),
                        size = Size(size.width, size.height)
                    )
                }
                // Progress arc
                Canvas(modifier = Modifier.size(130.dp)) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(AccentPurple, AccentBlue, AccentPurple)
                        ),
                        startAngle = -90f,
                        sweepAngle = animatedProgress * 360f,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round),
                        size = Size(size.width, size.height)
                    )
                }
                // Center text
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${(phase.progress * 100).toInt()}%",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentBlue
                    )
                    Text(
                        text = "${phase.completedDays}/3 天",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            // Status text
            Text(
                text = phase.statusText,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            // Completion badge
            if (phase.isCompleted) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .background(
                            Success.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Success,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "适配完成",
                        fontSize = 14.sp,
                        color = Success
                    )
                }
            }

            // Personalized threshold (shown after training complete)
            if (isTrainingCompleted) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Success,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "个性化阈值已建立：%.2f".format(personalizedThreshold),
                        fontSize = 13.sp,
                        color = Success
                    )
                }
            }
        }
    }
}

// ─── Day Cards Section ───────────────────────────────────────────────────────

@Composable
private fun DayCardsSection(phase: TrainingPhase) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "每日进度",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )

        for (day in 1..3) {
            DayProgressCard(
                day = day,
                isCompleted = day <= phase.completedDays,
                isCurrent = day == phase.completedDays + 1 && !phase.isCompleted,
                description = phase.dayDescription(day)
            )
        }
    }
}

@Composable
private fun DayProgressCard(
    day: Int,
    isCompleted: Boolean,
    isCurrent: Boolean,
    description: String
) {
    val circleColor = when {
        isCompleted -> Success
        isCurrent -> AccentPurple
        else -> CardBorder
    }

    val textColor = when {
        isCompleted || isCurrent -> TextPrimary
        else -> TextSecondary
    }

    val borderColor = if (isCurrent) AccentPurple.copy(alpha = 0.5f) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBackground.copy(alpha = 0.8f))
            .border(
                width = if (isCurrent) 1.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Day indicator circle
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(circleColor),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Text(
                    text = "$day",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCurrent) Color.White else TextSecondary
                )
            }
        }

        // Day info
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "第 $day 天",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
            Text(
                text = description,
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Status badge
        val badgeColor = when {
            isCompleted -> Success
            isCurrent -> AccentPurple
            else -> TextSecondary
        }
        val badgeBgColor = when {
            isCompleted -> Success.copy(alpha = 0.15f)
            isCurrent -> AccentPurple.copy(alpha = 0.15f)
            else -> CardBorder.copy(alpha = 0.3f)
        }
        val badgeText = when {
            isCompleted -> "已完成"
            isCurrent -> "进行中"
            else -> "未开始"
        }

        Box(
            modifier = Modifier
                .background(badgeBgColor, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = badgeText,
                fontSize = 12.sp,
                color = badgeColor
            )
        }
    }
}

// ─── Recommendation Card ──────────────────────────────────────────────────────

@Composable
private fun RecommendationCard(phase: TrainingPhase) {
    SomnilCard {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = AccentPurple,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "建议",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }

            // Main recommendation text
            Text(
                text = phase.recommendationText,
                fontSize = 14.sp,
                color = TextSecondary,
                lineHeight = 20.sp
            )

            // Next training night (only if not completed)
            if (!phase.isCompleted) {
                HorizontalDivider(color = CardBorder.copy(alpha = 0.3f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(InputBackground, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.NightsStay,
                        contentDescription = null,
                        tint = AccentBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "今晚开始第 ${phase.completedDays + 1} 天训练",
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}

// ─── Quick Stats Card ────────────────────────────────────────────────────────

@Composable
private fun QuickStatsCard(phase: TrainingPhase) {
    SomnilCard {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.BarChart,
                    contentDescription = null,
                    tint = AccentPurple,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "统计",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    icon = Icons.Default.NightsStay,
                    value = "${phase.totalSessions}",
                    label = "训练次数",
                    color = AccentBlue
                )
                StatItem(
                    icon = Icons.Default.CheckCircle,
                    value = "${phase.successfulAdaptations}",
                    label = "成功适配",
                    color = Success
                )
                StatItem(
                    icon = Icons.Default.Timer,
                    value = "${phase.daysRemaining}",
                    label = "剩余天数",
                    color = Warning
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.weight(1f)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}
