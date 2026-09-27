package com.example.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import android.os.PowerManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.example.ui.theme.CustomBackgroundPreference
import com.example.ui.theme.LiquidGlassPreference
import com.example.ui.theme.LocalAppPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Slow ambient motion. Disabled automatically in battery-saver. */
private const val SCENE_ANIMATED = true

/**
 * Premium glass ambient backdrop: multi-colour animated gradient built from ALL the selected palette's colours,
 * or custom background image if set, or solid background if Liquid Glass is disabled.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun PremiumGlassAmbientBackdrop(accentColor: Color, isDark: Boolean, isAmoled: Boolean = false, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val dark = isDark || isAmoled

    val customVersion = if (dark) CustomBackgroundPreference.darkVersion else CustomBackgroundPreference.lightVersion
    if (customVersion > 0L) {
        var customBitmap by remember(customVersion, dark) { mutableStateOf<ImageBitmap?>(null) }
        LaunchedEffect(customVersion, dark) {
            customBitmap = withContext(Dispatchers.IO) {
                val file = if (dark) CustomBackgroundPreference.darkFile(context) else CustomBackgroundPreference.lightFile(context)
                if (file.exists()) BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() else null
            }
        }
        val bmp = customBitmap
        if (bmp != null) {
            Image(
                bitmap = bmp,
                contentDescription = null,
                modifier = modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(modifier = modifier.fillMaxSize().background(if (isAmoled) Color.Black else MaterialTheme.colorScheme.background))
        }
        return
    }

    if (!LiquidGlassPreference.enabled) {
        val solid = if (isAmoled) Color.Black else MaterialTheme.colorScheme.background
        Box(modifier = modifier.fillMaxSize().background(solid))
        return
    }

    val palette = LocalAppPalette.current
    val usePalette = palette.isPredefinedPalette

    val animate = remember {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        SCENE_ANIMATED && pm?.isPowerSaveMode != true
    }
    var phase by remember { mutableFloatStateOf(0f) }
    if (animate) {
        LaunchedEffect(Unit) {
            while (true) {
                delay(100)
                phase = (phase + 0.00008f) % 1f
            }
        }
    }

    val stops: List<Color> = if (usePalette) {
        if (palette.isTwoColor) {
            val p = palette.getPrimary(isDark, isAmoled)
            val t = palette.getTertiary(isDark, isAmoled)
            listOf(p, t, p)
        } else {
            val p = palette.getPrimary(isDark, isAmoled)
            val s = palette.getSecondary(isDark, isAmoled)
            val t = palette.getTertiary(isDark, isAmoled)
            val n = palette.getNeutral(isDark, isAmoled)
            listOf(p, s, t, n, p)
        }
    } else {
        listOf(
            accentColor,
            paletteHueShift(accentColor, 32f),
            paletteHueShift(accentColor, 64f),
            paletteHueShift(accentColor, -32f),
            accentColor
        )
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val angle = (phase * 360f) * (kotlin.math.PI.toFloat() / 180f)
        val dx = kotlin.math.cos(angle) * w * 0.65f
        val dy = kotlin.math.sin(angle) * h * 0.65f
        val cx = w / 2f
        val cy = h / 2f

        drawRect(
            brush = Brush.linearGradient(
                colors = stops,
                start = Offset(cx - dx, cy - dy),
                end = Offset(cx + dx, cy + dy)
            )
        )
        drawRect(color = if (dark) Color.Black.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.45f))
    }
}

private fun paletteHueShift(base: Color, degrees: Float): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(base.toArgb(), hsv)
    hsv[0] = (hsv[0] + degrees + 360f) % 360f
    return Color(android.graphics.Color.HSVToColor(hsv))
}
