package com.somnil.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.somnil.app.ui.theme.*

/**
 * Somnil card - mirrors iOS .somnilCard() modifier.
 * Dark card with subtle border, corner radius 16, padding 16.
 */
@Composable
fun SomnilCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground.copy(alpha = 0.8f))
            .border(
                width = 1.dp,
                color = CardBorder.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp),
        content = content
    )
}

/**
 * Gradient button - mirrors iOS primary action button style.
 */
@Composable
fun SomnilPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    gradient: List<Color> = listOf(AccentPurple, AccentPurpleLight)
) {
    androidx.compose.material3.Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp)),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = if (enabled) Brush.horizontalGradient(gradient)
                    else Brush.horizontalGradient(listOf(InputBackground, InputBackground)),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = if (enabled) TextPrimary else TextSecondary
            )
        }
    }
}

/**
 * Danger/outlined button - mirrors iOS .somnilWarning style.
 */
@Composable
fun SomnilDangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    androidx.compose.material3.Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = if (enabled) Warning else InputBackground,
            contentColor = TextPrimary,
            disabledContainerColor = InputBackground,
            disabledContentColor = TextSecondary
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * Section header with icon - mirrors iOS card section headers.
 */
@Composable
fun SomnilSectionHeader(
    icon: String,
    title: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text(
            text = "  ", // Icon placeholder - use Icon composable in actual screens
            style = MaterialTheme.typography.headlineSmall,
            color = AccentPurple
        )
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary
        )
    }
}

/**
 * Signal bars view - mirrors iOS SignalBarsView.
 * 4 bars, colored by signal strength.
 */
@Composable
fun SignalBarsView(
    rssi: Int,
    modifier: Modifier = Modifier
) {
    val bars = when {
        rssi > -50 -> 4
        rssi > -60 -> 3
        rssi > -70 -> 2
        rssi > -80 -> 1
        else -> 0
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = androidx.compose.ui.Alignment.Bottom
    ) {
        for (i in 0 until 4) {
            val height = (6 + i * 3).dp
            val color = if (i < bars) AccentBlue else CardBorder
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(height)
                    .clip(RoundedCornerShape(1.dp))
                    .background(color)
            )
        }
    }
}

/**
 * Live indicator badge - mirrors iOS "LIVE" badge.
 */
@Composable
fun LiveBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(
                color = Success.copy(alpha = 0.15f),
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Success)
        )
        Text(
            text = "LIVE",
            style = MaterialTheme.typography.labelSmall,
            color = Success
        )
    }
}

/**
 * Sleep stage color dot - mirrors iOS Circle().fill(Color(hex: stage.color)).
 */
@Composable
fun SleepStageDot(
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(10.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(color)
    )
}

/**
 * Quality score badge - mirrors iOS QualityBadge.
 * Circular badge with score number, color coded.
 */
@Composable
fun QualityScoreBadge(
    score: Int,
    modifier: Modifier = Modifier
) {
    val color = when {
        score >= 80 -> Success
        score >= 60 -> AccentBlue
        score >= 40 -> AccentPurple
        else -> Warning
    }

    Box(
        modifier = modifier
            .size(50.dp)
            .clip(RoundedCornerShape(25.dp))
            .background(color),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text(
            text = score.toString(),
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary
        )
    }
}

/**
 * Sleep stage bar - mirrors iOS SleepStageBar.
 * Horizontal bar showing proportion of each sleep stage.
 */
@Composable
fun SleepStageBar(
    durations: Map<com.somnil.app.domain.model.SleepStage, Double>,
    totalDuration: Double,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
    ) {
        com.somnil.app.domain.model.SleepStage.entries.forEach { stage ->
            val duration = durations[stage] ?: 0.0
            val ratio = if (totalDuration > 0) duration / totalDuration else 0.0
            val width = if (ratio > 0) (maxOf(modifier as? androidx.compose.ui.layout.LayoutWidthModifier?.let { ratio * 100 } ?: 0.0, 2.0)).dp else 0.dp

            Box(
                modifier = Modifier
                    .weight(if (ratio > 0) maxOf(ratio, 0.02).toFloat().coerceAtLeast(0.02f) else 0.01f)
                    .fillMaxHeight()
                    .background(getStageColor(stage))
            )
        }
    }
}

private fun getStageColor(stage: com.somnil.app.domain.model.SleepStage): Color {
    return when (stage) {
        com.somnil.app.domain.model.SleepStage.AWAKE -> StageAwake
        com.somnil.app.domain.model.SleepStage.N1 -> StageN1
        com.somnil.app.domain.model.SleepStage.N2 -> StageN2
        com.somnil.app.domain.model.SleepStage.N3 -> StageN3
        com.somnil.app.domain.model.SleepStage.REM -> StageREM
        com.somnil.app.domain.model.SleepStage.UNKNOWN -> StageUnknown
    }
}
