package com.example.ui.components

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow

internal const val RECORDS_BLUR_RADIUS_DP = 8f
internal const val RECORDS_LENS_HEIGHT = 0.5f
internal const val RECORDS_LENS_AMOUNT = 0.5f
internal const val RECORDS_LENS_MAX_DP = 48f
internal const val RECORDS_SURFACE_OPACITY = 0.4f

/**
 * Liquid Glass modifier strictly for Records section components
 * utilizing the dedicated recordsBackdrop and matching the established
 * liquid glass pipeline (vibrancy, blur, lens on API 33+, Highlight.Default,
 * Shadow.Default, and adaptive surface tint).
 */
@Composable
fun Modifier.recordsLiquidGlass(
    backdrop: Backdrop?,
    shape: Shape,
    customSurfaceTint: Color? = null
): Modifier {
    if (backdrop == null || !isGlassSupported()) return this

    val density = LocalDensity.current
    val isAmoled = isAppInAmoledMode()
    val blurPx = with(density) { RECORDS_BLUR_RADIUS_DP.dp.toPx() }
    val lensHeightPx = with(density) { (RECORDS_LENS_HEIGHT * RECORDS_LENS_MAX_DP).dp.toPx() }
    val lensAmountPx = with(density) { (RECORDS_LENS_AMOUNT * RECORDS_LENS_MAX_DP).dp.toPx() }

    val defaultSurfaceTint = if (isAmoled) {
        Color(0xFF000000)
    } else if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) {
        Color(0xFFFAFAFA)
    } else {
        Color(0xFF121212)
    }
    val surfaceColor = customSurfaceTint ?: defaultSurfaceTint.copy(alpha = RECORDS_SURFACE_OPACITY)

    return this
        .clip(shape)
        .drawBackdrop(
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
                drawRect(surfaceColor)
            }
        )
}
