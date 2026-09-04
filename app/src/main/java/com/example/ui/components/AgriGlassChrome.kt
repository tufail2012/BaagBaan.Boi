package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalAppPalette
import com.example.ui.theme.getDynamicPaletteBackgroundBrush
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

/**
 * CompositionLocal providing HazeState across all frosted liquid glass surfaces throughout the app.
 */
val LocalAppGlassHazeState = compositionLocalOf<HazeState?> { null }

/**
 * High-performance, dynamic patterned liquid background responsive to the active Color Palette.
 * Draws a dim multi-color palette gradient, flowing organic wave ribbons, ambient color pools,
 * and a delicate micro-dot matrix pattern matching the design reference screenshot.
 */
@Composable
fun DynamicPatternedLiquidBackground(
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    scrollOffset: Float = 0f,
    content: @Composable BoxScope.() -> Unit
) {
    val fallbackHazeState = remember { HazeState() }
    val effectiveHazeState = hazeState ?: LocalAppGlassHazeState.current ?: fallbackHazeState
    val palette = LocalAppPalette.current
    val isDark = isAppInDarkMode()
    val isAmoled = isAppInAmoledMode()

    val bgBrush = remember(palette, isDark, isAmoled) {
        getDynamicPaletteBackgroundBrush(palette, isDark = isDark, isAmoled = isAmoled)
    }

    val bloomColors = remember(palette, isDark, isAmoled) {
        if (palette.isTwoColor) {
            val p = if (isDark || isAmoled) Color(0xFF3F3F46) else Color(0xFFCBD5E1)
            val s = if (isDark || isAmoled) Color(0xFF27272A) else Color(0xFFE2E8F0)
            val t = if (isDark || isAmoled) Color(0xFF52525B) else Color(0xFF94A3B8)
            listOf(p, s, t)
        } else {
            val baseP = palette.getPrimary(isDark, isAmoled)
            val baseS = palette.getSecondary(isDark, isAmoled)
            val baseT = palette.getTertiary(isDark, isAmoled)

            fun toSoftBloom(c: Color): Color {
                val r = (c.red * 255f).toInt().coerceIn(0, 255)
                val g = (c.green * 255f).toInt().coerceIn(0, 255)
                val b = (c.blue * 255f).toInt().coerceIn(0, 255)
                val hsv = FloatArray(3)
                android.graphics.Color.RGBToHSV(r, g, b, hsv)
                val sat = if (isAmoled) 0.50f else if (isDark) 0.44f else 0.28f
                val value = if (isAmoled) 0.85f else if (isDark) 0.78f else 0.95f
                return Color(android.graphics.Color.HSVToColor(floatArrayOf(hsv[0], sat, value)))
            }
            listOf(toSoftBloom(baseP), toSoftBloom(baseS), toSoftBloom(baseT))
        }
    }

    val primaryBloom = bloomColors[0]
    val secondaryBloom = bloomColors[1]
    val tertiaryBloom = bloomColors[2]

    val primaryAlpha = if (isAmoled) 0.12f else if (isDark) 0.16f else 0.20f
    val bloomAlpha = if (isAmoled) 0.10f else if (isDark) 0.14f else 0.16f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgBrush)
    ) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = effectiveHazeState)
        ) {
            val w = size.width
            val h = size.height
            val yShift = -scrollOffset * 0.25f

            // 1. Organic Radial Blooms derived from the active Palette
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primaryBloom.copy(alpha = primaryAlpha), Color.Transparent),
                    center = Offset(w * 0.85f, h * 0.12f + yShift),
                    radius = w * 0.85f
                ),
                center = Offset(w * 0.85f, h * 0.12f + yShift),
                radius = w * 0.85f
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(secondaryBloom.copy(alpha = bloomAlpha), Color.Transparent),
                    center = Offset(w * 0.10f, h * 0.35f + yShift),
                    radius = w * 0.75f
                ),
                center = Offset(w * 0.10f, h * 0.35f + yShift),
                radius = w * 0.75f
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(tertiaryBloom.copy(alpha = bloomAlpha * 0.90f), Color.Transparent),
                    center = Offset(w * 0.90f, h * 0.65f + yShift),
                    radius = w * 0.75f
                ),
                center = Offset(w * 0.90f, h * 0.65f + yShift),
                radius = w * 0.75f
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primaryBloom.copy(alpha = bloomAlpha * 0.80f), Color.Transparent),
                    center = Offset(w * 0.15f, h * 0.88f + yShift),
                    radius = w * 0.70f
                ),
                center = Offset(w * 0.15f, h * 0.88f + yShift),
                radius = w * 0.70f
            )

            // 2. Flowing subtle organic wave ribbons
            val waveAlpha = if (isAmoled) 0.04f else if (isDark) 0.06f else 0.07f
            val waveStroke = Stroke(width = 1.5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)

            val path1 = Path().apply {
                moveTo(-w * 0.2f, h * 0.22f + yShift)
                cubicTo(
                    w * 0.3f, h * 0.18f + yShift,
                    w * 0.6f, h * 0.30f + yShift,
                    w * 1.2f, h * 0.24f + yShift
                )
            }
            drawPath(path1, color = primaryBloom.copy(alpha = waveAlpha), style = waveStroke)

            val path2 = Path().apply {
                moveTo(-w * 0.1f, h * 0.48f + yShift)
                cubicTo(
                    w * 0.35f, h * 0.56f + yShift,
                    w * 0.70f, h * 0.42f + yShift,
                    w * 1.2f, h * 0.52f + yShift
                )
            }
            drawPath(path2, color = secondaryBloom.copy(alpha = waveAlpha * 0.9f), style = waveStroke)

            val path3 = Path().apply {
                moveTo(-w * 0.2f, h * 0.78f + yShift)
                cubicTo(
                    w * 0.4f, h * 0.72f + yShift,
                    w * 0.65f, h * 0.84f + yShift,
                    w * 1.15f, h * 0.76f + yShift
                )
            }
            drawPath(path3, color = tertiaryBloom.copy(alpha = waveAlpha * 0.85f), style = waveStroke)

            // 3. Stippled delicate micro-dot matrix pattern in lower section (matching screenshot footer)
            val dotSpacing = 16.dp.toPx()
            val dotRadius = 1.2.dp.toPx()
            val dotColor = primaryBloom.copy(alpha = if (isAmoled) 0.05f else if (isDark) 0.07f else 0.08f)
            val startX = w * 0.55f
            val startY = h * 0.82f + yShift
            var curX = startX
            while (curX < w) {
                var curY = startY
                while (curY < h) {
                    drawCircle(color = dotColor, radius = dotRadius, center = Offset(curX, curY))
                    curY += dotSpacing
                }
                curX += dotSpacing
            }
        }

        CompositionLocalProvider(LocalAppGlassHazeState provides effectiveHazeState) {
            content()
        }
    }
}

/**
 * Core Frosted Liquid Glass Surface modifier.
 * Uses real-time backdrop blur, subtle liquid translucency, and a two-tone specular rim border.
 */
@Composable
fun Modifier.frostedLiquidGlassSurface(
    hazeState: HazeState? = LocalAppGlassHazeState.current,
    shape: Shape = RoundedCornerShape(20.dp),
    isDark: Boolean = isAppInDarkMode(),
    isAmoled: Boolean = isAppInAmoledMode(),
    elevation: Dp = 1.dp
): Modifier {
    val effectiveHazeState = hazeState ?: LocalAppGlassHazeState.current

    val hazeStyle = remember(isDark, isAmoled) {
        HazeStyle(
            backgroundColor = Color.Transparent,
            blurRadius = 24.dp,
            tints = listOf(
                HazeTint(
                    color = if (isAmoled) Color.Black.copy(alpha = 0.10f)
                    else if (isDark) Color(0xFF14121B).copy(alpha = 0.12f)
                    else Color.White.copy(alpha = 0.06f)
                )
            ),
            noiseFactor = 0f
        )
    }

    val bgBrush = remember(isDark, isAmoled) {
        val topAlpha = if (isAmoled) 0.08f else if (isDark) 0.12f else 0.14f
        val bottomAlpha = if (isAmoled) 0.02f else if (isDark) 0.04f else 0.05f
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = topAlpha),
                Color.White.copy(alpha = bottomAlpha)
            )
        )
    }

    val rimBrush = remember(isDark, isAmoled) {
        val rimTop = if (isAmoled) 0.35f else if (isDark) 0.45f else 0.55f
        val rimBottom = if (isAmoled) 0.08f else if (isDark) 0.10f else 0.14f
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = rimTop),
                Color.White.copy(alpha = rimBottom)
            )
        )
    }

    return this
        .then(
            if (elevation > 0.dp) {
                Modifier.shadow(
                    elevation = elevation,
                    shape = shape,
                    clip = false,
                    spotColor = Color.Black.copy(alpha = if (isAmoled) 0.40f else if (isDark) 0.25f else 0.08f),
                    ambientColor = Color.Black.copy(alpha = if (isAmoled) 0.25f else if (isDark) 0.15f else 0.04f)
                )
            } else {
                Modifier
            }
        )
        .then(
            if (effectiveHazeState != null) {
                Modifier.hazeEffect(state = effectiveHazeState, style = hazeStyle)
            } else {
                Modifier
            }
        )
        .clip(shape)
        .background(bgBrush, shape = shape)
        .drawWithContent {
            drawContent()
            val w = size.width
            val highlightH = 1.dp.toPx()
            val margin = 12.dp.toPx()
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = if (isAmoled) 0.20f else if (isDark) 0.25f else 0.30f),
                        Color.Transparent
                    ),
                    startX = margin,
                    endX = w - margin
                ),
                topLeft = Offset(margin, 0.5.dp.toPx()),
                size = Size(w - (margin * 2), highlightH)
            )
        }
        .border(BorderStroke(1.dp, rimBrush), shape = shape)
}

/**
 * Frosted Liquid Glass Tile modifier for executive 2-column tiles, sub-cards, and metric pods.
 */
@Composable
fun Modifier.frostedLiquidGlassTile(
    hazeState: HazeState? = LocalAppGlassHazeState.current,
    shape: Shape = RoundedCornerShape(16.dp),
    isDark: Boolean = isAppInDarkMode(),
    isAmoled: Boolean = isAppInAmoledMode()
): Modifier = frostedLiquidGlassSurface(
    hazeState = hazeState,
    shape = shape,
    isDark = isDark,
    isAmoled = isAmoled,
    elevation = 1.dp
)

/**
 * Material 3 Chrome / Container modifier for floating headers, chips, and overlays.
 * Provides a rich glossy frosted liquid glass bevel surface with a crisp specular top rim.
 */
@Composable
fun Modifier.frostedGlassChrome(
    isDark: Boolean = isAppInDarkMode(),
    isAmoled: Boolean = isAppInAmoledMode(),
    accentColor: Color = MaterialTheme.colorScheme.primary,
    shape: Shape = RoundedCornerShape(percent = 50),
    elevation: Dp = 3.dp,
    borderWidth: Dp = 1.dp,
    blurRadius: Dp = 0.dp,
    frostTintAlpha: Float = 0f,
    surfaceOpacity: Float = 1f,
    refractionStrength: Float = 0f,
    chromaticAberration: Float = 0f,
    highlightStrength: Float = 0f,
    innerDepthStrength: Float = 0f
): Modifier {
    val hazeState = LocalAppGlassHazeState.current

    val bgBrush = if (isDark || isAmoled) {
        if (isAmoled) {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF161418).copy(alpha = 0.85f),
                    Color(0xFF0C0B0E).copy(alpha = 0.80f),
                    Color(0xFF000000).copy(alpha = 0.85f)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF2C2833).copy(alpha = 0.85f),
                    Color(0xFF221F28).copy(alpha = 0.80f),
                    Color(0xFF19171E).copy(alpha = 0.85f)
                )
            )
        }
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.70f),
                Color.White.copy(alpha = 0.55f),
                Color.White.copy(alpha = 0.65f)
            )
        )
    }

    val borderBrush = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = if (isAmoled) 0.35f else if (isDark) 0.38f else 0.55f),
            Color.White.copy(alpha = if (isAmoled) 0.12f else if (isDark) 0.15f else 0.25f),
            Color.White.copy(alpha = if (isAmoled) 0.04f else if (isDark) 0.08f else 0.12f)
        )
    )

    return this
        .then(
            if (elevation > 0.dp) {
                Modifier.shadow(
                    elevation = elevation,
                    shape = shape,
                    clip = false,
                    spotColor = Color.Black.copy(alpha = if (isAmoled) 0.50f else if (isDark) 0.35f else 0.12f),
                    ambientColor = Color.Black.copy(alpha = if (isAmoled) 0.30f else if (isDark) 0.20f else 0.06f)
                )
            } else {
                Modifier
            }
        )
        .then(
            if (hazeState != null) {
                val hazeStyle = HazeStyle(
                    backgroundColor = Color.Transparent,
                    blurRadius = 20.dp,
                    tints = listOf(
                        HazeTint(
                            color = if (isAmoled) Color.Black.copy(alpha = 0.10f)
                            else if (isDark) Color(0xFF14121B).copy(alpha = 0.12f)
                            else Color.White.copy(alpha = 0.08f)
                        )
                    ),
                    noiseFactor = 0f
                )
                Modifier.hazeEffect(state = hazeState, style = hazeStyle)
            } else {
                Modifier
            }
        )
        .clip(shape)
        .background(brush = bgBrush, shape = shape)
        .drawWithContent {
            drawContent()
            val w = size.width
            val highlightH = 1.5.dp.toPx()
            val margin = 16.dp.toPx()
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = if (isAmoled) 0.28f else if (isDark) 0.32f else 0.40f),
                        Color.Transparent
                    ),
                    startX = margin,
                    endX = w - margin
                ),
                topLeft = Offset(margin, 1.dp.toPx()),
                size = Size(w - (margin * 2), highlightH)
            )
        }
        .border(width = borderWidth, brush = borderBrush, shape = shape)
}

