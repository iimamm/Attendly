package com.appsbase.attendly.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = PrimaryBlueLight,
    onPrimaryContainer = PrimaryBlueDark,
    secondary = Neutral700,
    onSecondary = Color.White,
    secondaryContainer = Neutral100,
    onSecondaryContainer = Neutral900,
    surface = Color.White,
    onSurface = Neutral900,
    background = Neutral50,
    onBackground = Neutral900,
    outline = Neutral200,
    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorRedLight,
    onErrorContainer = ErrorRed
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlueLight,
    onPrimary = PrimaryBlueDark,
    primaryContainer = PrimaryBlueContainerDark,
    onPrimaryContainer = Color.White,
    secondary = Neutral200,
    onSecondary = Neutral900,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = Color.White,
    surface = DarkSurface,
    onSurface = Color.White,
    background = DarkBackground,
    onBackground = Color.White,
    outline = Neutral700,
    error = ErrorRedLight,
    onError = ErrorRed,
    errorContainer = ErrorRed,
    onErrorContainer = Color.White
)

@Composable
fun AttendlyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // false by default to ensure brand consistency
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}