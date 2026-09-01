package com.avantgardelabs.healthyhabitstracker.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Classic Material Design 1.0 Palette (inspired by 1.png and 2.png)
val MaterialBlue500 = Color(0xFF2196F3)
val MaterialBlue700 = Color(0xFF1976D2)
val MaterialBlue900 = Color(0xFF0D47A1)
val MaterialLightGrayCanvas = Color(0xFFECEFF1)
val MaterialCardWhite = Color(0xFFFFFFFF)
val MaterialTextDark = Color(0xFF263238)
val MaterialTextSecondary = Color(0xFF546E7A)
val MaterialDividerColor = Color(0xFFCFD8DC)

val MaterialGreen500 = Color(0xFF4CAF50)
val MaterialAmber500 = Color(0xFFFF9800)
val MaterialRed500 = Color(0xFFF44336)

private val ClassicMaterial1ColorScheme = lightColorScheme(
    primary = MaterialBlue500,
    onPrimary = Color.White,
    primaryContainer = MaterialBlue700,
    onPrimaryContainer = Color.White,
    secondary = MaterialBlue700,
    onSecondary = Color.White,
    background = MaterialLightGrayCanvas,
    onBackground = MaterialTextDark,
    surface = MaterialCardWhite,
    onSurface = MaterialTextDark,
    surfaceTint = Color.Transparent,
    surfaceVariant = Color(0xFFF5F7FA),
    onSurfaceVariant = MaterialTextSecondary,
    outline = MaterialDividerColor,
    outlineVariant = Color(0xFFE0E0E0),
    error = MaterialRed500,
    onError = Color.White
)

@Composable
fun HealthyHabitsTrackerTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = ClassicMaterial1ColorScheme,
        typography = Typography,
        content = content
    )
}