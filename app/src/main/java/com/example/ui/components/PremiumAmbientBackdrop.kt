package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.getAppDimBackgroundBrush

/**
 * Shared premium ambient background composable for all 6 tabs:
 * Local Plants, Imported Plants, Rootstocks, Site Visit, Pruning, and Garden Planning/Records.
 *
 * Provides a luminous, harmonic backdrop canvas with multi-spectrum radiant orbs
 * ensuring Liquid Glass surfaces achieve consistent, rich, and visible refraction.
 */
@Composable
fun PremiumGlassAmbientBackdrop(
    accentColor: Color,
    isDark: Boolean,
    isAmoled: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = isDark || isAmoled
    val backgroundBrush = remember(accentColor, isDark, isAmoled) {
        getAppDimBackgroundBrush(accentColor, isDark = isDark, isAmoled = isAmoled)
    }

    // Derive a harmonious secondary ambient color to give multi-chromatic refraction behind glass
    val harmonicColor = remember(accentColor) {
        val r = (accentColor.red * 255f).toInt().coerceIn(0, 255)
        val g = (accentColor.green * 255f).toInt().coerceIn(0, 255)
        val b = (accentColor.blue * 255f).toInt().coerceIn(0, 255)
        val hsv = FloatArray(3)
        android.graphics.Color.RGBToHSV(r, g, b, hsv)
        // Shift hue by +35 degrees for an adjacent harmonic tone
        val harmonicHue = (hsv[0] + 35f) % 360f
        val harmonicTone = android.graphics.Color.HSVToColor(floatArrayOf(harmonicHue, hsv[1].coerceAtLeast(0.65f), 0.95f))
        Color(harmonicTone)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. Top-Right Radiant Primary Ambient Orb
            // Positioned behind header / top controls for brilliant specular refraction
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = if (isAmoled) 0.25f else if (isDarkTheme) 0.28f else 0.22f),
                        accentColor.copy(alpha = if (isAmoled) 0.08f else if (isDarkTheme) 0.10f else 0.06f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.78f, h * 0.16f),
                    radius = w * 0.72f
                )
            )

            // 2. Mid-Left Harmonic Secondary Glow
            // Multi-hue chromatic ambient field providing rich gradient transition
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        harmonicColor.copy(alpha = if (isAmoled) 0.20f else if (isDarkTheme) 0.24f else 0.16f),
                        harmonicColor.copy(alpha = if (isAmoled) 0.06f else if (isDarkTheme) 0.08f else 0.04f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.16f, h * 0.52f),
                    radius = w * 0.75f
                )
            )

            // 3. Center-Right Subtle Ambient Diffuser
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = if (isAmoled) 0.14f else if (isDarkTheme) 0.16f else 0.10f),
                        accentColor.copy(alpha = if (isAmoled) 0.04f else if (isDarkTheme) 0.05f else 0.02f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.65f, h * 0.68f),
                    radius = w * 0.55f
                )
            )

            // 4. Bottom-Right Radiant Ambient Orb
            // Illuminates behind the bottom navigation lens and floating action controls
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = if (isAmoled) 0.24f else if (isDarkTheme) 0.26f else 0.18f),
                        accentColor.copy(alpha = if (isAmoled) 0.07f else if (isDarkTheme) 0.09f else 0.05f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.82f, h * 0.86f),
                    radius = w * 0.62f
                )
            )
        }
    }
}
