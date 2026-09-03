package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import com.example.ui.AppThemeMode

private fun getContrastingOnColor(color: Color): Color {
    return if (color.luminance() > 0.55f) Color.Black else Color.White
}

@Composable
fun MyApplicationTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    accentColor: Color = AgriRedPrimary,
    palette: AppPalette? = null,
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

    // Derive effective palette: if explicit palette is provided use it, otherwise wrap accentColor
    val effectivePalette = palette ?: AppPalette(
        id = "solid_active",
        name = "Solid Accent",
        isTwoColor = false,
        primaryHex = String.format("#%06X", (0xFFFFFF and accentColor.value.toInt())),
        secondaryHex = String.format("#%06X", (0xFFFFFF and accentColor.value.toInt())),
        tertiaryHex = String.format("#%06X", (0xFFFFFF and accentColor.value.toInt())),
        neutralHex = "#E2E8F0",
        primaryName = "Primary",
        secondaryName = "Secondary",
        tertiaryName = "Tertiary",
        neutralName = "Neutral"
    )

    // In 2-color palettes (Monochrome / Black & White):
    // In dark/AMOLED modes: Primary (Text/Icons) = White, Secondary (Backgrounds) = Black.
    // In light mode: Primary (Text/Icons) = Black, Secondary (Backgrounds) = White.
    val effectivePrimary = when {
        effectivePalette.isTwoColor && (isDark || isAmoled) -> effectivePalette.secondary // White in Dark/AMOLED
        else -> effectivePalette.primary
    }

    val effectiveSecondary = when {
        effectivePalette.isTwoColor && (isDark || isAmoled) -> effectivePalette.primary // Black in Dark/AMOLED
        else -> effectivePalette.secondary
    }

    val effectiveTertiary = effectivePalette.tertiary

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isAmoled -> {
            val amoledBg = Color.Black
            darkColorScheme(
                primary = effectivePrimary,
                onPrimary = getContrastingOnColor(effectivePrimary),
                primaryContainer = effectivePrimary.copy(alpha = 0.25f),
                onPrimaryContainer = Color.White,
                secondary = effectiveSecondary,
                onSecondary = getContrastingOnColor(effectiveSecondary),
                secondaryContainer = effectiveSecondary.copy(alpha = 0.18f),
                onSecondaryContainer = Color.White,
                tertiary = effectiveTertiary,
                onTertiary = getContrastingOnColor(effectiveTertiary),
                tertiaryContainer = effectiveTertiary.copy(alpha = 0.20f),
                onTertiaryContainer = Color.White,
                background = amoledBg,
                surface = amoledBg,
                surfaceVariant = if (effectivePalette.isTwoColor) Color(0xFF121212) else effectiveSecondary.copy(alpha = 0.12f),
                surfaceContainer = Color(0xFF0A0A0A),
                surfaceContainerHigh = Color(0xFF141414),
                onBackground = effectivePalette.getNeutralTextColor(isDark = true),
                onSurface = effectivePalette.getNeutralTextColor(isDark = true),
                onSurfaceVariant = if (effectivePalette.isTwoColor) Color(0xFFA1A1AA) else effectivePalette.neutral.copy(alpha = 0.80f),
                outline = effectivePalette.getNeutralBorderColor(isDark = true),
                outlineVariant = effectivePalette.getNeutralBorderColor(isDark = true).copy(alpha = 0.25f)
            )
        }
        isDark -> {
            val darkBg = getAppDimBackgroundColor(effectivePrimary, isDark = true, isAmoled = false)
            val darkSurface = Color(0xFF1E1C24)
            val darkSurfaceVariant = if (effectivePalette.isTwoColor) Color(0xFF282531) else effectiveSecondary.copy(alpha = 0.15f)
            val darkSurfaceContainer = Color(0xFF22202A)
            val darkSurfaceContainerHigh = Color(0xFF2D2A37)
            darkColorScheme(
                primary = effectivePrimary,
                onPrimary = getContrastingOnColor(effectivePrimary),
                primaryContainer = effectivePrimary.copy(alpha = 0.25f),
                onPrimaryContainer = Color.White,
                secondary = effectiveSecondary,
                onSecondary = getContrastingOnColor(effectiveSecondary),
                secondaryContainer = effectiveSecondary.copy(alpha = 0.18f),
                onSecondaryContainer = Color.White,
                tertiary = effectiveTertiary,
                onTertiary = getContrastingOnColor(effectiveTertiary),
                tertiaryContainer = effectiveTertiary.copy(alpha = 0.20f),
                onTertiaryContainer = Color.White,
                background = darkBg,
                surface = darkSurface,
                surfaceVariant = darkSurfaceVariant,
                surfaceContainer = darkSurfaceContainer,
                surfaceContainerHigh = darkSurfaceContainerHigh,
                onBackground = effectivePalette.getNeutralTextColor(isDark = true),
                onSurface = effectivePalette.getNeutralTextColor(isDark = true),
                onSurfaceVariant = if (effectivePalette.isTwoColor) Color(0xFFCBD5E1) else effectivePalette.neutral.copy(alpha = 0.80f),
                outline = effectivePalette.getNeutralBorderColor(isDark = true),
                outlineVariant = effectivePalette.getNeutralBorderColor(isDark = true).copy(alpha = 0.25f)
            )
        }
        else -> {
            val lightBg = getAppDimBackgroundColor(effectivePrimary, isDark = false, isAmoled = false)
            lightColorScheme(
                primary = effectivePrimary,
                onPrimary = getContrastingOnColor(effectivePrimary),
                primaryContainer = effectivePrimary.copy(alpha = 0.12f),
                onPrimaryContainer = effectivePrimary,
                secondary = effectiveSecondary,
                onSecondary = getContrastingOnColor(effectiveSecondary),
                secondaryContainer = effectiveSecondary.copy(alpha = 0.14f),
                onSecondaryContainer = Color(0xFF1E293B),
                tertiary = effectiveTertiary,
                onTertiary = getContrastingOnColor(effectiveTertiary),
                tertiaryContainer = effectiveTertiary.copy(alpha = 0.15f),
                onTertiaryContainer = Color(0xFF1E293B),
                background = lightBg,
                surface = Color.White,
                surfaceVariant = if (effectivePalette.isTwoColor) Color(0xFFF1F5F9) else effectivePalette.neutral.copy(alpha = 0.35f),
                surfaceContainerHigh = Color(0xFFE2E8F0),
                onBackground = effectivePalette.getNeutralTextColor(isDark = false),
                onSurface = effectivePalette.getNeutralTextColor(isDark = false),
                onSurfaceVariant = if (effectivePalette.isTwoColor) AgriTextSecondary else Color(0xFF475569),
                outline = effectivePalette.getNeutralBorderColor(isDark = false),
                outlineVariant = effectivePalette.getNeutralBorderColor(isDark = false).copy(alpha = 0.35f)
            )
        }
    }

    CompositionLocalProvider(LocalAppPalette provides effectivePalette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
