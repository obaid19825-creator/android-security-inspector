package com.securityinspector.app.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.securityinspector.app.domain.model.RiskLevel

private val DarkColorScheme = darkColorScheme(
    primary = CyberPrimaryDark,
    secondary = CyberSecondaryDark,
    background = CyberBackgroundDark,
    surface = CyberSurfaceDark,
    surfaceVariant = CyberSurfaceVariantDark,
    onBackground = CyberOnBackgroundDark,
    onSurface = CyberOnSurfaceDark,
    onPrimary = Color(0xFF00352E),
    onSecondary = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = CyberPrimaryLight,
    secondary = CyberSecondaryLight,
    background = CyberBackgroundLight,
    surface = CyberSurfaceLight,
    surfaceVariant = CyberSurfaceVariantLight,
    onBackground = CyberOnBackgroundLight,
    onSurface = CyberOnSurfaceLight,
    onPrimary = Color.White,
    onSecondary = Color.White
)

@Composable
fun SecurityInspectorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SecurityInspectorTypography,
        content = content
    )
}

/** Maps a [RiskLevel] to its semantic display color, independent of light/dark theme. */
fun RiskLevel.toColor(): Color = when (this) {
    RiskLevel.SAFE -> RiskSafe
    RiskLevel.LOW -> RiskLow
    RiskLevel.MEDIUM -> RiskMedium
    RiskLevel.HIGH -> RiskHigh
    RiskLevel.CRITICAL -> RiskCritical
}
