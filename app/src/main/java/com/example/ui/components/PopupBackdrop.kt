package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.unit.Density
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * A specialized Backdrop wrapper for Popups (such as DropdownMenu) that corrects
 * the coordinate mapping across separate Android Window boundaries.
 *
 * In Android Compose, a Popup lives in its own Window (PopupLayout). When a standard
 * LayerBackdrop computes relative coordinates across separate window hierarchies,
 * [LayoutCoordinates.localPositionOf] throws an exception, and the fallback
 * [LayoutCoordinates.positionInWindow] returns Offset(0, 0) for the popup root.
 * This caused the backdrop to sample from (0, 0) — the far-left of the screen — rather
 * than the actual pixels directly behind the popup.
 *
 * By resolving the absolute physical screen coordinates via [LayoutCoordinates.positionOnScreen]
 * on both the Popup consumer and the Backdrop source, this wrapper ensures that only
 * the live pixels directly behind the Profile Menu are sampled, blurred, and refracted.
 */
class PopupBackdrop(
    private val source: Backdrop,
    private val sourceCoordinatesProvider: (() -> LayoutCoordinates?)? = null
) : Backdrop {

    override val isCoordinatesDependent: Boolean
        get() = true

    override fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?
    ) {
        if (coordinates == null) return

        val layerBackdrop = source as? LayerBackdrop
        if (layerBackdrop == null) {
            with(source) {
                drawBackdrop(density, coordinates, layerBlock)
            }
            return
        }

        val layerCoordinates = sourceCoordinatesProvider?.invoke() ?: getBackdropCoordinates(layerBackdrop)
        if (layerCoordinates == null || !coordinates.isAttached || !layerCoordinates.isAttached) {
            with(source) {
                drawBackdrop(density, coordinates, layerBlock)
            }
            return
        }

        val graphicsLayer = layerBackdrop.graphicsLayer

        drawContext.canvas.save()

        val offset: Offset = try {
            val popupOnScreen = coordinates.positionOnScreen()
            val sourceOnScreen = layerCoordinates.positionOnScreen()
            popupOnScreen - sourceOnScreen
        } catch (_: Exception) {
            try {
                layerCoordinates.localPositionOf(coordinates, Offset.Zero)
            } catch (_: Exception) {
                Offset.Zero
            }
        }

        drawContext.transform.translate(-offset.x, -offset.y)
        drawLayer(graphicsLayer)
        drawContext.canvas.restore()
    }

    companion object {
        private val layerCoordinatesMethod: Method? by lazy {
            try {
                LayerBackdrop::class.java.getDeclaredMethod("getLayerCoordinates\$backdrop").apply {
                    isAccessible = true
                }
            } catch (_: Exception) {
                null
            }
        }

        private val layerCoordinatesField: Field? by lazy {
            try {
                LayerBackdrop::class.java.getDeclaredField("layerCoordinates\$delegate").apply {
                    isAccessible = true
                }
            } catch (_: Exception) {
                null
            }
        }

        fun getBackdropCoordinates(backdrop: LayerBackdrop): LayoutCoordinates? {
            return try {
                layerCoordinatesMethod?.invoke(backdrop) as? LayoutCoordinates
            } catch (_: Exception) {
                try {
                    @Suppress("UNCHECKED_CAST")
                    (layerCoordinatesField?.get(backdrop) as? androidx.compose.runtime.State<*>)?.value as? LayoutCoordinates
                } catch (_: Exception) {
                    null
                }
            }
        }
    }
}

/**
 * Remembers a [PopupBackdrop] wrapping this [Backdrop], ensuring accurate cross-window
 * screen coordinate sampling for popup menus without modifying the underlying renderer.
 */
@Composable
fun rememberPopupBackdrop(
    backdrop: Backdrop?,
    sourceCoordinatesProvider: (() -> LayoutCoordinates?)? = null
): Backdrop? {
    if (backdrop == null) return null
    return remember(backdrop, sourceCoordinatesProvider) {
        if (backdrop is PopupBackdrop) backdrop else PopupBackdrop(backdrop, sourceCoordinatesProvider)
    }
}
