package com.somnil.app.ui.screens.smarthome

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.somnil.app.data.model.EcosystemGuide
import com.somnil.app.data.model.GuideStep
import com.somnil.app.ui.theme.*
import kotlinx.coroutines.launch

/**
 * SmartHomeGuideScreen — step-by-step guide with ViewPager2-style pager.
 * Shows one step at a time with prev/next navigation and dot indicators.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SmartHomeGuideScreen(
    ecosystem: EcosystemGuide,
    onBack: () -> Unit
) {
    val steps = ecosystem.steps
    val pagerState = rememberPagerState(pageCount = { steps.size })
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var copiedText by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = ecosystem.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = ecosystem.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = TextPrimary
                        )
                    }
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
        ) {
            // ── Progress indicator (dots) ─────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                steps.forEachIndexed { index, _ ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (isSelected) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) AccentPurple else CardBorder)
                    )
                }
            }

            // ── Step counter ──────────────────────────────────────────────
            Text(
                text = "第 ${pagerState.currentPage + 1} 步，共 ${steps.size} 步",
                style = MaterialTheme.typography.labelMedium,
                color = AccentBlue,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            // ── Pager (step content) ──────────────────────────────────────
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                val step = steps[page]
                StepContent(
                    step = step,
                    isLastStep = page == steps.lastIndex,
                    copiedText = copiedText,
                    onCopy = { text ->
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("MQTT Config", text)
                        clipboard.setPrimaryClip(clip)
                        copiedText = text
                        Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // ── Navigation buttons ────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous button
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            if (pagerState.currentPage > 0) {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        }
                    },
                    enabled = pagerState.currentPage > 0,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextPrimary,
                        disabledContentColor = TextMuted
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(52.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("上一步")
                }

                // Next / Finish button
                Button(
                    onClick = {
                        scope.launch {
                            if (pagerState.currentPage < steps.size - 1) {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            } else {
                                // Last step done
                                onBack()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentPurple,
                        contentColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(52.dp)
                ) {
                    Text(if (pagerState.currentPage == steps.size - 1) "完成" else "下一步")
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        if (pagerState.currentPage == steps.size - 1)
                            Icons.Default.ContentCopy
                        else
                            Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StepContent(
    step: GuideStep,
    isLastStep: Boolean,
    copiedText: String?,
    onCopy: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Step title
        Text(
            text = step.title,
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Progress badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(AccentPurple.copy(alpha = 0.15f))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = "${step.stepNumber} / ${step.totalSteps}",
                style = MaterialTheme.typography.labelMedium,
                color = AccentPurple
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Screenshot hint placeholder (dashed border box)
        if (step.screenshotHint != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(InputBackground),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "📸",
                        style = MaterialTheme.typography.headlineLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = step.screenshotHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Instruction text
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CardBackground)
                .padding(16.dp)
        ) {
            Text(
                text = step.instruction,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.3f
            )
        }

        // Copyable text section (e.g. MQTT config)
        if (step.copyableText != null) {
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(InputBackground)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = step.copyableText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AccentBlue,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = { onCopy(step.copyableText) },
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = if (copiedText == step.copyableText) Success else AccentPurple
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "复制"
                    )
                }
            }

            Text(
                text = if (copiedText == step.copyableText) "已复制 ✓" else "点击复制按钮复制配置",
                style = MaterialTheme.typography.labelSmall,
                color = if (copiedText == step.copyableText) Success else TextMuted,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
