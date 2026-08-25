package com.avantgardelabs.healthyhabitstracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF6600), // Hacker News Orange
    onPrimary = Color.White,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onBackground = Color(0xFFE0E0E0),
    onSurface = Color(0xFFE0E0E0),
    outline = Color(0xFF333333)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFFF6600), // Hacker News Orange
    onPrimary = Color.White,
    background = Color(0xFFF6F6EF), // Hacker News Off-white
    surface = Color(0xFFF6F6EF),
    onBackground = Color(0xFF1A1A1A),
    onSurface = Color(0xFF1A1A1A),
    outline = Color(0xFFCCCCCC)
)

@Composable
fun HealthyHabitsTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    primaryColor: Color = Color(0xFF1B5E20), // Default Material Dark Green
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = primaryColor,
            onPrimary = Color.White,
            background = Color(0xFF121212),
            surface = Color(0xFF1E1E1E),
            onBackground = Color(0xFFE0E0E0),
            onSurface = Color(0xFFE0E0E0),
            outline = Color(0xFF333333)
        )
    } else {
        lightColorScheme(
            primary = primaryColor,
            onPrimary = Color.White,
            background = Color(0xFFF6F6EF), // Warm Hacker News style flat background
            surface = Color(0xFFF6F6EF),
            onBackground = Color(0xFF1A1A1A),
            onSurface = Color(0xFF1A1A1A),
            outline = Color(0xFFCCCCCC)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}