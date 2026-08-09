package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.data.ThemePreferencesManager
import com.example.ui.AppThemeMode

fun createColorSchemeFromSeed(
    seedColor: Color,
    darkTheme: Boolean,
    isAmoled: Boolean = false
): ColorScheme {
    val seedInt = seedColor.toArgb()
    val hsl = FloatArray(3)
    androidx.core.graphics.ColorUtils.colorToHSL(seedInt, hsl)
    val hue = hsl[0]
    val sat = hsl[1]

    fun hslColor(h: Float, s: Float, l: Float): Color {
        val normalizedHue = (h % 360f + 360f) % 360f
        val clampedSat = s.coerceIn(0f, 1f)
        val clampedLight = l.coerceIn(0f, 1f)
        val argb = androidx.core.graphics.ColorUtils.HSLToColor(floatArrayOf(normalizedHue, clampedSat, clampedLight))
        return Color(argb)
    }

    return if (!darkTheme) {
        val primary = hslColor(hue, sat.coerceAtLeast(0.45f), 0.40f)
        val onPrimary = Color.White
        val primaryContainer = hslColor(hue, sat * 0.45f, 0.90f)
        val onPrimaryContainer = hslColor(hue, sat, 0.15f)

        val secondary = hslColor(hue + 15f, sat * 0.5f, 0.42f)
        val onSecondary = Color.White
        val secondaryContainer = hslColor(hue + 15f, sat * 0.4f, 0.92f)
        val onSecondaryContainer = hslColor(hue + 15f, sat * 0.6f, 0.15f)

        val tertiary = hslColor(hue + 35f, sat * 0.5f, 0.42f)
        val onTertiary = Color.White
        val tertiaryContainer = hslColor(hue + 35f, sat * 0.4f, 0.93f)
        val onTertiaryContainer = hslColor(hue + 35f, sat * 0.6f, 0.15f)

        val background = hslColor(hue, sat * 0.08f, 0.98f)
        val onBackground = hslColor(hue, sat * 0.15f, 0.10f)
        val surface = hslColor(hue, sat * 0.05f, 0.99f)
        val onSurface = hslColor(hue, sat * 0.15f, 0.10f)
        val surfaceVariant = hslColor(hue, sat * 0.12f, 0.93f)
        val onSurfaceVariant = hslColor(hue, sat * 0.15f, 0.30f)
        val outline = hslColor(hue, sat * 0.15f, 0.50f)
        val outlineVariant = hslColor(hue, sat * 0.10f, 0.82f)

        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary,
            onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onTertiaryContainer,
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            outline = outline,
            outlineVariant = outlineVariant
        )
    } else {
        val primary = hslColor(hue, sat.coerceAtLeast(0.45f), 0.70f)
        val onPrimary = hslColor(hue, sat, 0.10f)
        val primaryContainer = hslColor(hue, sat * 0.7f, 0.25f)
        val onPrimaryContainer = hslColor(hue, sat * 0.4f, 0.92f)

        val secondary = hslColor(hue + 15f, sat * 0.5f, 0.70f)
        val onSecondary = hslColor(hue + 15f, sat, 0.10f)
        val secondaryContainer = hslColor(hue + 15f, sat * 0.6f, 0.25f)
        val onSecondaryContainer = hslColor(hue + 15f, sat * 0.4f, 0.92f)

        val tertiary = hslColor(hue + 35f, sat * 0.5f, 0.70f)
        val onTertiary = hslColor(hue + 35f, sat, 0.10f)
        val tertiaryContainer = hslColor(hue + 35f, sat * 0.6f, 0.25f)
        val onTertiaryContainer = hslColor(hue + 35f, sat * 0.4f, 0.92f)

        val background = if (isAmoled) Color.Black else hslColor(hue, sat * 0.08f, 0.07f)
        val onBackground = hslColor(hue, sat * 0.05f, 0.90f)
        val surface = if (isAmoled) Color.Black else hslColor(hue, sat * 0.08f, 0.10f)
        val onSurface = hslColor(hue, sat * 0.05f, 0.90f)
        val surfaceVariant = if (isAmoled) hslColor(hue, sat * 0.08f, 0.12f) else hslColor(hue, sat * 0.10f, 0.17f)
        val onSurfaceVariant = hslColor(hue, sat * 0.10f, 0.80f)
        val outline = hslColor(hue, sat * 0.15f, 0.55f)
        val outlineVariant = hslColor(hue, sat * 0.10f, 0.25f)

        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary,
            onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onTertiaryContainer,
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            outline = outline,
            outlineVariant = outlineVariant
        )
    }
}

@Composable
fun MyApplicationTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    accentColor: Color? = null,
    seedColorArgb: Long = ThemePreferencesManager.DEFAULT_ACCENT_COLOR_ARGB,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
        AppThemeMode.AMOLED -> true
    }
    val isAmoled = themeMode == AppThemeMode.AMOLED

    val seedColor = remember(accentColor, seedColorArgb) {
        accentColor ?: Color(seedColorArgb)
    }

    val colorScheme = remember(seedColor, darkTheme, isAmoled) {
        createColorSchemeFromSeed(seedColor, darkTheme, isAmoled)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
