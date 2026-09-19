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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.getAppDimBackgroundBrush
import java.util.Random

/**
 * Geometric shape definition for stable scatter rendering.
 */
private data class AmbientDot(
    val relX: Float,
    val relY: Float,
    val radiusDp: Float,
    val colorIndex: Int,
    val alphaMultiplier: Float
)

private data class AmbientRing(
    val relX: Float,
    val relY: Float,
    val radiusDp: Float,
    val strokeWidthDp: Float,
    val colorIndex: Int,
    val alphaMultiplier: Float
)

/**
 * Precomputed, deterministic scattering of hard-edged elements (46 dots + 10 rings).
 * Using a fixed seed ensures layout is 100% stable across recompositions and config changes.
 */
private val STABLE_AMBIENT_DOTS: List<AmbientDot> by lazy {
    val rng = Random(42L)
    val list = ArrayList<AmbientDot>(46)
    for (i in 0 until 46) {
        val relX = 0.05f + rng.nextFloat() * 0.90f
        val relY = 0.04f + rng.nextFloat() * 0.92f
        val radiusDp = 2.5f + rng.nextFloat() * 4.5f // 2.5dp to 7.0dp
        val colorIdx = rng.nextInt(4)
        val alphaMul = 0.55f + rng.nextFloat() * 0.45f
        list.add(AmbientDot(relX, relY, radiusDp, colorIdx, alphaMul))
    }
    list
}

private val STABLE_AMBIENT_RINGS: List<AmbientRing> by lazy {
    val rng = Random(1337L)
    val list = ArrayList<AmbientRing>(10)
    for (i in 0 until 10) {
        val relX = 0.08f + rng.nextFloat() * 0.84f
        val relY = 0.06f + rng.nextFloat() * 0.88f
        val radiusDp = 10f + rng.nextFloat() * 16f // 10dp to 26dp
        val strokeWidthDp = 1.2f + rng.nextFloat() * 1.3f // 1.2dp to 2.5dp
        val colorIdx = rng.nextInt(4)
        val alphaMul = 0.50f + rng.nextFloat() * 0.40f
        list.add(AmbientRing(relX, relY, radiusDp, strokeWidthDp, colorIdx, alphaMul))
    }
    list
}

/**
 * Shared premium ambient background composable for all 6 tabs:
 * Local Plants, Imported Plants, Rootstocks, Site Visit, Pruning, and Garden Planning/Records.
 *
 * Provides a luminous, harmonic backdrop canvas with multi-spectrum radiant orbs
 * AND hard-edged scattering elements (46 dots + 10 rings) so Liquid Glass surfaces
 * achieve crisp, visible lens refraction and optical displacement across the UI.
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

    // 4-color palette derived from the accent color:
    // 0: Accent color (primary)
    // 1: Harmonic color (+35° hue shift)
    // 2: Triadic complementary (+70° hue shift)
    // 3: Soft radiant highlight (warm/tinted white-shifted accent)
    val colorPalette = remember(accentColor) {
        val r = (accentColor.red * 255f).toInt().coerceIn(0, 255)
        val g = (accentColor.green * 255f).toInt().coerceIn(0, 255)
        val b = (accentColor.blue * 255f).toInt().coerceIn(0, 255)
        val hsv = FloatArray(3)
        android.graphics.Color.RGBToHSV(r, g, b, hsv)

        val harmonicHue = (hsv[0] + 35f) % 360f
        val harmonicTone = Color(android.graphics.Color.HSVToColor(floatArrayOf(harmonicHue, hsv[1].coerceAtLeast(0.65f), 0.95f)))

        val triadicHue = (hsv[0] + 70f) % 360f
        val triadicTone = Color(android.graphics.Color.HSVToColor(floatArrayOf(triadicHue, (hsv[1] * 0.85f).coerceAtLeast(0.55f), 0.98f)))

        val highlightTone = Color(
            red = (accentColor.red * 0.5f + 0.5f).coerceIn(0f, 1f),
            green = (accentColor.green * 0.5f + 0.5f).coerceIn(0f, 1f),
            blue = (accentColor.blue * 0.5f + 0.5f).coerceIn(0f, 1f),
            alpha = 1f
        )

        listOf(accentColor, harmonicTone, triadicTone, highlightTone)
    }

    val harmonicColor = colorPalette[1]

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val density = this

            // =========================================================================
            // 1. SOFT RADIANT ORBS (ambient glow fields behind cards and controls)
            // =========================================================================

            // Top-Right Primary Radiant Orb
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

            // Mid-Left Harmonic Secondary Glow
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

            // Center-Right Subtle Ambient Diffuser
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

            // Bottom-Right Radiant Ambient Orb behind navigation & FAB
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

            // Top-Left Soft Secondary Diffuser
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        colorPalette[2].copy(alpha = if (isAmoled) 0.12f else if (isDarkTheme) 0.14f else 0.08f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.12f, h * 0.08f),
                    radius = w * 0.45f
                )
            )

            // Bottom-Left Grounding Tone
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = if (isAmoled) 0.12f else if (isDarkTheme) 0.15f else 0.09f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.22f, h * 0.90f),
                    radius = w * 0.50f
                )
            )

            // =========================================================================
            // 2. HARD-EDGED SCATTER SHAPES (provides sharp contrast for lens refraction)
            // =========================================================================
            val baseShapeAlpha = if (isAmoled) 0.38f else if (isDarkTheme) 0.42f else 0.32f

            // 46 Hard-Edged Filled Dots
            for (dot in STABLE_AMBIENT_DOTS) {
                val color = colorPalette[dot.colorIndex % colorPalette.size]
                val dotAlpha = (baseShapeAlpha * dot.alphaMultiplier).coerceIn(0f, 1f)
                val radiusPx = density.run { dot.radiusDp.dp.toPx() }
                drawCircle(
                    color = color.copy(alpha = dotAlpha),
                    radius = radiusPx,
                    center = Offset(w * dot.relX, h * dot.relY)
                )
            }

            // 10 Hard-Edged Ring Outlines
            for (ring in STABLE_AMBIENT_RINGS) {
                val color = colorPalette[ring.colorIndex % colorPalette.size]
                val ringAlpha = (baseShapeAlpha * ring.alphaMultiplier * 0.9f).coerceIn(0f, 1f)
                val radiusPx = density.run { ring.radiusDp.dp.toPx() }
                val strokePx = density.run { ring.strokeWidthDp.dp.toPx() }
                drawCircle(
                    color = color.copy(alpha = ringAlpha),
                    radius = radiusPx,
                    center = Offset(w * ring.relX, h * ring.relY),
                    style = Stroke(width = strokePx)
                )
            }
        }
    }
}
