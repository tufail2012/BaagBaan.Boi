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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalAppPalette
import com.example.ui.theme.getAppDimBackgroundBrush
import com.example.ui.theme.getDynamicPaletteBackgroundBrush
import kotlinx.coroutines.delay
import java.util.Random
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin

private const val BACKDROP_INTENSITY = 1.0f
private const val BACKDROP_ANIMATED = true

private class Apple(val x: Float, val y: Float, val rDp: Float, val ripe: Boolean, val phaseOffset: Float)
private class Blade(val x: Float, val rDp: Float, val dry: Boolean, val phaseOffset: Float)

private val APPLES: List<Apple> by lazy {
    val rnd = Random(20260921L)
    List(16) { i ->
        Apple(
            x = 0.20f + rnd.nextFloat() * 0.42f,
            y = 0.08f + rnd.nextFloat() * 0.34f,
            rDp = 10f + rnd.nextFloat() * 16f,
            ripe = i % 5 != 0,
            phaseOffset = rnd.nextFloat()
        )
    }
}

private val GRASS: List<Blade> by lazy {
    val rnd = Random(20260921L)
    List(22) { i ->
        Blade(
            x = rnd.nextFloat(),
            rDp = 30f + rnd.nextFloat() * 60f,
            dry = i % 3 == 0,
            phaseOffset = rnd.nextFloat()
        )
    }
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

    val skyTop = if (darkish) Color(0xFF16241C) else Color(0xFFBFE3D0)
    val skyBottom = if (darkish) Color(0xFF0E1712) else Color(0xFFEFE6C8)
    val canopyDeep = Color(0xFF1F5C3B)
    val canopyMid = Color(0xFF3F8F52)
    val canopyLight = Color(0xFF7CB342)
    val trunkColor = Color(0xFF6B4226)
    val trunkDark = Color(0xFF3E2A18)
    val appleRed = Color(0xFFE53935)
    val appleRedGlow = Color(0xFFFF6F5E)
    val appleGreen = Color(0xFFA8D24C)
    val grassFresh = Color(0xFF4CAF50)
    val grassFreshLight = Color(0xFF8BC34A)
    val grassDry = Color(0xFFC8A24A)
    val grassDryLight = Color(0xFFE3C77E)
    val personWarm = Color(0xFFE8B382)

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
                phase = (phase + 0.0010f) % 1f
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
            val ph = phase
            val k = BACKDROP_INTENSITY
            val tone = if (darkish) 1f else 0.78f
            val twoPi = (2.0 * PI).toFloat()
            fun wave(offset: Float): Float = sin(twoPi * (ph + offset))

            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(skyTop.copy(alpha = 0.55f * k), skyBottom.copy(alpha = 0.35f * k), Color.Transparent)
                )
            )

            val trunkX = w * 0.22f
            val trunkPath = Path().apply {
                moveTo(trunkX - 26.dp.toPx(), h * 1.02f)
                cubicTo(
                    trunkX - 34.dp.toPx(), h * 0.62f,
                    trunkX - 10.dp.toPx(), h * 0.46f,
                    trunkX, h * 0.30f
                )
                cubicTo(
                    trunkX + 10.dp.toPx(), h * 0.46f,
                    trunkX + 34.dp.toPx(), h * 0.62f,
                    trunkX + 26.dp.toPx(), h * 1.02f
                )
                close()
            }
            drawPath(
                path = trunkPath,
                brush = Brush.verticalGradient(listOf(trunkColor.copy(alpha = 0.5f * k), trunkDark.copy(alpha = 0.35f * k)))
            )

            fun canopyBlob(color: Color, cx: Float, cy: Float, r: Float, alpha: Float) {
                val al = (alpha * tone * k).coerceIn(0f, 1f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(color.copy(alpha = al), color.copy(alpha = al * 0.4f), Color.Transparent),
                        center = Offset(cx, cy), radius = r
                    ),
                    radius = r, center = Offset(cx, cy)
                )
            }
            val canopyCx = trunkX + w * 0.06f * wave(0.1f)
            val canopyCy = h * 0.22f + h * 0.02f * wave(0.4f)
            canopyBlob(canopyDeep, canopyCx - w * 0.10f, canopyCy + h * 0.03f, w * 0.30f, 0.48f)
            canopyBlob(canopyMid, canopyCx + w * 0.06f, canopyCy - h * 0.02f, w * 0.34f, 0.50f)
            canopyBlob(canopyLight, canopyCx + w * 0.16f, canopyCy + h * 0.05f, w * 0.22f, 0.42f)
            canopyBlob(canopyMid, canopyCx - w * 0.02f, canopyCy - h * 0.06f, w * 0.24f, 0.40f)

            APPLES.forEach { a ->
                val cx = w * a.x + 6.dp.toPx() * wave(a.phaseOffset)
                val cy = h * a.y + 5.dp.toPx() * wave(a.phaseOffset + 0.3f)
                val r = a.rDp.dp.toPx()
                val core = if (a.ripe) appleRed else appleGreen
                val glow = if (a.ripe) appleRedGlow else appleGreen
                val al = (0.55f * tone * k).coerceIn(0f, 1f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glow.copy(alpha = al), core.copy(alpha = al * 0.5f), Color.Transparent),
                        center = Offset(cx, cy), radius = r * 1.8f
                    ),
                    radius = r * 1.8f, center = Offset(cx, cy)
                )
                drawCircle(color = core.copy(alpha = (0.6f * k).coerceIn(0f, 1f)), radius = r * 0.55f, center = Offset(cx, cy))
            }

            val personX = trunkX + w * 0.13f
            val personY = h * 0.42f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(personWarm.copy(alpha = 0.30f * tone * k), Color.Transparent),
                    center = Offset(personX, personY), radius = w * 0.10f
                ),
                radius = w * 0.10f, center = Offset(personX, personY)
            )

            GRASS.forEach { b ->
                val cx = w * b.x
                val baseY = h * (1.02f + 0.01f * wave(b.phaseOffset))
                val c1 = if (b.dry) grassDry else grassFresh
                val c2 = if (b.dry) grassDryLight else grassFreshLight
                val r = b.rDp.dp.toPx()
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(c1.copy(alpha = 0.40f * tone * k), c2.copy(alpha = 0.18f * tone * k), Color.Transparent),
                        center = Offset(cx, baseY), radius = r
                    ),
                    radius = r, center = Offset(cx, baseY)
                )
            }

            if (darkish) {
                drawRect(
                    brush = Brush.radialGradient(
                        0.0f to Color.Transparent,
                        0.55f to Color.Transparent,
                        1.0f to Color.Black.copy(alpha = 0.35f),
                        center = Offset(w / 2f, h / 2f),
                        radius = max(w, h) * 0.75f
                    )
                )
            }
        }
    }
}
