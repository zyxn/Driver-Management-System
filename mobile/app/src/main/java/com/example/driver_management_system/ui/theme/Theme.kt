package com.example.driver_management_system.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    secondary = PrimaryHover,
    tertiary = PrimaryActive,
    background = Color(0xFF141414),
    surface = Color(0xFF1F1F1F),
    surfaceVariant = Color(0xFF262626),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFFFFFFFF),
    onSurface = Color(0xFFFFFFFF),
    error = Error,
    outline = Color(0xFF434343)
)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    secondary = PrimaryHover,
    tertiary = PrimaryActive,
    background = BackgroundBase,
    surface = BackgroundWhite,
    surfaceVariant = BackgroundLight,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = Error,
    outline = BorderBase,
    outlineVariant = BorderLight
)

@Composable
fun DriverManagementSystemTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

