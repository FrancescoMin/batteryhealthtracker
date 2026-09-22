package com.fivestars.batterytracker

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme()

private val DarkColorScheme = darkColorScheme()

private val AmoledColorScheme = darkColorScheme(
    background = Color(0xFF000000),
    surface = Color(0xFF0A0A0A),
    surfaceVariant = Color(0xFF141416),
    onBackground = Color(0xFFEDEDED),
    onSurface = Color(0xFFEDEDED),
    onSurfaceVariant = Color(0xFFC4C7C8),
    outline = Color(0xFF333338),
    outlineVariant = Color(0xFF222226),
    primaryContainer = Color(0xFF221738),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondaryContainer = Color(0xFF1E1A24),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiaryContainer = Color(0xFF261820),
    onTertiaryContainer = Color(0xFFFFD8E4)
)

@Composable
fun BatteryTrackerTheme(
    themeMode: AppThemeMode,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK, AppThemeMode.AMOLED -> true
    }

    val colorScheme: ColorScheme = when (themeMode) {
        AppThemeMode.SYSTEM -> if (isSystemDark) DarkColorScheme else LightColorScheme
        AppThemeMode.LIGHT -> LightColorScheme
        AppThemeMode.DARK -> DarkColorScheme
        AppThemeMode.AMOLED -> AmoledColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                window.statusBarColor = colorScheme.primaryContainer.toArgb()
                window.navigationBarColor = colorScheme.surface.toArgb()

                val insetsController = WindowCompat.getInsetsController(window, view)
                val isLightStatusBar = colorScheme.primaryContainer.luminance() > 0.5f
                val isLightNavBar = colorScheme.surface.luminance() > 0.5f
                insetsController.isAppearanceLightStatusBars = isLightStatusBar
                insetsController.isAppearanceLightNavigationBars = isLightNavBar
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
