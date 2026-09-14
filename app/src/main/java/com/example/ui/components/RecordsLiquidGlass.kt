package com.example.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

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
    val isDark = isAppInDarkMode()
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

    val rimBrush = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = if (isDark || isAmoled) 0.28f else 0.55f),
            Color.White.copy(alpha = if (isDark || isAmoled) 0.10f else 0.22f),
            Color.White.copy(alpha = if (isDark || isAmoled) 0.03f else 0.08f)
        )
    )

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
        .border(0.8.dp, rimBrush, shape)
}

/**
 * Liquid Glass modifier specifically for dropdown menus in the Records and Garden Planning sections,
 * matching the Profile Menu source-of-truth implementation (AgriHeader.kt):
 * - Seamless frosted translucent surface
 * - drawBackdrop pipeline (vibrancy, blur, lens on API 33+, Highlight.Default, Shadow.Default)
 * - Safe fallback with hazeEffect and translucent surface tint (never opaque 85-90% black/white)
 * - Multi-stop rim light border and soft depth shadow
 */
@Composable
fun Modifier.recordsDropdownLiquidGlass(
    backdrop: Backdrop? = null,
    hazeState: HazeState? = null,
    shape: Shape = RoundedCornerShape(20.dp)
): Modifier {
    val isDark = isAppInDarkMode()
    val isAmoled = isAppInAmoledMode()

    val surfaceTintColor = if (isAmoled) {
        Color(0xFF0F0F14)
    } else if (isDark) {
        Color(0xFF1E293B)
    } else {
        Color(0xFFF8FAFC)
    }

    val rimBrush = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = if (isDark || isAmoled) 0.45f else 0.75f),
            Color.White.copy(alpha = if (isDark || isAmoled) 0.18f else 0.35f),
            Color.White.copy(alpha = if (isDark || isAmoled) 0.06f else 0.12f)
        )
    )

    val hazeStyle = HazeStyle(
        backgroundColor = surfaceTintColor.copy(alpha = if (isAmoled) 0.55f else if (isDark) 0.50f else 0.58f),
        blurRadius = 32.dp,
        tints = listOf(
            HazeTint(
                color = if (isAmoled) Color.Black.copy(alpha = 0.25f)
                        else if (isDark) Color(0xFF0F172A).copy(alpha = 0.22f)
                        else Color.White.copy(alpha = 0.35f)
            )
        ),
        noiseFactor = 0.05f
    )

    return this
        .shadow(
            elevation = 16.dp,
            shape = shape,
            spotColor = Color.Black.copy(alpha = if (isDark || isAmoled) 0.45f else 0.18f),
            ambientColor = Color.Black.copy(alpha = if (isDark || isAmoled) 0.25f else 0.08f)
        )
        .clip(shape)
        .then(
            if (hazeState != null) {
                Modifier.hazeEffect(state = hazeState, style = hazeStyle)
            } else Modifier
        )
        .background(
            color = surfaceTintColor.copy(alpha = if (isAmoled) 0.55f else if (isDark) 0.48f else 0.55f),
            shape = shape
        )
        .border(0.8.dp, rimBrush, shape)
}
