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
        isAmoled -> {
            val amoledBg = getAppDimBackgroundColor(accentColor, isDark = true, isAmoled = true)
            darkColorScheme(
                primary = accentColor,
                onPrimary = Color.White,
                primaryContainer = accentColor.copy(alpha = 0.25f),
                onPrimaryContainer = Color.White,
                secondary = accentColor,
                background = amoledBg,
                surface = amoledBg,
                surfaceVariant = Color(0xFF0F0F12),
                onBackground = Color(0xFFFAFAFA),
                onSurface = Color(0xFFFAFAFA),
                onSurfaceVariant = Color(0xFFA3A3A3),
                outline = Color(0xFF262626)
            )
        }
        isDark -> {
            val darkBg = getAppDimBackgroundColor(accentColor, isDark = true, isAmoled = false)
            val hsv = FloatArray(3)
            android.graphics.Color.RGBToHSV(
                (accentColor.red * 255f).toInt().coerceIn(0, 255),
                (accentColor.green * 255f).toInt().coerceIn(0, 255),
                (accentColor.blue * 255f).toInt().coerceIn(0, 255),
                hsv
            )
            val darkSurface = Color(android.graphics.Color.HSVToColor(floatArrayOf(hsv[0], 0.10f, 0.13f)))
            darkColorScheme(
                primary = accentColor,
                onPrimary = Color.White,
                primaryContainer = accentColor.copy(alpha = 0.25f),
                onPrimaryContainer = Color.White,
                secondary = accentColor,
                background = darkBg,
                surface = darkSurface,
                surfaceVariant = Color(android.graphics.Color.HSVToColor(floatArrayOf(hsv[0], 0.08f, 0.16f))),
                onBackground = Color.White,
                onSurface = Color.White,
                onSurfaceVariant = Color(0xFFB0B0B0),
                outline = Color(0xFF383838)
            )
        }
        else -> {
            val lightBg = getAppDimBackgroundColor(accentColor, isDark = false, isAmoled = false)
            lightColorScheme(
                primary = accentColor,
                onPrimary = Color.White,
                primaryContainer = accentColor.copy(alpha = 0.10f),
                onPrimaryContainer = lerp(accentColor, Color(0xFF0F172A), 0.60f),
                secondary = accentColor,
                secondaryContainer = accentColor.copy(alpha = 0.08f),
                background = lightBg,
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
