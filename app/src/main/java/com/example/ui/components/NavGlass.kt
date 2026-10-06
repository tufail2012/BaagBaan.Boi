/*
 * Glass rendering for the bottom nav bar. Ported 1:1 from the reference app's
 * LiquidGlass.kt (itself adapted from EchoMusicApp/Echo-Music, GPL-3.0) on top
 * of the vendored Kyant0/backdrop v2.0.0 source (Apache-2.0) that lives in
 * com.example.ui.components.backdrop.
 *
 * Names are prefixed "Nav"/"nav" so they never clash with LiquidGlassNav.kt
 * (the older Maven-backdrop 1.0.6 helpers the rest of the app still uses).
 * isGlassSupported(), glassContentColor(), glassIndicatorColor() and the
 * GLASS_EDGE_* values are reused from LiquidGlassNav.kt.
 */
package com.example.ui.components

import android.os.Build
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.ui.components.backdrop.Backdrop
import com.example.ui.components.backdrop.backdrops.LayerBackdrop
import com.example.ui.components.backdrop.drawBackdrop
import com.example.ui.components.backdrop.effects.blur
import com.example.ui.components.backdrop.effects.colorControls
import com.example.ui.components.backdrop.effects.lens
import com.example.ui.components.backdrop.highlight.Highlight
import com.example.ui.components.backdrop.shadow.Shadow

/**
 * Where a [navLiquidGlass] surface composed under it also records the glass it
 * draws, so the travelling selection pill can refract the bar's glass rather
 * than only the page behind it.
 */
internal val LocalNavGlassExport = compositionLocalOf<LayerBackdrop?> { null }

/** the reference app's selection-pill / tab spring (GlassSpring). */
internal val NavGlassSpring = spring<Float>(dampingRatio = 0.72f, stiffness = 320f)

/** Apple-matched defaults, identical to the reference design. */
private const val VIBRANCY = 1f
private const val BLUR_RADIUS_DP = 8f
private const val LENS_HEIGHT_FRACTION = 0.5f
private const val LENS_AMOUNT_FRACTION = 0.5f
private const val LENS_MAX_DP = 48f
private const val SURFACE_OPACITY = 0.4f

/**
 * Resolution fraction the glass records and processes its backdrop at.
 * Blur radius and lens parameters are pre-multiplied by it (see below).
 */
private const val GLASS_RESOLUTION_SCALE = 0.33f

/**
 * Renders this composable as a liquid glass surface sampling [backdrop]:
 * vibrancy, blur and lens refraction, then a theme-adaptive surface tint.
 * Exact copy of the reference app's `Modifier.liquidGlass`.
 *
 * [shape] must be a [CornerBasedShape] — the lens effect throws otherwise.
 */
@Composable
fun Modifier.navLiquidGlass(shape: CornerBasedShape, backdrop: Backdrop): Modifier {
    val exportedBackdrop = LocalNavGlassExport.current
    val density = LocalDensity.current
    val blurPx = with(density) { BLUR_RADIUS_DP.dp.toPx() } * GLASS_RESOLUTION_SCALE
    val lensHeightPx =
        with(density) { (LENS_HEIGHT_FRACTION * LENS_MAX_DP).dp.toPx() } * GLASS_RESOLUTION_SCALE
    val lensAmountPx =
        with(density) { (LENS_AMOUNT_FRACTION * LENS_MAX_DP).dp.toPx() } * GLASS_RESOLUTION_SCALE
    val surfaceTintColor = if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) {
        Color(0xFFFAFAFA)
    } else {
        Color(0xFF121212)
    }

    return drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            colorControls(saturation = 1f + 0.5f * VIBRANCY)
            blur(blurPx)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                lens(
                    refractionHeight = lensHeightPx,
                    refractionAmount = lensAmountPx,
                    depthEffect = true,
                    chromaticAberration = true,
                )
            }
        },
        highlight = { Highlight.Default },
        shadow = { Shadow.Default },
        onDrawSurface = {
            drawRect(color = surfaceTintColor.copy(alpha = SURFACE_OPACITY), size = size)
        },
        exportedBackdrop = exportedBackdrop,
        backdropScale = GLASS_RESOLUTION_SCALE,
    )
}
