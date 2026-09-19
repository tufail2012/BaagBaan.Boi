package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
 * Comprehensive, premium color palette for ambient micro-details:
 * Red, crimson, coral, orange, amber, yellow, gold, lime, green, teal,
 * cyan, blue, indigo, violet, pink, rose, grey, silver, cream, and soft neutral.
 * All tones are calibrated for soft, elegant light diffusion through Liquid Glass.
 */
private val AMBIENT_PALETTE = listOf(
    Color(0xFFE57373), // Red
    Color(0xFFE53935), // Crimson
    Color(0xFFFF8A65), // Coral
    Color(0xFFFFB74D), // Orange
    Color(0xFFFFCA28), // Amber
    Color(0xFFFFF176), // Yellow
    Color(0xFFFFD54F), // Gold
    Color(0xFFDCE775), // Lime
    Color(0xFF81C784), // Green
    Color(0xFF4DB6AC), // Teal
    Color(0xFF4DD0E1), // Cyan
    Color(0xFF64B5F6), // Blue
    Color(0xFF7986CB), // Indigo
    Color(0xFF9575CD), // Violet
    Color(0xFFF06292), // Pink
    Color(0xFFFF80AB), // Rose
    Color(0xFF90A4AE), // Grey
    Color(0xFFCFD8DC), // Silver
    Color(0xFFFFF8E1), // Cream
    Color(0xFFECEFF1)  // Soft Neutral
)

/**
 * Precomputed, deterministic scattering of hard-edged elements (210 dots + 46 rings).
 * Using a fixed seed ensures layout is 100% stable across recompositions and config changes.
 * Consists of mostly small circles with some medium circles, and zero oversized dominant orbs.
 */
private val STABLE_AMBIENT_DOTS: List<AmbientDot> by lazy {
    val rng = Random(42L)
    val list = ArrayList<AmbientDot>(210)
    for (i in 0 until 210) {
        val relX = 0.02f + rng.nextFloat() * 0.96f
        val relY = 0.02f + rng.nextFloat() * 0.96f
        val isMedium = (i % 4 == 0)
        val radiusDp = if (isMedium) {
            7.0f + rng.nextFloat() * 9.0f // 7dp to 16dp (medium)
        } else {
            2.0f + rng.nextFloat() * 4.8f // 2dp to 6.8dp (small)
        }
        val colorIdx = rng.nextInt(AMBIENT_PALETTE.size)
        val alphaMul = if (isMedium) {
            0.30f + rng.nextFloat() * 0.35f
        } else {
            0.45f + rng.nextFloat() * 0.45f
        }
        list.add(AmbientDot(relX, relY, radiusDp, colorIdx, alphaMul))
    }
    list
}

private val STABLE_AMBIENT_RINGS: List<AmbientRing> by lazy {
    val rng = Random(1337L)
    val list = ArrayList<AmbientRing>(46)
    for (i in 0 until 46) {
        val relX = 0.03f + rng.nextFloat() * 0.94f
        val relY = 0.03f + rng.nextFloat() * 0.94f
        val radiusDp = 8f + rng.nextFloat() * 14f // 8dp to 22dp (medium rings)
        val strokeWidthDp = 0.8f + rng.nextFloat() * 1.0f // 0.8dp to 1.8dp
        val colorIdx = rng.nextInt(AMBIENT_PALETTE.size)
        val alphaMul = 0.30f + rng.nextFloat() * 0.35f
        list.add(AmbientRing(relX, relY, radiusDp, strokeWidthDp, colorIdx, alphaMul))
    }
    list
}

/**
 * Shared premium ambient background composable for all 6 tabs:
 * Local Plants, Imported Plants, Rootstocks, Site Visit, Pruning, and Garden Planning/Records.
 *
 * Provides a rich, dense field of colorful micro-details (dots & rings) behind the glass,
 * allowing Liquid Glass surfaces to produce vibrant, organic light refraction without
 * large blob-like orbs dominating the visual canvas.
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

    val activePalette = remember(accentColor) {
        AMBIENT_PALETTE + listOf(accentColor)
    }

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
            // DENSE SCATTER FIELD (Micro-details sampled by Liquid Glass)
            // =========================================================================
            val baseShapeAlpha = if (isAmoled) 0.35f else if (isDarkTheme) 0.38f else 0.28f

            // 210 Hard-Edged Small & Medium Filled Dots
            for (dot in STABLE_AMBIENT_DOTS) {
                val color = activePalette[dot.colorIndex % activePalette.size]
                val dotAlpha = (baseShapeAlpha * dot.alphaMultiplier).coerceIn(0f, 1f)
                val radiusPx = density.run { dot.radiusDp.dp.toPx() }
                drawCircle(
                    color = color.copy(alpha = dotAlpha),
                    radius = radiusPx,
                    center = Offset(w * dot.relX, h * dot.relY)
                )
            }

            // 46 Fine-Edged Ring Outlines
            for (ring in STABLE_AMBIENT_RINGS) {
                val color = activePalette[ring.colorIndex % activePalette.size]
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
