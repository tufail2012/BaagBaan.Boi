package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.ui.animation.IosMotion
import com.example.ui.components.backdrop.Backdrop
import com.example.ui.theme.LocalLiquidGlassConfig
import kotlin.math.roundToInt
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private const val MORPH_BLOB_SCALE = 0.12f

/**
 * Provides the sibling backdrop layer (such as the chat screen's `chatGlass`)
 * so [MorphMenu] can apply the same `glassModifier` without changing its
 * required parameter signature.
 */
val LocalMorphMenuBackdrop = compositionLocalOf<Backdrop?> { null }

@Composable
fun MorphMenu(
    expanded: Boolean,
    anchorBounds: Rect,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 220.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val reduceMotion = remember(context) { IosMotion.isReducedMotion(context) }
    val currentOnDismissRequest by rememberUpdatedState(onDismissRequest)

    var isPresent by remember { mutableStateOf(expanded) }
    var isMeasured by remember { mutableStateOf(false) }
    var closeRequested by remember { mutableStateOf(false) }
    var previousExpanded by remember { mutableStateOf(expanded) }

    if (expanded && !previousExpanded) {
        isPresent = true
        closeRequested = false
    }
    previousExpanded = expanded

    val shouldBeOpen = expanded && !closeRequested

    val scaleX = remember { Animatable(MORPH_BLOB_SCALE) }
    val scaleY = remember { Animatable(MORPH_BLOB_SCALE) }
    val alpha = remember { Animatable(0f) }

    BackHandler(enabled = isPresent && shouldBeOpen) {
        closeRequested = true
    }

    LaunchedEffect(shouldBeOpen, isMeasured, reduceMotion) {
        if (!isPresent) return@LaunchedEffect

        if (shouldBeOpen) {
            if (!isMeasured) {
                scaleX.snapTo(MORPH_BLOB_SCALE)
                scaleY.snapTo(MORPH_BLOB_SCALE)
                alpha.snapTo(0f)
                return@LaunchedEffect
            }
            if (reduceMotion) {
                scaleX.snapTo(1f)
                scaleY.snapTo(1f)
                alpha.snapTo(1f)
            } else {
                scaleX.snapTo(MORPH_BLOB_SCALE)
                scaleY.snapTo(MORPH_BLOB_SCALE)
                alpha.snapTo(0f)
                coroutineScope {
                    launch {
                        alpha.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(durationMillis = 90),
                        )
                    }
                    launch {
                        scaleX.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(dampingRatio = 0.72f, stiffness = 700f),
                        )
                    }
                    launch {
                        scaleY.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(dampingRatio = 0.78f, stiffness = 420f),
                        )
                    }
                }
            }
        } else {
            if (!isMeasured || reduceMotion) {
                scaleX.snapTo(MORPH_BLOB_SCALE)
                scaleY.snapTo(MORPH_BLOB_SCALE)
                alpha.snapTo(0f)
                isPresent = false
                isMeasured = false
                closeRequested = false
                currentOnDismissRequest()
            } else {
                coroutineScope {
                    launch {
                        scaleX.animateTo(
                            targetValue = MORPH_BLOB_SCALE,
                            animationSpec = spring(dampingRatio = 1f, stiffness = 900f),
                        )
                    }
                    launch {
                        scaleY.animateTo(
                            targetValue = MORPH_BLOB_SCALE,
                            animationSpec = spring(dampingRatio = 1f, stiffness = 900f),
                        )
                    }
                    launch {
                        alpha.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(durationMillis = 140, delayMillis = 40),
                        )
                    }
                }
                isPresent = false
                isMeasured = false
                closeRequested = false
                currentOnDismissRequest()
            }
        }
    }

    if (!isPresent) return

    var hostWindowOffset by remember { mutableStateOf(Offset.Zero) }
    var hostSize by remember { mutableStateOf(IntSize.Zero) }
    var panelSize by remember { mutableStateOf(IntSize.Zero) }

    val panelWidthPx = if (panelSize.width > 0) {
        panelSize.width
    } else {
        with(density) { width.roundToPx() }
    }
    val panelHeightPx = panelSize.height
    val gapPx = with(density) { 8.dp.toPx() }
    val marginPx = with(density) { 10.dp.toPx() }

    val hasValidAnchor = anchorBounds.width > 0f && anchorBounds.height > 0f
    val localAnchor = if (hasValidAnchor) {
        anchorBounds.translate(-hostWindowOffset.x, -hostWindowOffset.y)
    } else {
        Rect.Zero
    }

    val minX = marginPx
    val maxX = if (hostSize.width > 0) {
        (hostSize.width - panelWidthPx - marginPx).coerceAtLeast(minX)
    } else {
        Float.MAX_VALUE
    }
    val panelLeftPx = if (hasValidAnchor) {
        (localAnchor.right - panelWidthPx).coerceIn(minX, maxX)
    } else if (hostSize.width > 0) {
        maxX
    } else {
        minX
    }

    val minY = marginPx
    val maxY = if (hostSize.height > 0 && panelHeightPx > 0) {
        (hostSize.height - panelHeightPx - marginPx).coerceAtLeast(minY)
    } else {
        Float.MAX_VALUE
    }
    val panelTopPx = if (hasValidAnchor) {
        val belowTop = localAnchor.bottom + gapPx
        val runsPastBottom = hostSize.height > 0 &&
            panelHeightPx > 0 &&
            belowTop + panelHeightPx > hostSize.height - marginPx
        val desiredTop = if (runsPastBottom) {
            localAnchor.top - gapPx - panelHeightPx
        } else {
            belowTop
        }
        desiredTop.coerceIn(minY, maxY)
    } else {
        minY
    }

    val originX = if (panelWidthPx > 0 && hasValidAnchor) {
        ((localAnchor.center.x - panelLeftPx) / panelWidthPx.toFloat()).coerceIn(0f, 1f)
    } else {
        1f
    }
    val originY = if (panelHeightPx > 0 && hasValidAnchor) {
        ((localAnchor.center.y - panelTopPx) / panelHeightPx.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val menuTransformOrigin = TransformOrigin(originX, originY)

    val isDark = isAppInDarkMode()
    val glassConfig = LocalLiquidGlassConfig.current
    val backdrop = LocalMorphMenuBackdrop.current
    val glassOn = isGlassSupported() && glassConfig.chatGlassEnabled && backdrop != null
    val menuShape = RoundedCornerShape(20.dp)
    val surfaceModifier = if (glassOn && backdrop != null) {
        Modifier.navLiquidGlass(shape = menuShape, backdrop = backdrop)
    } else {
        Modifier
            .clip(menuShape)
            .background(if (isDark) Color(0x991E293B) else Color(0xB3FFFFFF))
            .border(0.5.dp, Color.White.copy(alpha = 0.10f), menuShape)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(20f)
            .onGloballyPositioned { coords ->
                hostWindowOffset = coords.positionInWindow()
                hostSize = coords.size
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { closeRequested = true },
                )
                .testTag("conversation_more_menu_outside_dismiss"),
        )

        Column(
            modifier = modifier
                .offset { IntOffset(panelLeftPx.roundToInt(), panelTopPx.roundToInt()) }
                .width(width)
                .onSizeChanged { size ->
                    panelSize = size
                    if (size.width > 0 && size.height > 0 && !isMeasured) {
                        isMeasured = true
                    }
                }
                .graphicsLayer {
                    val effectiveScaleX = when {
                        !isMeasured -> MORPH_BLOB_SCALE
                        reduceMotion && shouldBeOpen -> 1f
                        else -> scaleX.value
                    }
                    val effectiveScaleY = when {
                        !isMeasured -> MORPH_BLOB_SCALE
                        reduceMotion && shouldBeOpen -> 1f
                        else -> scaleY.value
                    }
                    val effectiveAlpha = when {
                        !isMeasured -> 0f
                        reduceMotion && shouldBeOpen -> 1f
                        else -> alpha.value
                    }
                    this.scaleX = effectiveScaleX
                    this.scaleY = effectiveScaleY
                    this.alpha = effectiveAlpha
                    this.transformOrigin = menuTransformOrigin
                }
                .then(surfaceModifier)
                .border(0.5.dp, Color.White.copy(alpha = 0.15f), menuShape)
                .clip(menuShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .padding(vertical = 8.dp)
                .testTag("conversation_overflow_glass_menu"),
            content = content,
        )
    }
}
