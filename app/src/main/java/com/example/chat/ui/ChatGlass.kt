package com.example.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.ui.components.backdrop.Backdrop as NavBackdrop
import com.example.ui.components.glassContentColor
import com.example.ui.components.isAppInAmoledMode
import com.example.ui.components.isAppInDarkMode
import com.example.ui.components.isGlassSupported
import com.example.ui.components.liquidGlassNav
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy

internal fun Backdrop.asNavBackdrop(): NavBackdrop {
    val delegate = this
    return object : NavBackdrop {
        override val isCoordinatesDependent: Boolean
            get() = delegate.isCoordinatesDependent

        override fun DrawScope.drawBackdrop(
            density: Density,
            coordinates: LayoutCoordinates?,
            layerBlock: (GraphicsLayerScope.() -> Unit)?
        ) {
            with(delegate) {
                drawBackdrop(density, coordinates, layerBlock)
            }
        }
    }
}

@Composable
internal fun Modifier.chatHeaderGlass(
    backdrop: Backdrop?,
    shape: CornerBasedShape = RoundedCornerShape(percent = 50)
): Modifier {
    return if (backdrop != null && isGlassSupported()) {
        this.liquidGlassNav(shape = shape, backdrop = backdrop)
    } else {
        this
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            .border(0.5.dp, glassContentColor().copy(alpha = 0.15f), shape)
    }
}

private const val STRIP_BLUR_RADIUS_DP = 8f
private val STRIP_FADE_HEIGHT = 12.dp

@Composable
internal fun Modifier.chatHeaderStripGlass(
    backdrop: Backdrop?
): Modifier {
    val isDark = isAppInDarkMode()
    val isAmoled = isAppInAmoledMode()
    val surfaceColor = MaterialTheme.colorScheme.surface
    val contentColor = glassContentColor()

    if (backdrop == null || !isGlassSupported()) {
        val dividerColor = contentColor.copy(alpha = 0.12f)
        return this
            .background(surfaceColor.copy(alpha = 0.9f))
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

    val density = LocalDensity.current
    val blurPx = with(density) { STRIP_BLUR_RADIUS_DP.dp.toPx() }
    val tintAlpha = if (isDark || isAmoled) 0.6f else 0.7f
    val stripShape = RoundedCornerShape(0.dp)

    return this
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
        .drawBackdrop(
            backdrop = backdrop,
            shape = { stripShape },
            effects = {
                vibrancy()
                blur(blurPx)
                // lens() removed while debugging the native RenderThread crash; re-enable once verified:
                // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                //     lens(
                //         refractionHeight = lensHeightPx,
                //         refractionAmount = lensAmountPx,
                //         depthEffect = true,
                //         chromaticAberration = true
                //     )
                // }
            },
            highlight = null,
            shadow = null,
            onDrawSurface = {
                drawRect(surfaceColor.copy(alpha = tintAlpha))
            }
        )
}
