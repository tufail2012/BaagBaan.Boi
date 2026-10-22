package com.example.chat.ui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.ui.components.asVendoredBackdrop
import com.example.ui.components.backdrop.Backdrop as NavBackdrop
import com.example.ui.components.backdrop.drawBackdrop
import com.example.ui.components.backdrop.effects.blur
import com.example.ui.components.backdrop.effects.colorControls
import com.example.ui.components.backdrop.effects.lens
import com.example.ui.components.glassContentColor
import com.example.ui.components.isAppInAmoledMode
import com.example.ui.components.isAppInDarkMode
import com.example.ui.components.isGlassSupported
import com.example.ui.components.liquidGlassNav
import com.example.ui.theme.GlassStyle
import com.example.ui.theme.LocalLiquidGlassConfig
import com.example.ui.theme.getAppDimBackgroundBrush
import com.kyant.backdrop.Backdrop

internal fun Backdrop.asNavBackdrop(): NavBackdrop = asVendoredBackdrop()

internal const val SELECTION_MENU_REAL_GLASS_ENABLED = true

@Composable
internal fun Modifier.chatHeaderGlass(
    backdrop: Backdrop?,
    shape: CornerBasedShape = RoundedCornerShape(percent = 50),
    isMenu: Boolean = false
): Modifier {
    val config = LocalLiquidGlassConfig.current
    return if (backdrop != null && isGlassSupported() && config.chatGlassEnabled) {
        this.liquidGlassNav(shape = shape, backdrop = backdrop, isMenu = isMenu)
    } else {
        this
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            .border(0.5.dp, glassContentColor().copy(alpha = 0.15f), shape)
    }
}

private val STRIP_FADE_HEIGHT = 32.dp

@Composable
internal fun Modifier.chatHeaderStripGlass(
    backdrop: Backdrop?,
    accentColor: Color = MaterialTheme.colorScheme.primary
): Modifier {
    val config = LocalLiquidGlassConfig.current
    val isDark = isAppInDarkMode()
    val isAmoled = isAppInAmoledMode()
    val contentColor = glassContentColor()
    val bgBrush = remember(accentColor, isDark, isAmoled) {
        getAppDimBackgroundBrush(accentColor, isDark = isDark, isAmoled = isAmoled)
    }

    if (backdrop == null || !isGlassSupported() || !config.chatGlassEnabled) {
        val dividerColor = contentColor.copy(alpha = 0.12f)
        return this
            .background(brush = bgBrush, alpha = 0.9f)
            .drawBehind {
                val strokePx = 0.5.dp.toPx()
                val y = size.height - strokePx / 2f
                drawLine(
                    color = dividerColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = strokePx
                )
            }
    }

    val tintAlpha = config.surfaceOpacity
    // Use 1.dp corner radius instead of 0.dp so lens() shader has a non-zero corner radius
    val stripShape = RoundedCornerShape(1.dp)

    val fadeModifier = this
        .graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
        }
        .drawWithContent {
            drawContent()
            val fadePx = STRIP_FADE_HEIGHT.toPx().coerceAtMost(size.height)
            if (fadePx > 0f && size.height > 0f) {
                val fadeStart = ((size.height - fadePx) / size.height).coerceIn(0f, 1f)
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.Black,
                        fadeStart to Color.Black,
                        1f to Color.Transparent
                    ),
                    blendMode = BlendMode.DstIn
                )
            }
        }

    if (config.glassStyle == GlassStyle.TRANSPARENT) {
        return fadeModifier.background(brush = bgBrush, alpha = tintAlpha)
    }

    val density = LocalDensity.current
    val captureScale = config.normalCaptureScale
    val saturation = config.backdropSaturation
    val blurPx = with(density) { config.blurRadius.dp.toPx() } * captureScale
    val lensHeightPx = with(density) { config.lensRefractionHeightDp.dp.toPx() } * captureScale
    val lensAmountPx = with(density) { config.lensRefractionAmountDp.dp.toPx() } * captureScale
    val isLiquid = config.glassStyle == GlassStyle.LIQUID
    val navBackdrop = remember(backdrop) { backdrop.asVendoredBackdrop() }

    return fadeModifier
        .drawBackdrop(
            backdrop = navBackdrop,
            shape = { stripShape },
            effects = {
                colorControls(saturation = saturation)
                blur(blurPx)
                if (isLiquid && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    lens(
                        refractionHeight = lensHeightPx,
                        refractionAmount = lensAmountPx,
                        depthEffect = config.depthEffect,
                        chromaticAberration = config.chromaticAberration
                    )
                }
            },
            highlight = null,
            shadow = null,
            onDrawSurface = {
                drawRect(brush = bgBrush, alpha = tintAlpha)
            },
            backdropScale = captureScale
        )
}
