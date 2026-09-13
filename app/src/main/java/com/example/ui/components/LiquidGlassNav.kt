package com.example.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import com.kyant.backdrop.shadow.Shadow

/** RenderEffect/RenderNode backdrop blur requires Android 12 (API 31). */
fun isGlassSupported(sdkInt: Int = Build.VERSION.SDK_INT): Boolean = sdkInt >= Build.VERSION_CODES.S

private const val BLUR_RADIUS_DP = 8f
private const val LENS_HEIGHT = 0.5f
private const val LENS_AMOUNT = 0.5f
private const val LENS_MAX_DP = 48f
private const val SURFACE_OPACITY = 0.4f

internal val GLASS_EDGE_WIDTH = 0.5.dp
internal val GLASS_EDGE_COLOR = Color.White.copy(alpha = 0.10f)

@Composable
fun glassContentColor(): Color =
    if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) Color.Black else Color.White

/**
 * True liquid glass surface sampling [backdrop]: vibrancy, blur, and lens
 * refraction (API 33+ only), then a theme-adaptive tint fill and a specular
 * rim. Returns the receiver unchanged if [backdrop] is null or the device
 * doesn't support it — callers should fall back to the existing Haze path
 * in that case.
 *
 * Values (blur radius, lens height/amount, surface opacity) match BitChord's
 * own liquid glass nav bar. API calls (vibrancy(), no shadow/colorControls
 * params) are written against backdrop 1.0.6, this project's current pinned
 * version — not 2.0.0, which needs compileSdk 37 this project doesn't have.
 */
@Composable
fun Modifier.liquidGlassNav(shape: CornerBasedShape, backdrop: Backdrop?): Modifier {
    if (backdrop == null || !isGlassSupported()) return this
    val isDark = isAppInDarkMode()
    val density = LocalDensity.current
    val blurPx = with(density) { BLUR_RADIUS_DP.dp.toPx() }
    val lensHeightPx = with(density) { (LENS_HEIGHT * LENS_MAX_DP).dp.toPx() }
    val lensAmountPx = with(density) { (LENS_AMOUNT * LENS_MAX_DP).dp.toPx() }
    val surfaceTintColor = if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) {
        Color(0xFFFAFAFA)
    } else {
        Color(0xFF121212)
    }

    return drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            vibrancy()
            blur(blurPx)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                lens(
                    refractionHeight = lensHeightPx,
                    refractionAmount = lensAmountPx,
                    depthEffect = true,
                    chromaticAberration = true
                )
            }
        },
        highlight = { Highlight.Default },
        shadow = { Shadow.Default },
        onDrawSurface = {
            drawRect(surfaceTintColor.copy(alpha = SURFACE_OPACITY))
        }
    )
}
