/*
 * Rendering half of the selection pill.
 *
 * The motion half (spring / lift / squash / hand-off) is in GlassPillMotion.kt.
 */
package com.example.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.example.ui.components.backdrop.Backdrop
import com.example.ui.components.backdrop.backdrops.LayerBackdrop
import com.example.ui.components.backdrop.drawBackdrop
import com.example.ui.components.backdrop.effects.lens
import com.example.ui.components.backdrop.highlight.Highlight
import com.example.ui.components.backdrop.highlight.HighlightStyle
import com.example.ui.components.backdrop.shadow.Shadow
import com.example.ui.theme.GlassStyle
import com.example.ui.theme.LiquidGlassSettings
import com.example.ui.theme.LocalLiquidGlassConfig
import kotlin.math.roundToInt

/** How much taller than the bar the pill stands while it is lifted. */
internal val PILL_GROW_HEIGHT = 9.dp

/**
 * Places a pill-sized child centred on [center] at [size], inside a parent it
 * matches the size of - so the pill can move and grow past that parent's
 * edges without ever changing its measured size.
 */
internal fun Modifier.pillPlacement(center: () -> Offset, size: () -> Size): Modifier =
    layout { measurable, constraints ->
        val s = size()
        val w = s.width.roundToInt().coerceAtLeast(1)
        val h = s.height.roundToInt().coerceAtLeast(1)
        val placeable = measurable.measure(Constraints.fixed(w, h))
        layout(constraints.minWidth, constraints.minHeight) {
            val c = center()
            placeable.place((c.x - w / 2f).roundToInt(), (c.y - h / 2f).roundToInt())
        }
    }

/** The flat pill: a fill using selectionPillColor and selectionPillOpacity. */
@Composable
internal fun FlatSelectionPill(
    color: Color,
    modifier: Modifier,
) {
    val config = LocalLiquidGlassConfig.current
    val resolvedColor = config.selectionPillColor.copy(alpha = config.selectionPillOpacity)
    val shape = remember { RoundedCornerShape(percent = 50) }
    Box(modifier.background(resolvedColor, shape))
}

/**
 * The lifted pill: a lens over the tab bar that refracts the bar's own glass
 * and its tab row, drawn above both and free to stand past the bar's edges.
 */
@Composable
internal fun GlassSelectionPill(
    motion: GlassPillMotion,
    fillColor: Color,
    page: Backdrop,
    barGlass: LayerBackdrop,
    tabRow: LayerBackdrop,
    modifier: Modifier,
) {
    val config = LocalLiquidGlassConfig.current
    val restWashColor = config.selectionPillColor.copy(alpha = config.selectionPillOpacity)
    val highlightColor = config.highlightColor
    val highlightOpacity = config.highlightOpacity
    val isLiquid = config.glassStyle == GlassStyle.LIQUID

    val shape = remember { RoundedCornerShape(percent = 50) }
    val backdrop = remember(page, barGlass, tabRow, restWashColor) {
        SelectionPillBackdrop(page, barGlass, tabRow) {
            restWashColor.copy(alpha = restWashColor.alpha * (1f - motion.material))
        }
    }
    val lensHeight = PILL_LENS_HEIGHT * (config.lensHeight / LiquidGlassSettings.DEFAULT_LENS_HEIGHT)
    val lensAmount = PILL_LENS_AMOUNT * (config.lensAmount / LiquidGlassSettings.DEFAULT_LENS_AMOUNT)

    Box(
        modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                if (isLiquid && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    lens(
                        refractionHeight = lensHeight.toPx() * motion.glassPresence,
                        refractionAmount = lensAmount.toPx() * motion.material,
                        depthEffect = config.depthEffect,
                        chromaticAberration = true,
                    )
                }
            },
            highlight = {
                val alpha = if (isLiquid) {
                    motion.material * rimOf(motion.glassPresence) * highlightOpacity
                } else {
                    0f
                }
                if (alpha > 0f) {
                    Highlight(
                        alpha = alpha,
                        style = HighlightStyle.Default(color = highlightColor)
                    )
                } else {
                    null
                }
            },
            shadow = {
                val alpha = if (isLiquid) {
                    motion.material * rimOf(motion.glassPresence)
                } else {
                    0f
                }
                if (alpha > 0f) PILL_SHADOW.copy(alpha = alpha) else null
            },
        ),
    )
}

/** The rim and its shadow are gone by the time the hand-off is 55% through. */
private fun rimOf(presence: Float): Float = ((presence - 0.45f) / 0.55f).coerceIn(0f, 1f)

private val PILL_LENS_HEIGHT = 12.dp
private val PILL_LENS_AMOUNT = 14.dp

private val PILL_SHADOW = Shadow(
    radius = 12.dp,
    offset = DpOffset(0.dp, 2.dp),
    color = Color.Black.copy(alpha = 0.3f),
)

/**
 * What the pill refracts, bottom to top: the page, the bar's glass, the
 * selection fill, and the tab row.
 */
private class SelectionPillBackdrop(
    private val page: Backdrop,
    private val bar: Backdrop,
    private val tabs: Backdrop,
    private val fill: () -> Color,
) : Backdrop {

    override val isCoordinatesDependent: Boolean = true

    override fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?,
    ) {
        with(page) { drawBackdrop(density, coordinates, layerBlock) }
        with(bar) { drawBackdrop(density, coordinates, layerBlock) }
        val color = fill()
        if (color.alpha > 0f) {
            // Past every edge: the recorded area includes the lens's padding.
            drawRect(
                color = color,
                topLeft = Offset(-size.width, -size.height),
                size = Size(size.width * 3, size.height * 3),
            )
        }
        with(tabs) { drawBackdrop(density, coordinates, layerBlock) }
    }
}
