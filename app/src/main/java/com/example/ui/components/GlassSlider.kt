package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.ui.animation.IosMotion
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

private val SLIDER_TOUCH_HEIGHT = 44.dp
private val SLIDER_TRACK_HEIGHT = 6.dp
private val SLIDER_LENS_HEIGHT = 10.dp
private val SLIDER_LENS_AMOUNT = 14.dp
private const val SLIDER_VELOCITY_DIVISOR = 10f
private const val SLIDER_TRACK_SCALE_MAX = 1f

private fun Density.sliderThumbTranslationX(trackWidthPx: Float, fraction: Float): Float {
    val w = GLASS_THUMB_WIDTH.toPx()
    val raw = -w / 2f + trackWidthPx * fraction.coerceIn(0f, 1f)
    val minX = -w / 4f
    val maxX = (trackWidthPx - 3f * w / 4f).coerceAtLeast(minX)
    return raw.coerceIn(minX, maxX)
}

@Composable
fun GlassSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = Color(0xFF787880).copy(alpha = 0.36f),
    backdrop: Backdrop? = null,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val reduceMotion = remember(context) { IosMotion.isReducedMotion(context) }
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val span = (valueRange.endInclusive - valueRange.start).coerceAtLeast(0f)
    val targetFraction = if (span > 0f) {
        ((value - valueRange.start) / span).coerceIn(0f, 1f)
    } else {
        0f
    }

    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentOnValueChangeFinished by rememberUpdatedState(onValueChangeFinished)
    val currentRange by rememberUpdatedState(valueRange)

    var trackWidthPx by remember { mutableFloatStateOf(0f) }
    val thumbState = rememberGlassDragThumbState(
        value = targetFraction,
        reduceMotion = reduceMotion,
    )
    val capsuleShape = remember { RoundedCornerShape(percent = 50) }
    val glassSupported = isGlassSupported()
    val trackBackdrop = rememberLayerBackdrop()

    fun fractionToValue(fraction: Float): Float {
        val range = currentRange
        val rangeSpan = (range.endInclusive - range.start).coerceAtLeast(0f)
        return (range.start + fraction.coerceIn(0f, 1f) * rangeSpan)
            .coerceIn(range.start, range.endInclusive)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(SLIDER_TOUCH_HEIGHT)
            .alpha(if (enabled) 1f else 0.4f)
            .onSizeChanged { trackWidthPx = it.width.toFloat() }
            .pointerInput(enabled, trackWidthPx, isRtl) {
                if (!enabled || trackWidthPx <= 0f) return@pointerInput
                detectTapGestures { offset ->
                    val rawFraction = offset.x / trackWidthPx
                    val tappedFraction = (if (isRtl) 1f - rawFraction else rawFraction).coerceIn(0f, 1f)
                    thumbState.startPress()
                    currentOnValueChange(fractionToValue(tappedFraction))
                    thumbState.finishRelease(tappedFraction, holdPressForTap = true)
                    currentOnValueChangeFinished?.invoke()
                }
            }
            .glassThumbDragGestures(
                state = thumbState,
                enabled = enabled,
                travelPx = trackWidthPx,
                reduceMotion = reduceMotion,
                currentValue = targetFraction,
                onValueChange = { nextFraction ->
                    currentOnValueChange(fractionToValue(nextFraction))
                },
                onRelease = { releasedFraction ->
                    currentOnValueChangeFinished?.invoke()
                    releasedFraction
                },
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        val trackModifier = Modifier
            .align(Alignment.Center)
            .fillMaxWidth()
            .height(SLIDER_TRACK_HEIGHT)
            .then(
                if (glassSupported) {
                    Modifier.layerBackdrop(trackBackdrop)
                } else {
                    Modifier
                }
            )
            .clip(capsuleShape)
            .drawBehind {
                drawRect(color = inactiveColor)
                val progress = thumbState.animatedValue
                val fillWidth = size.width * progress
                if (fillWidth > 0f) {
                    val startX = if (isRtl) size.width - fillWidth else 0f
                    drawRect(
                        color = activeColor,
                        topLeft = Offset(startX, 0f),
                        size = Size(fillWidth, size.height),
                    )
                }
            }

        Box(modifier = trackModifier)

        if (glassSupported) {
            GlassDragThumb(
                state = thumbState,
                thumbSize = GLASS_THUMB_SIZE,
                travelPx = trackWidthPx,
                value = targetFraction,
                enabled = enabled,
                reduceMotion = reduceMotion,
                velocityDivisor = SLIDER_VELOCITY_DIVISOR,
                backdrop = backdrop,
                trackBackdrop = trackBackdrop,
                trackScaleMax = SLIDER_TRACK_SCALE_MAX,
                lensHeight = SLIDER_LENS_HEIGHT,
                lensAmount = SLIDER_LENS_AMOUNT,
                shadowAlpha = if (enabled) 0.15f else 0.05f,
                translationX = { fraction ->
                    sliderThumbTranslationX(trackWidthPx, fraction)
                },
                onValueChange = { nextFraction ->
                    currentOnValueChange(fractionToValue(nextFraction))
                },
                onRelease = { releasedFraction ->
                    currentOnValueChangeFinished?.invoke()
                    releasedFraction
                },
            )
        } else {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(GLASS_THUMB_SIZE)
                    .graphicsLayer {
                        val ltrOffset = sliderThumbTranslationX(trackWidthPx, thumbState.animatedValue)
                        translationX = if (isRtl) -ltrOffset else ltrOffset
                    }
                    .shadow(elevation = 4.dp, shape = capsuleShape)
                    .background(color = Color.White, shape = capsuleShape),
            )
        }
    }
}
