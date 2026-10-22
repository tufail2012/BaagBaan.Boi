package com.example.ui.components

import android.os.Build
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.ui.components.backdrop.Backdrop as NavBackdrop
import com.example.ui.theme.LiquidGlassPreference
import com.example.ui.theme.LocalLiquidGlassConfig
import com.kyant.backdrop.Backdrop

/** RenderEffect/RenderNode backdrop blur requires Android 12 (API 31). */
fun isGlassSupported(sdkInt: Int = Build.VERSION.SDK_INT): Boolean =
    sdkInt >= Build.VERSION_CODES.S && LiquidGlassPreference.enabled

fun Modifier.glassEdge(shape: CornerBasedShape): Modifier =
    border(GLASS_EDGE_WIDTH, GLASS_EDGE_COLOR, shape)

internal val GLASS_EDGE_WIDTH = 0.5.dp
internal val GLASS_EDGE_COLOR: Brush = Brush.verticalGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.22f),
        Color.White.copy(alpha = 0.06f)
    )
)

@Composable
fun glassContentColor(): Color {
    val config = LocalLiquidGlassConfig.current
    return if (config.adaptiveContrast) {
        if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) Color(0xFF1A1A1A) else Color.White
    } else {
        config.glassTextColor
    }
}

@Composable
fun glassIndicatorColor(): Color =
    if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) Color.White else Color.Black

internal fun Backdrop.asVendoredBackdrop(): NavBackdrop {
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

/**
 * True liquid glass surface sampling [backdrop], reading all parameters from
 * [LocalLiquidGlassConfig]. Returns the receiver unchanged if [backdrop] is null
 * or the device doesn't support glass.
 */
@Composable
fun Modifier.liquidGlassNav(
    shape: CornerBasedShape,
    backdrop: Backdrop?,
    isMenu: Boolean = false
): Modifier {
    if (backdrop == null || !isGlassSupported()) return this
    val vendoredBackdrop = remember(backdrop) { backdrop.asVendoredBackdrop() }
    return this.navLiquidGlass(
        shape = shape,
        backdrop = vendoredBackdrop,
        isMenu = isMenu
    )
}
