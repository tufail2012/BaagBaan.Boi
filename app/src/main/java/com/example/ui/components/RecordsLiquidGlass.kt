package com.example.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CornerBasedShape
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

/**
 * Liquid Glass modifier strictly for Records section components
 * reusing the canonical LiquidGlassNav implementation (vibrancy, blur 8.dp,
 * lens on API 33+, Highlight.Default, Shadow.Default, and surface tint).
 */
@Composable
fun Modifier.recordsLiquidGlass(
    backdrop: Backdrop?,
    shape: Shape,
    customSurfaceTint: Color? = null
): Modifier {
    val cornerShape = shape as? CornerBasedShape ?: RoundedCornerShape(percent = 50)
    return this.liquidGlassNav(shape = cornerShape, backdrop = backdrop)
}

/**
 * Liquid Glass modifier specifically for dropdown menus in the Records and Garden Planning sections:
 * - Seamless frosted translucent surface using Haze and custom gradients
 * - Designed specifically for popup/dropdown windows where cross-window hardware backdrop
 *   sampling causes BLASTBufferQueue transaction leaks on window destruction (dtor)
 * - Safe across all Android versions (including Android 16 / SDK 36 on Samsung)
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
