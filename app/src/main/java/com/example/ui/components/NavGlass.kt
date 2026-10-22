/*
 * Glass rendering for the bottom nav bar and app-wide glass surfaces on top
 * of the vendored Kyant0/backdrop v2.0.0 source (Apache-2.0) that lives in
 * com.example.ui.components.backdrop.
 */
package com.example.ui.components

import android.os.Build
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.ui.components.backdrop.Backdrop
import com.example.ui.components.backdrop.backdrops.LayerBackdrop
import com.example.ui.components.backdrop.drawBackdrop
import com.example.ui.components.backdrop.effects.blur
import com.example.ui.components.backdrop.effects.colorControls
import com.example.ui.components.backdrop.effects.lens
import com.example.ui.components.backdrop.highlight.Highlight
import com.example.ui.components.backdrop.highlight.HighlightStyle
import com.example.ui.components.backdrop.shadow.Shadow
import com.example.ui.theme.GlassStyle
import com.example.ui.theme.LocalLiquidGlassConfig

/**
 * Where a [navLiquidGlass] surface composed under it also records the glass it
 * draws, so the travelling selection pill can refract the bar's glass rather
 * than only the page behind it.
 */
internal val LocalNavGlassExport = compositionLocalOf<LayerBackdrop?> { null }

/** The selection-pill / tab spring. */
internal val NavGlassSpring = spring<Float>(dampingRatio = 0.72f, stiffness = 320f)

/**
 * Renders this composable as a liquid glass surface sampling [backdrop], reading
 * all optical and material parameters from [LocalLiquidGlassConfig].
 *
 * [shape] must be a [CornerBasedShape] - the lens effect throws otherwise.
 */
@Composable
fun Modifier.navLiquidGlass(
    shape: CornerBasedShape,
    backdrop: Backdrop,
    isMenu: Boolean = false
): Modifier {
    val config = LocalLiquidGlassConfig.current
    val surfaceColor = config.surfaceTintColor.copy(alpha = config.surfaceOpacity)

    if (config.glassStyle == GlassStyle.TRANSPARENT) {
        return this
            .clip(shape)
            .background(surfaceColor, shape)
    }

    val exportedBackdrop = LocalNavGlassExport.current
    val density = LocalDensity.current
    val captureScale = if (isMenu) {
        config.menuBackdropResolution.coerceIn(0.3f, 1.0f)
    } else {
        config.normalCaptureScale
    }
    val saturation = config.backdropSaturation
    val blurPx = with(density) { config.blurRadius.dp.toPx() } * captureScale
    val lensHeightPx = with(density) { config.lensRefractionHeightDp.dp.toPx() } * captureScale
    val lensAmountPx = with(density) { config.lensRefractionAmountDp.dp.toPx() } * captureScale
    val isLiquid = config.glassStyle == GlassStyle.LIQUID

    return drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            colorControls(saturation = saturation)
            blur(blurPx)
            if (isLiquid && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                lens(
                    refractionHeight = lensHeightPx,
                    refractionAmount = lensAmountPx,
                    depthEffect = config.depthEffect,
                    chromaticAberration = config.chromaticAberration,
                )
            }
        },
        highlight = if (isLiquid && config.highlightOpacity > 0f) {
            {
                Highlight(
                    alpha = config.highlightOpacity,
                    style = HighlightStyle.Default(color = config.highlightColor)
                )
            }
        } else {
            null
        },
        shadow = if (isLiquid) {
            { Shadow.Default }
        } else {
            null
        },
        onDrawSurface = {
            drawRect(color = surfaceColor, size = size)
        },
        exportedBackdrop = exportedBackdrop,
        backdropScale = captureScale,
    )
}
