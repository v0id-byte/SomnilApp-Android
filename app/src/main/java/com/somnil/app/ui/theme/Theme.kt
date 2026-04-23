package com.somnil.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val SomnilDarkColorScheme = darkColorScheme(
    primary = AccentPurple,
    onPrimary = TextPrimary,
    primaryContainer = CardBackground,
    onPrimaryContainer = TextPrimary,
    secondary = AccentBlue,
    onSecondary = BackgroundDark,
    secondaryContainer = BackgroundLight,
    onSecondaryContainer = TextPrimary,
    tertiary = Success,
    onTertiary = BackgroundDark,
    error = Warning,
    onError = TextPrimary,
    errorContainer = Warning.copy(alpha = 0.2f),
    onErrorContainer = Warning,
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = CardBackground,
    onSurface = TextPrimary,
    surfaceVariant = InputBackground,
    onSurfaceVariant = TextSecondary,
    outline = CardBorder,
    outlineVariant = CardBorder.copy(alpha = 0.5f),
    scrim = BackgroundDark.copy(alpha = 0.8f)
)

@Composable
fun SomnilTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = SomnilDarkColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = BackgroundDark.toArgb()
            window.navigationBarColor = BackgroundDark.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SomnilTypography,
        content = content
    )
}
