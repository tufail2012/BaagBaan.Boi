package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import com.example.ui.AppThemeMode

@Composable
fun MyApplicationTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    accentColor: Color = AgriRedPrimary,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
        AppThemeMode.AMOLED -> true
    }
    val isAmoled = themeMode == AppThemeMode.AMOLED

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isAmoled -> darkColorScheme(
            primary = accentColor,
            onPrimary = Color.White,
            primaryContainer = accentColor.copy(alpha = 0.25f),
            onPrimaryContainer = Color.White,
            secondary = accentColor,
            background = Color(0xFF000000),      // True pitch black background for AMOLED
            surface = Color(0xFF000000),         // Pure pitch black surface for AMOLED
            surfaceVariant = Color(0xFF121212),
            onBackground = Color(0xFFFAFAFA),
            onSurface = Color(0xFFFAFAFA),
            onSurfaceVariant = Color(0xFFA3A3A3),
            outline = Color(0xFF262626)
        )
        isDark -> darkColorScheme(
            primary = accentColor,
            onPrimary = Color.White,
            primaryContainer = accentColor.copy(alpha = 0.25f),
            onPrimaryContainer = Color.White,
            secondary = accentColor,
            background = Color(0xFF121212),      // Standard dark background
            surface = Color(0xFF1E1E1E),         // Standard dark surface
            surfaceVariant = Color(0xFF282828),
            onBackground = Color.White,
            onSurface = Color.White,
            onSurfaceVariant = Color(0xFFB0B0B0),
            outline = Color(0xFF383838)
        )
        else -> {
            val faintTintedBackground = lerp(accentColor, Color.White, 0.93f)
            lightColorScheme(
                primary = accentColor,
                onPrimary = Color.White,
                primaryContainer = accentColor.copy(alpha = 0.12f),
                onPrimaryContainer = lerp(accentColor, Color(0xFF0F172A), 0.60f),
                secondary = accentColor,
                secondaryContainer = accentColor.copy(alpha = 0.10f),
                background = faintTintedBackground,
                surface = Color.White,
                surfaceVariant = Color(0xFFF1F5F9),
                surfaceContainerHigh = Color(0xFFE2E8F0),
                onBackground = AgriTextPrimary,
                onSurface = AgriTextPrimary,
                onSurfaceVariant = AgriTextSecondary,
                outline = AgriOutline
            )
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
