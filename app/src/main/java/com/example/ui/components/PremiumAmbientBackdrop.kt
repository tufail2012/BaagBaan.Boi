package com.example.ui.components

import android.content.Context
import android.os.PowerManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.example.ui.theme.LiquidGlassPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Random
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** Slow ambient motion (stars, fireflies, clouds, leaves). Disabled automatically in battery-saver. */
private const val SCENE_ANIMATED = true
/** The heavy static scene (trees, people, grass) is drawn once into a bitmap at this scale. */
private const val STATIC_SCALE = 0.75f
private const val TAU = 6.2831855f

private fun ca(argb: Long, a: Float): Color = Color(argb).copy(alpha = a)

private fun smooth(a: Float, b: Float, x: Float): Float {
    val t = ((x - a) / (b - a)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

/**
 * "Golden Orchard" background: an apple orchard scene (sun, hills, apple tree with red & green apples,
 * a picker on the ground, a climber in the tree, ladder, basket, fresh + dry grass, flowers, fireflies).
 * Public signature is unchanged so every screen picks it up automatically.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun PremiumGlassAmbientBackdrop(accentColor: Color, isDark: Boolean, isAmoled: Boolean = false, modifier: Modifier = Modifier) {
    if (!LiquidGlassPreference.enabled) {
        val solid = if (isAmoled) Color.Black else MaterialTheme.colorScheme.background
        Box(modifier = modifier.fillMaxSize().background(solid))
        return
    }
    val dark = isDark || isAmoled
    val context = LocalContext.current
    val animate = remember {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        SCENE_ANIMATED && pm?.isPowerSaveMode != true
    }
    var phase by remember { mutableFloatStateOf(0f) }
    if (animate) {
        LaunchedEffect(Unit) {
            while (true) {
                delay(100)
                phase = (phase + 0.0016f) % 1f
            }
        }
    }

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val front by produceState<ImageBitmap?>(SceneCache.peek(canvasSize, dark), canvasSize, dark) {
        if (canvasSize.width > 0 && canvasSize.height > 0) {
            value = SceneCache.getOrRender(canvasSize, dark)
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { canvasSize = it }
    ) {
        val ph = phase
        drawSceneBack(dark, ph)
        val bmp = front
        if (bmp != null) {
            drawImage(
                image = bmp,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(bmp.width, bmp.height),
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                filterQuality = FilterQuality.Medium
            )
        } else {
            // First frame only: plain meadow so the sky never shows through before the scene is ready.
            drawRect(
                color = if (dark) Color(0xFF2F7A38) else Color(0xFF5DBB4A),
                topLeft = Offset(0f, size.height * 0.64f),
                size = Size(size.width, size.height * 0.36f)
            )
        }
        drawSceneAtmosphere(dark, ph)
    }
}

private object SceneCache {
    private var key: String? = null
    private var bmp: ImageBitmap? = null
    private fun keyOf(size: IntSize, dark: Boolean) = "${size.width}x${size.height}_$dark"

    fun peek(size: IntSize, dark: Boolean): ImageBitmap? = if (key == keyOf(size, dark)) bmp else null

    suspend fun getOrRender(size: IntSize, dark: Boolean): ImageBitmap {
        peek(size, dark)?.let { return it }
        val rendered = withContext(Dispatchers.Default) { renderFront(size, dark) }
        key = keyOf(size, dark)
        bmp = rendered
        return rendered
    }
}

private fun renderFront(size: IntSize, dark: Boolean): ImageBitmap {
    val bw = max(1, (size.width * STATIC_SCALE).roundToInt())
    val bh = max(1, (size.height * STATIC_SCALE).roundToInt())
    val image = ImageBitmap(bw, bh)
    val canvas = androidx.compose.ui.graphics.Canvas(image)
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, Size(bw.toFloat(), bh.toFloat())) {
        drawSceneFront(dark)
    }
    return image
}

// ------------------------------------------------------------------------------------------------
// LAYER 1 (every frame, cheap): sky, sun, clouds, rays, birds, stars
// ------------------------------------------------------------------------------------------------

private fun DrawScope.glow(cx: Float, cy: Float, r: Float, argb: Long, a: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            0f to ca(argb, a),
            0.45f to ca(argb, a * 0.4f),
            1f to ca(argb, 0f),
            center = Offset(cx, cy),
            radius = r
        ),
        radius = r,
        center = Offset(cx, cy)
    )
}

private fun DrawScope.drawSceneBack(dark: Boolean, ph: Float) {
    val w = size.width
    val h = size.height
    val u = w / 1000f
    fun wave(o: Float): Float = sin(TAU * (ph + o))

    val sunC: Long
    val sunX: Float
    val sunY: Float
    val sky: Array<Pair<Float, Color>>
    if (dark) {
        sky = arrayOf(
            0.00f to Color(0xFF101B33), 0.22f to Color(0xFF27386A), 0.38f to Color(0xFF6A4C93),
            0.50f to Color(0xFFE0687A), 0.58f to Color(0xFFF59E5B), 0.64f to Color(0xFFFFD48A)
        )
        sunC = 0xFFFFC978; sunX = 0.74f; sunY = 0.535f
    } else {
        sky = arrayOf(
            0.00f to Color(0xFF4FA8E8), 0.30f to Color(0xFF8FD2F4), 0.52f to Color(0xFFDDF2FF),
            0.64f to Color(0xFFFFF1C4)
        )
        sunC = 0xFFFFF3B0; sunX = 0.78f; sunY = 0.20f
    }
    drawRect(brush = Brush.verticalGradient(*sky, startY = 0f, endY = h * 0.66f))
    glow(w * sunX, h * sunY, w * 0.75f, sunC, if (dark) 0.55f else 0.45f)
    glow(w * sunX, h * sunY, w * 0.30f, 0xFFFFFFFF, 0.35f)
    drawCircle(color = Color(0xFFFFF2C2), radius = 62f * u, center = Offset(w * sunX, h * sunY))

    // clouds
    val cl: Long = if (dark) 0xFFFFC4A0 else 0xFFFFFFFF
    val clouds = arrayOf(
        floatArrayOf(0.22f, 0.17f, 210f, 26f, 0.30f), floatArrayOf(0.34f, 0.19f, 150f, 20f, 0.22f),
        floatArrayOf(0.70f, 0.29f, 240f, 28f, 0.28f), floatArrayOf(0.15f, 0.36f, 180f, 22f, 0.25f),
        floatArrayOf(0.62f, 0.42f, 200f, 24f, 0.22f)
    )
    for (c in clouds) {
        val cx = w * c[0] + 14f * u * wave(c[0])
        val cy = h * c[1]
        drawOval(ca(cl, c[4]), Offset(cx - c[2] * u, cy - c[3] * u), Size(2f * c[2] * u, 2f * c[3] * u))
        val cx2 = cx + 60f * u
        val cy2 = cy - 14f * u
        val rx2 = c[2] * 0.55f * u
        val ry2 = c[3] * 0.9f * u
        drawOval(ca(cl, c[4]), Offset(cx2 - rx2, cy2 - ry2), Size(2f * rx2, 2f * ry2))
    }

    // sun rays
    for (i in 0 until 6) {
        val ang = ((112 + i * 11) * 0.017453292f)
        val sx = w * sunX
        val sy = h * sunY
        val len = h * 0.9f
        val p = Path().apply {
            moveTo(sx, sy)
            lineTo(sx + cos(ang - 0.045f) * len, sy + sin(ang - 0.045f) * len)
            lineTo(sx + cos(ang + 0.045f) * len, sy + sin(ang + 0.045f) * len)
            close()
        }
        drawPath(p, ca(0xFFFFE9A8, (if (dark) 0.10f else 0.14f) * (0.8f + 0.2f * wave(i / 6f))))
    }

    // birds
    val birds = arrayOf(floatArrayOf(0.28f, 0.27f, 1.0f), floatArrayOf(0.36f, 0.25f, 0.8f), floatArrayOf(0.44f, 0.285f, 0.7f))
    for (b in birds) {
        val x = w * b[0]
        val y = h * b[1] + 4f * u * wave(b[0])
        val sc = b[2]
        val p = Path().apply {
            moveTo(x - 16f * u * sc, y)
            quadraticBezierTo(x - 8f * u * sc, y - 12f * u * sc, x, y)
            quadraticBezierTo(x + 8f * u * sc, y - 12f * u * sc, x + 16f * u * sc, y)
        }
        drawPath(p, if (dark) Color(0xFF2B2440) else Color(0xFF35506B), style = Stroke(2.4f * u, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }

    // stars (dusk only)
    if (dark) {
        val sr = Random(9001L)
        for (i in 0 until 48) {
            val sx = w * sr.nextFloat()
            val sy = h * (0.02f + 0.30f * sr.nextFloat())
            val rr = (1.1f + 1.6f * sr.nextFloat()) * u
            val tw = 0.55f + 0.45f * wave(sr.nextFloat())
            glow(sx, sy, rr * 5f, 0xFFBFD4FF, 0.30f * tw)
            drawCircle(ca(0xFFFFFFFF, 0.55f + 0.4f * tw), rr, Offset(sx, sy))
        }
    }
}

// ------------------------------------------------------------------------------------------------
// LAYER 3 (every frame, cheap): fireflies, falling leaves, vignette
// ------------------------------------------------------------------------------------------------

private fun DrawScope.drawSceneAtmosphere(dark: Boolean, ph: Float) {
    val w = size.width
    val h = size.height
    val u = w / 1000f
    fun wave(o: Float): Float = sin(TAU * (ph + o))

    val fr2 = Random(4242L)
    for (k in 0 until 30) {
        val xf = 0.04f + 0.92f * fr2.nextFloat()
        val yf = 0.22f + 0.72f * fr2.nextFloat()
        val r = (5 + fr2.nextInt(7)) * u
        val o = fr2.nextFloat()
        val x = w * xf + 12f * u * wave(o)
        val y = h * yf + 16f * u * wave(o + 0.3f)
        glow(x, y, r * 4.2f, 0xFFFFE27A, if (dark) 0.55f else 0.35f)
        drawCircle(ca(0xFFFFF6C8, 0.95f), r * 0.55f, Offset(x, y))
    }

    val leafColors = longArrayOf(0xFF6CBF4A, 0xFFB8D94A, 0xFFE0A030)
    val lr = Random(606L)
    for (k in 0 until 7) {
        val xf = 0.05f + 0.6f * lr.nextFloat()
        val yf = 0.36f + 0.5f * lr.nextFloat()
        val ang = lr.nextFloat() * 360f
        val sc = (16 + lr.nextInt(10)) * u
        val cx = w * xf
        val cy = h * yf + 24f * u * wave(xf * 2f)
        rotate(degrees = ang + 25f * wave(xf), pivot = Offset(cx, cy)) {
            val lp = Path().apply {
                moveTo(cx - sc, cy)
                quadraticBezierTo(cx, cy - sc * 0.9f, cx + sc, cy)
                quadraticBezierTo(cx, cy + sc * 0.9f, cx - sc, cy)
            }
            drawPath(lp, Color(leafColors[k % 3]))
            drawLine(ca(0xFF2F5F2A, 0.7f), Offset(cx - sc, cy), Offset(cx + sc, cy), 1.4f * u, StrokeCap.Round)
        }
    }

    if (dark) {
        drawRect(
            brush = Brush.radialGradient(
                0.0f to ca(0xFF000000, 0.14f),
                0.55f to ca(0xFF000000, 0.14f),
                1.0f to ca(0xFF000000, 0.46f),
                center = Offset(w / 2f, h / 2f),
                radius = max(w, h) * 0.78f
            )
        )
    }
}

// ------------------------------------------------------------------------------------------------
// LAYER 2 (rendered ONCE into a bitmap): hills, orchard, meadow, tree, people, grass
// ------------------------------------------------------------------------------------------------

private fun DrawScope.drawSceneFront(dark: Boolean) {
    val w = size.width
    val h = size.height
    val u = w / 1000f
    val ph = 0f
    fun xAt(f: Float) = f * w
    fun yAt(f: Float) = f * h
    fun wave(o: Float): Float = sin(TAU * (ph + o))
    val roundStroke = { wd: Float -> Stroke(width = wd, cap = StrokeCap.Round, join = StrokeJoin.Round) }

    fun line(x0: Float, y0: Float, x1: Float, y1: Float, wd: Float, c: Color) =
        drawLine(c, Offset(x0, y0), Offset(x1, y1), wd, StrokeCap.Round)
    fun circle(cx: Float, cy: Float, r: Float, c: Color) = drawCircle(c, r, Offset(cx, cy))
    fun oval(cx: Float, cy: Float, rx: Float, ry: Float, c: Color) =
        drawOval(c, Offset(cx - rx, cy - ry), Size(2f * rx, 2f * ry))

    fun shaded(px: Float, py: Float, r: Float, light: Long, base: Long, dk: Long) {
        drawCircle(
            brush = Brush.radialGradient(
                0.0f to Color(light), 0.55f to Color(base), 1.0f to Color(dk),
                center = Offset(px - r * 0.30f, py - r * 0.36f), radius = r * 1.40f
            ),
            radius = r, center = Offset(px, py)
        )
    }

    fun apple(px: Float, py: Float, r: Float, colr: Long) {
        circle(px + 2f * u, py + 3f * u, r, ca(0xFF000000, 0.20f))
        circle(px, py, r, Color(colr))
        circle(px + r * 0.30f, py + r * 0.35f, r * 0.70f, ca(0xFF000000, 0.10f))
        oval(px - r * 0.32f, py - r * 0.36f, r * 0.27f, r * 0.19f, ca(0xFFFFFFFF, 0.60f))
        line(px, py - r * 0.85f, px + 2f * u, py - r * 1.40f, max(1.6f, r * 0.13f), Color(0xFF4A2E1A))
        val lf = Path().apply {
            moveTo(px + 2f * u, py - r * 1.25f)
            quadraticBezierTo(px + r * 1.05f, py - r * 1.85f, px + r * 1.5f, py - r * 1.15f)
            quadraticBezierTo(px + r * 0.8f, py - r * 1.05f, px + 2f * u, py - r * 1.25f)
        }
        drawPath(lf, Color(0xFF3F9B3E))
    }

    val reds = longArrayOf(0xFFD7263D, 0xFFB5122B, 0xFFE0453A, 0xFFC81E3A)
    val limes = longArrayOf(0xFF9BD03C, 0xFFB7DE55, 0xFF82BD34)

    // ---------------- HILLS
    fun hill(base: Float, amp: Float, f1: Float, f2: Float, p0: Float, top: Long, bot: Long, yBottom: Float) {
        val p = Path()
        p.moveTo(0f, h)
        val n = 24
        for (i in 0..n) {
            val x = w * i / n
            val y = yAt(base) + amp * u * (sin(f1 * i / n * TAU + p0) * 0.6f + sin(f2 * i / n * TAU + p0 * 1.7f) * 0.4f)
            p.lineTo(x, y)
        }
        p.lineTo(w, h)
        p.close()
        drawPath(p, Brush.verticalGradient(0f to Color(top), 1f to Color(bot), startY = yAt(base) - amp * u, endY = yAt(yBottom)))
    }
    if (dark) {
        hill(0.560f, 46f, 1.2f, 2.6f, 0.5f, 0xFF5B4A86, 0xFF6B4F80, 0.66f)
        hill(0.590f, 34f, 1.7f, 3.1f, 1.9f, 0xFF3D4F77, 0xFF3A5A66, 0.68f)
    } else {
        hill(0.560f, 46f, 1.2f, 2.6f, 0.5f, 0xFF9DC3E2, 0xFFB9D6EA, 0.66f)
        hill(0.590f, 34f, 1.7f, 3.1f, 1.9f, 0xFF86BFA0, 0xFF9AD0A6, 0.68f)
    }

    // distant orchard
    val rr = Random(7L)
    for (i in 0 until 16) {
        val tx = 0.03f + i * 0.062f + rr.nextFloat() * 0.03f
        val ty = 0.60f + 0.006f * sin(i * 1.3f)
        val r = (20 + rr.nextInt(12)) * u
        val tcol: Long = if (dark) 0xFF244B3A else 0xFF4FA36B
        line(xAt(tx), yAt(ty), xAt(tx), yAt(ty) - r * 0.9f, 4f * u, Color(if (dark) 0xFF3B2A22 else 0xFF6B4A32))
        circle(xAt(tx), yAt(ty) - r * 1.35f, r, Color(tcol))
        circle(xAt(tx) - r * 0.35f, yAt(ty) - r * 1.55f, r * 0.62f, Color(if (dark) 0xFF33654B else 0xFF6CBF7F))
        for (k in 0 until 4) {
            val axx = xAt(tx) + (rr.nextFloat() - 0.5f) * r * 1.5f
            val ayy = yAt(ty) - r * 1.35f + (rr.nextFloat() - 0.5f) * r * 1.3f
            circle(axx, ayy, 3.2f * u, Color(if (rr.nextInt(3) > 0) 0xFFE0454F else 0xFFB9E05A))
        }
    }

    // ---------------- MEADOW
    val mead = if (dark) arrayOf(0f to Color(0xFF6FA245), 0.25f to Color(0xFF3F8A3C), 1f to Color(0xFF1D5A2C))
    else arrayOf(0f to Color(0xFF9AD65A), 0.25f to Color(0xFF5DBB4A), 1f to Color(0xFF2F8C3A))
    val mp = Path().apply {
        moveTo(0f, yAt(0.640f))
        cubicTo(xAt(0.25f), yAt(0.615f), xAt(0.55f), yAt(0.655f), xAt(1.0f), yAt(0.625f))
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(mp, Brush.verticalGradient(*mead, startY = yAt(0.62f), endY = h))

    val dry1: Long = if (dark) 0xFFB79A47 else 0xFFCDB35A
    val dry2: Long = if (dark) 0xFF8C7133 else 0xFFA98A3A
    val patches = arrayOf(
        floatArrayOf(0.82f, 0.80f, 300f, 90f, 1f, 0.85f), floatArrayOf(0.70f, 0.90f, 380f, 120f, 2f, 0.75f),
        floatArrayOf(0.92f, 0.95f, 260f, 100f, 1f, 0.85f), floatArrayOf(0.10f, 0.93f, 240f, 80f, 1f, 0.70f),
        floatArrayOf(0.55f, 0.985f, 320f, 70f, 2f, 0.80f)
    )
    for (pt in patches) {
        oval(xAt(pt[0]), yAt(pt[1]), pt[2] * u, pt[3] * u, ca(if (pt[4] == 1f) dry1 else dry2, pt[5]))
    }
    oval(xAt(0.35f), yAt(0.70f), 520f * u, 70f * u, ca(0xFFFFF1A8, if (dark) 0.10f else 0.18f))

    // ---------------- RIGHT SMALL TREE
    run {
        val bx = 0.90f
        val byy = 0.735f
        val sc = 1.0f
        val bark = Color(0xFF5B3A22)
        line(xAt(bx), yAt(byy), xAt(bx) - 6f * u * sc, yAt(byy) - 190f * u * sc, 26f * u * sc, bark)
        line(xAt(bx) - 6f * u * sc, yAt(byy) - 150f * u * sc, xAt(bx) - 70f * u * sc, yAt(byy) - 230f * u * sc, 12f * u * sc, bark)
        val rnd = Random(31L)
        val gsetDark = arrayOf(longArrayOf(0xFF2E7A45, 0xFF1F5A34, 0xFF123A22), longArrayOf(0xFF4AA55A, 0xFF2E8443, 0xFF1C5A30), longArrayOf(0xFF86D05B, 0xFF58B04A, 0xFF2F7F3F))
        val gsetDay = arrayOf(longArrayOf(0xFF3C8B47, 0xFF276B36, 0xFF17482A), longArrayOf(0xFF6FCB5E, 0xFF3F9A48, 0xFF24703A), longArrayOf(0xFFB0EA70, 0xFF74CE56, 0xFF3F9A48))
        for (layer in 0 until 3) {
            for (k in 0 until 14) {
                val ang = rnd.nextFloat() * TAU
                val d = rnd.nextFloat() * 95f * u * sc
                val cx = xAt(bx) - 20f * u * sc + cos(ang) * d * 1.25f
                val cy = yAt(byy) - 290f * u * sc + sin(ang) * d * 0.85f - layer * 6f * u
                val g = if (dark) gsetDark[layer] else gsetDay[layer]
                shaded(cx, cy, (44 + rnd.nextInt(24)) * u * sc, g[0], g[1], g[2])
            }
        }
        for (k in 0 until 16) {
            val ang = rnd.nextFloat() * TAU
            val d = rnd.nextFloat() * 110f * u * sc
            val axx = xAt(bx) - 20f * u * sc + cos(ang) * d * 1.2f
            val ayy = yAt(byy) - 290f * u * sc + sin(ang) * d * 0.8f
            apple(axx, ayy, 11f * u * sc, if (rnd.nextInt(3) > 0) 0xFFD92B3A else 0xFF9ED43E)
        }
    }

    // ---------------- GRASS BLADES (back pass) + FLOWERS
    val fresh = if (dark) longArrayOf(0xFF4FA83F, 0xFF2F8A3B, 0xFF69C04B, 0xFF1F6F32) else longArrayOf(0xFF5FBF4A, 0xFF3FA045, 0xFF7ED957, 0xFF2E8B3F)
    val dryc = if (dark) longArrayOf(0xFFC9AE52, 0xFFA98C3E, 0xFF7C6430) else longArrayOf(0xFFD9BE5E, 0xFFB79B45, 0xFF8F7434)
    val gr = Random(555L)
    val bladeCount = 520
    val bXf = FloatArray(bladeCount); val bYf = FloatArray(bladeCount); val bLn = FloatArray(bladeCount)
    val bWd = FloatArray(bladeCount); val bBend = FloatArray(bladeCount); val bCol = LongArray(bladeCount)
    for (k in 0 until bladeCount) {
        val yf = 0.635f + 0.365f * sqrt(gr.nextFloat())
        val xf = gr.nextFloat()
        val t = (yf - 0.635f) / 0.365f
        val ln = (12f + 46f * t) * u
        val wd = (1.6f + 2.6f * t) * u
        val bend = (gr.nextFloat() - 0.5f) * ln * 0.9f
        val pd = 0.10f + 0.55f * smooth(0.55f, 0.9f, xf) * smooth(0.72f, 0.95f, yf)
        val isDry = gr.nextFloat() < pd
        val colr = if (isDry) dryc[gr.nextInt(3)] else fresh[gr.nextInt(4)]
        bXf[k] = xf; bYf[k] = yf; bLn[k] = ln; bWd[k] = wd; bBend[k] = bend; bCol[k] = colr
    }
    fun blade(k: Int) {
        val x = xAt(bXf[k])
        val y = yAt(bYf[k])
        val ln = bLn[k]
        val bend = bBend[k]
        val sw = 3f * u * wave(bXf[k] * 3.1f) * (ln / (58f * u))
        val p = Path().apply {
            moveTo(x, y)
            quadraticBezierTo(x + bend * 0.3f + sw * 0.4f, y - ln * 0.6f, x + bend + sw, y - ln)
        }
        drawPath(p, Color(bCol[k]), style = roundStroke(bWd[k]))
    }
    for (k in 0 until bladeCount) if (bYf[k] < 0.86f) blade(k)

    val fr = Random(808L)
    for (k in 0 until 38) {
        val xf = fr.nextFloat()
        val yf = 0.66f + 0.33f * fr.nextFloat()
        val t = (yf - 0.635f) / 0.365f
        val r = (3f + 4.5f * t) * u
        val fc = longArrayOf(0xFFFFFFFF, 0xFFFFD54A, 0xFFFF9EBB)[fr.nextInt(3)]
        val x = xAt(xf)
        val y = yAt(yf)
        line(x, y, x, y - (14f + 22f * t) * u, 1.6f * u, Color(0xFF2F8C3A))
        circle(x, y - (14f + 22f * t) * u, r, Color(fc))
        circle(x, y - (14f + 22f * t) * u, r * 0.38f, Color(0xFFF39C12))
    }

    // ---------------- MAIN TREE
    val tx = 0.30f
    val ty = 0.805f
    val cxm = 0.30f
    val cym = 0.42f
    val forky = 0.565f
    oval(xAt(tx) + 40f * u, yAt(ty) + 8f * u, 360f * u, 40f * u, ca(0xFF0B2A12, 0.34f))
    val trunk = Path().apply {
        moveTo(xAt(tx) - 84f * u, yAt(ty))
        cubicTo(xAt(tx) - 46f * u, yAt(ty) - 170f * u, xAt(tx) - 50f * u, yAt(ty) - 330f * u, xAt(tx) - 38f * u, yAt(forky))
        lineTo(xAt(tx) + 44f * u, yAt(forky))
        cubicTo(xAt(tx) + 54f * u, yAt(ty) - 330f * u, xAt(tx) + 58f * u, yAt(ty) - 170f * u, xAt(tx) + 98f * u, yAt(ty))
        close()
    }
    drawPath(
        trunk,
        Brush.horizontalGradient(
            0f to Color(0xFF3F2716), 0.42f to Color(0xFF7B532F), 1f to Color(0xFF54361F),
            startX = xAt(tx) - 84f * u, endX = xAt(tx) + 98f * u
        )
    )
    val barkLines = arrayOf(floatArrayOf(-20f, 40f, 200f), floatArrayOf(10f, 60f, 260f), floatArrayOf(34f, 30f, 180f), floatArrayOf(-34f, 120f, 300f))
    for (bl in barkLines) {
        val p = Path().apply {
            moveTo(xAt(tx) + bl[0] * u, yAt(ty) - bl[1] * u)
            quadraticBezierTo(xAt(tx) + (bl[0] + 6f) * u, yAt(ty) - (bl[1] + bl[2]) * 0.5f * u, xAt(tx) + (bl[0] - 2f) * u, yAt(ty) - bl[2] * u)
        }
        drawPath(p, ca(0xFF2A190D, 0.45f), style = roundStroke(3f * u))
    }
    for (d in intArrayOf(-1, 1)) {
        val p = Path().apply {
            moveTo(xAt(tx) + 8f * u + d * 44f * u, yAt(ty) - 34f * u)
            quadraticBezierTo(xAt(tx) + d * 84f * u, yAt(ty) - 2f * u, xAt(tx) + d * 128f * u, yAt(ty) + 8f * u)
        }
        drawPath(p, Color(0xFF4B301C), style = roundStroke(22f * u))
    }

    fun branch(x0: Float, y0: Float, x1: Float, y1: Float, x2: Float, y2: Float, wd: Float) {
        val p = Path().apply {
            moveTo(xAt(x0), yAt(y0))
            quadraticBezierTo(xAt(x1), yAt(y1), xAt(x2), yAt(y2))
        }
        drawPath(p, Color(0xFF5B3A22), style = roundStroke(wd * u))
    }
    branch(0.30f, 0.57f, 0.22f, 0.52f, 0.12f, 0.49f, 30f)
    branch(0.31f, 0.57f, 0.42f, 0.53f, 0.54f, 0.50f, 26f)
    branch(0.30f, 0.57f, 0.30f, 0.48f, 0.29f, 0.40f, 24f)

    // canopy tones[layer][variant] = light/base/dark
    val tones: Array<Array<LongArray>> = if (dark) arrayOf(
        arrayOf(longArrayOf(0xFF2C6A3E, 0xFF1D4B2C, 0xFF123522), longArrayOf(0xFF337548, 0xFF215433, 0xFF143A26), longArrayOf(0xFF2C6A3E, 0xFF1D4B2C, 0xFF123522)),
        arrayOf(longArrayOf(0xFF4AA35A, 0xFF2E7F42, 0xFF1C5A30), longArrayOf(0xFF54B064, 0xFF34884A, 0xFF1F6034), longArrayOf(0xFF48A058, 0xFF2C7A40, 0xFF1B5730)),
        arrayOf(longArrayOf(0xFF8FD65F, 0xFF52B04A, 0xFF2F7F3F), longArrayOf(0xFF9BE066, 0xFF5BBA50, 0xFF338844), longArrayOf(0xFF86D05B, 0xFF4CAA48, 0xFF2C7C3C))
    ) else arrayOf(
        arrayOf(longArrayOf(0xFF3C8B47, 0xFF276B36, 0xFF17482A), longArrayOf(0xFF44954F, 0xFF2C723A, 0xFF1A4F2D), longArrayOf(0xFF3C8B47, 0xFF276B36, 0xFF17482A)),
        arrayOf(longArrayOf(0xFF6FCB5E, 0xFF3F9A48, 0xFF24703A), longArrayOf(0xFF7AD667, 0xFF46A44E, 0xFF287A3E), longArrayOf(0xFF69C55A, 0xFF3A9444, 0xFF216A36)),
        arrayOf(longArrayOf(0xFFB6EE72, 0xFF74CE56, 0xFF3F9A48), longArrayOf(0xFFC2F47E, 0xFF80D85C, 0xFF46A44E), longArrayOf(0xFFAEE86C, 0xFF6CC852, 0xFF3A9444))
    )
    val rnd = Random(2026L)
    for (layer in 0 until 3) {
        val n = when (layer) { 0 -> 44; 1 -> 56; else -> 50 }
        val shrink = 1f - layer * 0.14f
        for (k in 0 until n) {
            val ang = rnd.nextFloat() * TAU
            val d = sqrt(rnd.nextFloat()) * shrink
            val px = xAt(cxm) + cos(ang) * d * 440f * u
            val py = yAt(cym) + sin(ang) * d * 262f * u - layer * 16f * u
            val r = (50 + rnd.nextInt(56)) * u * (1f - layer * 0.08f)
            val tset = tones[layer][rnd.nextInt(3)]
            shaded(px, py, r, tset[0], tset[1], tset[2])
        }
    }
    for (k in 0 until 30) {
        val ang = rnd.nextFloat() * TAU
        val d = sqrt(rnd.nextFloat())
        val px = xAt(cxm) + cos(ang) * d * 400f * u + 70f * u
        val py = yAt(cym) + sin(ang) * d * 220f * u - 40f * u
        glow(px, py, (26 + rnd.nextInt(22)) * u, 0xFFFFF0A0, if (dark) 0.20f else 0.26f)
    }

    // front branches
    branch(0.30f, 0.575f, 0.22f, 0.525f, 0.13f, 0.495f, 26f)
    branch(0.31f, 0.575f, 0.41f, 0.545f, 0.50f, 0.53f, 20f)
    branch(0.33f, 0.615f, 0.44f, 0.640f, 0.53f, 0.668f, 20f)

    // apples on the canopy
    val ar = Random(99L)
    for (k in 0 until 64) {
        val ang = ar.nextFloat() * TAU
        val d = sqrt(ar.nextFloat())
        val px = xAt(cxm) + cos(ang) * d * 440f * u
        val py = yAt(cym) + sin(ang) * d * 262f * u + 10f * u
        val isRed = ar.nextInt(100) < 62
        val colr = if (isRed) reds[ar.nextInt(4)] else limes[ar.nextInt(3)]
        val r = (15 + ar.nextInt(8)) * u
        apple(px, py, r, colr)
    }

    // low hanging cluster (the one being picked)
    val lc = Random(77L)
    for (k in 0 until 11) {
        val px = xAt(0.53f) + (lc.nextFloat() - 0.5f) * 200f * u
        val py = yAt(0.686f) + (lc.nextFloat() - 0.5f) * 90f * u
        val r = (44 + lc.nextInt(30)) * u
        val tset = if (k % 2 == 0) tones[1][lc.nextInt(3)] else tones[2][lc.nextInt(3)]
        shaded(px, py, r, tset[0], tset[1], tset[2])
    }
    for (k in 0 until 8) {
        val px = xAt(0.53f) + (lc.nextFloat() - 0.5f) * 200f * u
        val py = yAt(0.686f) + (lc.nextFloat() - 0.2f) * 80f * u
        val r = (17 + lc.nextInt(6)) * u
        val colr = if (k % 3 != 0) reds[lc.nextInt(4)] else limes[lc.nextInt(3)]
        apple(px, py, r, colr)
    }

    // ---------------- LADDER
    val lad = Color(0xFF9A6B3B)
    val ladd = Color(0xFF6E4726)
    line(xAt(0.405f), yAt(0.845f), xAt(0.255f), yAt(0.545f), 9f * u, ladd)
    line(xAt(0.455f), yAt(0.845f), xAt(0.285f), yAt(0.545f), 9f * u, lad)
    for (i in 1 until 9) {
        val f = i / 9f
        val xa = xAt(0.405f) + (xAt(0.255f) - xAt(0.405f)) * f
        val xb = xAt(0.455f) + (xAt(0.285f) - xAt(0.455f)) * f
        val y = yAt(0.845f) + (yAt(0.545f) - yAt(0.845f)) * f
        line(xa, y, xb, y, 7f * u, lad)
    }

    // ---------------- PEOPLE
    fun person(
        x: Float, y: Float, hgt: Float, face: Int, shirt: Long, pants: Long, hat: Long,
        hand: Offset, hand2: Offset?, lean: Float
    ) {
        val sc = hgt / 100f
        val skin = Color(0xFFC98B5B)
        val hipX = x
        val hipY = y - 46f * sc
        val shX = x + lean * sc
        val shY = y - 80f * sc
        line(hipX - 6f * sc, hipY, x - 12f * sc * face, y - 2f * sc, 13f * sc, Color(pants))
        line(hipX + 6f * sc, hipY, x + 12f * sc * face, y - 2f * sc, 13f * sc, Color(pants))
        oval(x - 15f * sc * face, y, 10f * sc, 4.5f * sc, Color(0xFF2A1D14))
        oval(x + 15f * sc * face, y, 10f * sc, 4.5f * sc, Color(0xFF2A1D14))
        line(hipX, hipY, shX, shY, 31f * sc, Color(shirt))
        line(hipX, hipY - 4f * sc, shX, shY + 4f * sc, 12f * sc, ca(0xFFFFFFFF, 0.12f))
        if (hand2 == null) {
            line(shX - 8f * sc * face, shY + 2f * sc, shX - 14f * sc * face, shY + 30f * sc, 10f * sc, Color(shirt))
            circle(shX - 14f * sc * face, shY + 33f * sc, 4.8f * sc, skin)
        } else {
            line(shX - 8f * sc * face, shY + 2f * sc, hand2.x, hand2.y, 10f * sc, Color(shirt))
            circle(hand2.x, hand2.y, 4.8f * sc, skin)
        }
        line(shX + 6f * sc * face, shY + 2f * sc, hand.x, hand.y, 10.5f * sc, Color(shirt))
        circle(hand.x, hand.y, 5.2f * sc, skin)
        val hdX = shX + 2f * sc * face
        val hdY = shY - 12f * sc
        drawRoundRect(skin, Offset(hdX - 4f * sc, shY - 8f * sc), Size(8f * sc, 10f * sc), CornerRadius(3f * sc))
        circle(hdX, hdY, 10.5f * sc, skin)
        oval(hdX, hdY - 6f * sc, 17f * sc, 4.4f * sc, Color(hat))
        drawArc(Color(hat), 180f, 180f, true, Offset(hdX - 10f * sc, hdY - 20f * sc), Size(20f * sc, 18f * sc))
        oval(hdX, hdY - 6.5f * sc, 17f * sc, 1.6f * sc, ca(0xFF000000, 0.22f))
    }

    // tree climber
    val climberFeetY = yAt(0.5164f) - 10f * u
    person(
        xAt(0.197f), climberFeetY, 205f * u, 1, 0xFFE4572E, 0xFF3E4A6B, 0xFFF2C94C,
        hand = Offset(xAt(0.245f), climberFeetY - 170f * u * 0.66f - 20f * u),
        hand2 = Offset(xAt(0.150f), yAt(0.5164f) - 62f * u),
        lean = 8f
    )
    // ground picker
    person(
        xAt(0.615f), yAt(0.848f), 300f * u, -1, 0xFF1F8A8A, 0xFF4A3728, 0xFFE8C26A,
        hand = Offset(xAt(0.548f), yAt(0.703f)), hand2 = null, lean = -6f
    )
    apple(xAt(0.548f), yAt(0.703f) - 12f * u, 15f * u, 0xFFD7263D)

    // basket with apples
    val bkx = xAt(0.475f)
    val bky = yAt(0.856f)
    val basketApples = arrayOf(
        floatArrayOf(-30f, -22f), floatArrayOf(0f, -30f), floatArrayOf(28f, -22f), floatArrayOf(-12f, -12f), floatArrayOf(16f, -12f)
    )
    val basketCols = longArrayOf(0xFFD7263D, 0xFF9BD03C, 0xFFC81E3A, 0xFFB7DE55, 0xFFE0453A)
    for (i in basketApples.indices) {
        apple(bkx + basketApples[i][0] * u, bky + basketApples[i][1] * u, 17f * u, basketCols[i])
    }
    val bp = Path().apply {
        moveTo(bkx - 60f * u, bky - 26f * u)
        quadraticBezierTo(bkx - 52f * u, bky + 34f * u, bkx, bky + 38f * u)
        quadraticBezierTo(bkx + 52f * u, bky + 34f * u, bkx + 60f * u, bky - 26f * u)
        close()
    }
    drawPath(
        bp,
        Brush.horizontalGradient(
            0f to Color(0xFF8A5A2B), 0.5f to Color(0xFFC28A4A), 1f to Color(0xFF7A4B22),
            startX = bkx - 60f * u, endX = bkx + 60f * u
        )
    )
    for (i in -2..2) {
        line(bkx + i * 20f * u, bky - 18f * u, bkx + i * 16f * u, bky + 30f * u, 2.2f * u, ca(0xFF4A2E1A, 0.55f))
    }
    line(bkx - 58f * u, bky - 16f * u, bkx + 58f * u, bky - 16f * u, 3f * u, ca(0xFF4A2E1A, 0.55f))
    line(bkx - 50f * u, bky + 8f * u, bkx + 50f * u, bky + 8f * u, 3f * u, ca(0xFF4A2E1A, 0.55f))
    val handle = Path().apply {
        moveTo(bkx - 60f * u, bky - 26f * u)
        quadraticBezierTo(bkx, bky - 96f * u, bkx + 60f * u, bky - 26f * u)
    }
    drawPath(handle, Color(0xFF6E4726), style = roundStroke(5f * u))

    // falling apple
    val fx = xAt(0.50f)
    val fy = yAt(0.775f)
    line(fx, fy - 70f * u, fx, fy - 14f * u, 7f * u, ca(0xFFFFFFFF, 0.14f))
    apple(fx, fy, 18f * u, 0xFFE0453A)

    // fallen apples
    val fallen = arrayOf(
        floatArrayOf(0.42f, 0.885f, 17f), floatArrayOf(0.24f, 0.88f, 15f), floatArrayOf(0.67f, 0.89f, 16f),
        floatArrayOf(0.15f, 0.915f, 21f), floatArrayOf(0.53f, 0.94f, 23f), floatArrayOf(0.83f, 0.925f, 21f),
        floatArrayOf(0.34f, 0.965f, 25f)
    )
    val fallenCols = longArrayOf(0xFFD7263D, 0xFF9BD03C, 0xFFC81E3A, 0xFFE0453A, 0xFFB7DE55, 0xFFD7263D, 0xFFC81E3A)
    for (i in fallen.indices) {
        apple(xAt(fallen[i][0]), yAt(fallen[i][1]), fallen[i][2] * u, fallenCols[i])
    }

    // ---------------- GRASS (front pass)
    for (k in 0 until bladeCount) if (bYf[k] >= 0.86f) blade(k)
    val fg = Random(1234L)
    for (k in 0 until 110) {
        val xf = fg.nextFloat()
        val x = xAt(xf)
        val y = h + 6f * u
        val ln = (60 + fg.nextInt(120)) * u
        val bend = (fg.nextFloat() - 0.5f) * ln * 0.7f + 10f * u * wave(xf * 2.3f)
        val isDry = fg.nextFloat() < (0.15f + 0.5f * smooth(0.5f, 0.95f, xf))
        val colr = if (isDry) dryc[fg.nextInt(3)] else fresh[fg.nextInt(4)]
        val p = Path().apply {
            moveTo(x, y)
            quadraticBezierTo(x + bend * 0.3f, y - ln * 0.6f, x + bend, y - ln)
        }
        drawPath(p, Color(colr), style = roundStroke((4 + fg.nextInt(4)) * u))
    }
}
