package com.example.ui.components

import android.content.Context
import android.os.PowerManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalAppPalette
import com.example.ui.theme.getAppDimBackgroundBrush
import com.example.ui.theme.getDynamicPaletteBackgroundBrush
import kotlinx.coroutines.delay
import java.util.Random
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sin

/** Global strength of every coloured layer (0.7 = calmer, 1.3 = bolder). */
private const val BACKDROP_INTENSITY = 1.0f
/** Very slow drift (10 fps). Automatically disabled in battery-saver mode. */
private const val BACKDROP_ANIMATED = true

private class BackdropTriad(val p: Color, val s: Color, val t: Color)

private class Bokeh(
    val x: Float,
    val y: Float,
    val rDp: Float,
    val colorIdx: Int,
    val ring: Boolean,
    val phaseOffset: Float
)

private val BOKEH: List<Bokeh> by lazy {
    val rnd = Random(20260921L)
    List(11) { i ->
        Bokeh(
            x = 0.06f + rnd.nextFloat() * 0.88f,
            y = 0.05f + rnd.nextFloat() * 0.90f,
            rDp = 12f + rnd.nextFloat() * 34f,
            colorIdx = i % 4,
            ring = i % 3 == 0,
            phaseOffset = rnd.nextFloat()
        )
    }
}

private fun hueShifted(base: Color, dh: Float, ds: Float): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(base.toArgb(), hsv)
    hsv[0] = (hsv[0] + dh + 360f) % 360f
    hsv[1] = (hsv[1] * ds).coerceIn(0f, 1f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

@Composable
fun PremiumGlassAmbientBackdrop(
    accentColor: Color,
    isDark: Boolean,
    isAmoled: Boolean = false,
    modifier: Modifier = Modifier
) {
    val palette = LocalAppPalette.current
    val usePalette = palette.isPredefinedPalette
    val darkish = isDark || isAmoled

    val backgroundBrush = remember(accentColor, palette, usePalette, isDark, isAmoled) {
        if (usePalette) getDynamicPaletteBackgroundBrush(palette, isDark = isDark, isAmoled = isAmoled)
        else getAppDimBackgroundBrush(accentColor, isDark = isDark, isAmoled = isAmoled)
    }

    val triad = remember(palette, usePalette, accentColor, isDark, isAmoled) {
        if (usePalette) {
            val tert = palette.getTertiary(isDark, isAmoled)
            if (palette.isTwoColor) {
                BackdropTriad(
                    tert,
                    if (darkish) Color(0xFFE4E4E7) else Color(0xFF52525B),
                    Color(0xFF71717A)
                )
            } else {
                BackdropTriad(
                    palette.getPrimary(isDark, isAmoled),
                    palette.getSecondary(isDark, isAmoled),
                    tert
                )
            }
        } else {
            BackdropTriad(
                accentColor,
                hueShifted(accentColor, 38f, 0.95f),
                hueShifted(accentColor, -42f, 0.90f)
            )
        }
    }

    val context = LocalContext.current
    val animate = remember {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        BACKDROP_ANIMATED && pm?.isPowerSaveMode != true
    }
    var phase by remember { mutableFloatStateOf(0f) }
    if (animate) {
        LaunchedEffect(Unit) {
            while (true) {
                delay(100)
                phase = (phase + 0.0012f) % 1f
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val big = max(w, h)
            val ph = phase
            val k = BACKDROP_INTENSITY
            val tone = if (darkish) 1f else 0.62f
            val twoPi = (2.0 * PI).toFloat()
            val p = triad.p
            val s = triad.s
            val t = triad.t

            fun wave(offset: Float): Float = sin(twoPi * (ph + offset))

            // 1. AURORA FIELDS
            fun aurora(color: Color, cx: Float, cy: Float, radius: Float, alpha: Float) {
                val al = (alpha * tone * k).coerceIn(0f, 1f)
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(color.copy(alpha = al), color.copy(alpha = al * 0.45f), Color.Transparent),
                        center = Offset(cx, cy),
                        radius = radius
                    )
                )
            }
            aurora(p, w * (0.10f + 0.05f * wave(0.00f)), h * (0.10f + 0.03f * wave(0.25f)), big * 0.85f, 0.42f)
            aurora(s, w * (0.95f + 0.04f * wave(0.33f)), h * (0.36f + 0.05f * wave(0.58f)), big * 0.75f, 0.34f)
            aurora(t, w * (0.05f + 0.05f * wave(0.66f)), h * (0.68f + 0.04f * wave(0.10f)), big * 0.80f, 0.36f)
            aurora(p, w * (0.85f + 0.05f * wave(0.80f)), h * (0.95f + 0.02f * wave(0.45f)), big * 0.70f, 0.30f)

            // 2. SILK RIBBONS (glow + mid band + bright core + fine echo lines)
            val ribbonBrush = Brush.linearGradient(
                colors = listOf(p, s, t, p),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )
            val ribbons = listOf(
                floatArrayOf(0.18f, 0.62f, 0.02f, 0.95f),
                floatArrayOf(0.55f, 0.20f, 0.85f, 0.05f),
                floatArrayOf(0.82f, 0.88f, 0.45f, 1.05f)
            )
            ribbons.forEachIndexed { i, r ->
                val shift = w * 0.05f * wave(i / 3f)
                val path = Path().apply {
                    moveTo(-0.15f * w, h * r[0])
                    cubicTo(0.28f * w + shift, h * r[2], 0.72f * w - shift, h * r[3], 1.15f * w, h * r[1])
                }
                val widthScale = 1f - i * 0.18f
                drawPath(
                    path = path, brush = ribbonBrush, alpha = (0.16f * tone * k).coerceIn(0f, 1f),
                    style = Stroke(width = 96.dp.toPx() * widthScale, cap = StrokeCap.Round)
                )
                drawPath(
                    path = path, brush = ribbonBrush, alpha = (0.22f * tone * k).coerceIn(0f, 1f),
                    style = Stroke(width = 34.dp.toPx() * widthScale, cap = StrokeCap.Round)
                )
                val core = if (darkish) Color.White else p
                drawPath(
                    path = path, color = core, alpha = (0.55f * k).coerceIn(0f, 1f),
                    style = Stroke(width = 2.2f.dp.toPx(), cap = StrokeCap.Round)
                )
                for (n in -3..3) {
                    if (n == 0) continue
                    translate(top = n * 9.dp.toPx()) {
                        drawPath(
                            path = path, brush = ribbonBrush,
                            alpha = ((0.20f - abs(n) * 0.04f) * tone * k).coerceIn(0f, 1f),
                            style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
            }

            // 3. BOKEH ORBS + RINGS
            BOKEH.forEach { b ->
                val cx = w * b.x + 10.dp.toPx() * wave(b.phaseOffset)
                val cy = h * b.y + 14.dp.toPx() * wave(b.phaseOffset + 0.25f)
                val r = b.rDp.dp.toPx()
                val c = when (b.colorIdx) {
                    0 -> p
                    1 -> s
                    2 -> t
                    else -> if (darkish) Color.White else p
                }
                val al = (0.34f * tone * k).coerceIn(0f, 1f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(c.copy(alpha = al), c.copy(alpha = al * 0.35f), Color.Transparent),
                        center = Offset(cx, cy),
                        radius = r * 1.6f
                    ),
                    radius = r * 1.6f,
                    center = Offset(cx, cy)
                )
                if (b.ring) {
                    drawCircle(
                        color = c.copy(alpha = (0.45f * tone * k).coerceIn(0f, 1f)),
                        radius = r,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.4f.dp.toPx())
                    )
                }
            }

            // 4. SHEEN (dark only)
            if (darkish) {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.055f * k), Color.Transparent),
                        start = Offset(w * (0.0f + 0.2f * wave(0.5f)), 0f),
                        end = Offset(w * (0.8f + 0.2f * wave(0.5f)), h * 0.7f)
                    )
                )
            }

            // 5. VIGNETTE (dark only)
            if (darkish) {
                drawRect(
                    brush = Brush.radialGradient(
                        0.0f to Color.Transparent,
                        0.55f to Color.Transparent,
                        1.0f to Color.Black.copy(alpha = 0.38f),
                        center = Offset(w / 2f, h / 2f),
                        radius = big * 0.75f
                    )
                )
            }
        }
    }
}
