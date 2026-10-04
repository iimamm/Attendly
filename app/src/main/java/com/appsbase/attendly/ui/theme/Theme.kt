package com.appsbase.attendly.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = PrimaryBlueContainer,
    onPrimaryContainer = TitleNavy,
    secondary = LabelGrey,
    onSecondary = Color.White,
    surface = Color.White,
    onSurface = Color(0xFF2D3340),
    onSurfaceVariant = BodyGrey,
    background = ScreenBackground,
    onBackground = Color(0xFF2D3340),
    outline = TrackGrey,
    error = DangerRed,
    onError = Color.White,
    errorContainer = DangerContainer,
    onErrorContainer = DangerRed
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlueLight,
    onPrimary = TitleNavy,
    primaryContainer = PrimaryBlueContainerDark,
    onPrimaryContainer = Color.White,
    secondary = HintGrey,
    onSecondary = DarkSurface,
    surface = DarkSurface,
    onSurface = Color(0xFFE6E9F0),
    onSurfaceVariant = HintGrey,
    background = DarkBackground,
    onBackground = Color(0xFFE6E9F0),
    outline = DarkSurfaceVariant,
    error = DangerRed,
    onError = Color.White,
    errorContainer = DangerContainer,
    onErrorContainer = DangerRed
)

@Composable
fun AttendlyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        content = content
    )
}
