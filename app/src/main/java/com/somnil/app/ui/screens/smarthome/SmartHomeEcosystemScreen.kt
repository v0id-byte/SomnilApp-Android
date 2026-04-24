package com.somnil.app.ui.screens.smarthome

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.somnil.app.data.model.EcosystemGuide
import com.somnil.app.data.model.EcosystemPriority
import com.somnil.app.data.model.SmartHomeGuideRepository
import com.somnil.app.ui.theme.*

/**
 * SmartHomeEcosystemScreen — first page: pick which smart home platform to configure.
 * Matches the design spec: large emoji buttons, high-contrast dark theme, priority badges.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartHomeEcosystemScreen(
    onEcosystemSelected: (EcosystemGuide) -> Unit
) {
    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "配置智能家居",
                        style = MaterialTheme.typography.headlineLarge,
                        color = TextPrimary
                    )
                },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = "选择你的智能家居平台，我们将一步步引导你完成配置。",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // HIGH priority section
            if (SmartHomeGuideRepository.allEcosystems.any { it.priority == EcosystemPriority.HIGH }) {
                Text(
                    text = "高优先级",
                    style = MaterialTheme.typography.labelLarge,
                    color = AccentPurple,
                    modifier = Modifier.padding(top = 8.dp)
                )
                SmartHomeGuideRepository.allEcosystems
                    .filter { it.priority == EcosystemPriority.HIGH }
                    .forEach { ecosystem ->
                        EcosystemCard(
                            ecosystem = ecosystem,
                            onClick = { onEcosystemSelected(ecosystem) }
                        )
                    }
            }

            // MEDIUM priority section
            if (SmartHomeGuideRepository.allEcosystems.any { it.priority == EcosystemPriority.MEDIUM }) {
                Text(
                    text = "其他平台",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 8.dp)
                )
                SmartHomeGuideRepository.allEcosystems
                    .filter { it.priority == EcosystemPriority.MEDIUM }
                    .forEach { ecosystem ->
                        EcosystemCard(
                            ecosystem = ecosystem,
                            onClick = { onEcosystemSelected(ecosystem) }
                        )
                    }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun EcosystemCard(
    ecosystem: EcosystemGuide,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Emoji icon
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(InputBackground),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = ecosystem.iconEmoji,
                    style = MaterialTheme.typography.headlineLarge
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = ecosystem.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary
                )
                Text(
                    text = ecosystem.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    text = "${ecosystem.steps.size} 步引导",
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentBlue
                )
            }
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "进入",
            tint = TextSecondary,
            modifier = Modifier.size(24.dp)
        )
    }
}
