package com.example.ui.components

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

internal val GLASS_THUMB_WIDTH = 40.dp
internal val GLASS_THUMB_HEIGHT = 24.dp
internal val GLASS_THUMB_SIZE = DpSize(GLASS_THUMB_WIDTH, GLASS_THUMB_HEIGHT)
private const val PRESSED_SCALE = 1.5f

@Stable
internal class GlassDragThumbState(
    initialValue: Float,
    private val scope: CoroutineScope,
    reduceMotion: Boolean,
) {
    var reduceMotion: Boolean = reduceMotion

    val valueSpec: AnimationSpec<Float>
        get() = if (reduceMotion) snap() else spring(
            dampingRatio = 1.0f,
            stiffness = 1000f,
            visibilityThreshold = 0.001f,
        )

    val pressSpec: AnimationSpec<Float>
        get() = if (reduceMotion) snap() else spring(
            dampingRatio = 1.0f,
            stiffness = 1000f,
            visibilityThreshold = 0.001f,
        )

    val velocitySpec: AnimationSpec<Float>
        get() = if (reduceMotion) snap() else spring(
            dampingRatio = 0.5f,
            stiffness = 300f,
            visibilityThreshold = 0.01f,
        )

    val scaleXSpec: AnimationSpec<Float>
        get() = if (reduceMotion) snap() else spring(
            dampingRatio = 0.6f,
            stiffness = 250f,
            visibilityThreshold = 0.001f,
        )

    val scaleYSpec: AnimationSpec<Float>
        get() = if (reduceMotion) snap() else spring(
            dampingRatio = 0.7f,
            stiffness = 250f,
            visibilityThreshold = 0.001f,
        )

    val value = Animatable(initialValue.coerceIn(0f, 1f))
    val pressProgress = Animatable(0f)
    val velocity = Animatable(0f)
    val scaleX = Animatable(1f)
    val scaleY = Animatable(1f)
    private val mutatorMutex = MutatorMutex()

    var dragging by mutableStateOf(false)
    var releaseJob by mutableStateOf<Job?>(null)
        private set

    val animatedValue: Float
        get() = value.value.coerceIn(0f, 1f)

    fun startPress() {
        releaseJob?.cancel()
        releaseJob = null
        scope.launch { pressProgress.animateTo(1f, pressSpec) }
        scope.launch { scaleX.animateTo(PRESSED_SCALE, scaleXSpec) }
        scope.launch { scaleY.animateTo(PRESSED_SCALE, scaleYSpec) }
    }

    fun snapValue(nextValue: Float) {
        scope.launch {
            value.snapTo(nextValue.coerceIn(0f, 1f))
        }
    }

    fun snapVelocity(fractionVelocity: Float) {
        if (reduceMotion) return
        scope.launch {
            velocity.snapTo(fractionVelocity)
        }
    }

    fun resetVelocity() {
        if (reduceMotion) return
        scope.launch {
            velocity.animateTo(0f, velocitySpec)
        }
    }

    fun finishRelease(targetValue: Float, holdPressForTap: Boolean = false) {
        val clampedTarget = targetValue.coerceIn(0f, 1f)
        releaseJob?.cancel()
        resetVelocity()
        releaseJob = scope.launch {
            mutatorMutex.mutate {
                launch { value.animateTo(clampedTarget, valueSpec) }
                if (holdPressForTap && !reduceMotion) {
                    launch { pressProgress.animateTo(1f, pressSpec) }
                    launch { scaleX.animateTo(PRESSED_SCALE, scaleXSpec) }
                    launch { scaleY.animateTo(PRESSED_SCALE, scaleYSpec) }
                    withTimeoutOrNull(220L) {
                        snapshotFlow { pressProgress.value >= 0.65f }
                            .first { it }
                    }
                }
                withTimeoutOrNull(400L) {
                    snapshotFlow { abs(value.value - clampedTarget) <= 0.025f }
                        .first { it }
                }
                launch { pressProgress.animateTo(0f, pressSpec) }
                launch { scaleX.animateTo(1f, scaleXSpec) }
                launch { scaleY.animateTo(1f, scaleYSpec) }
            }
        }
    }
}

@Composable
internal fun rememberGlassDragThumbState(
    value: Float,
    reduceMotion: Boolean,
): GlassDragThumbState {
    val scope = rememberCoroutineScope()
    val state = remember {
        GlassDragThumbState(
            initialValue = value,
            scope = scope,
            reduceMotion = reduceMotion,
        )
    }
    state.reduceMotion = reduceMotion

    LaunchedEffect(value, state.dragging) {
        if (!state.dragging && state.releaseJob?.isActive != true) {
            state.value.animateTo(value.coerceIn(0f, 1f), state.valueSpec)
        }
    }

    return state
}

@Composable
internal fun Modifier.glassThumbDragGestures(
    state: GlassDragThumbState,
    enabled: Boolean,
    travelPx: Float,
    reduceMotion: Boolean,
    currentValue: Float,
    onValueChange: (Float) -> Unit,
    onRelease: (Float) -> Float,
): Modifier {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val latestValue by rememberUpdatedState(currentValue)
    val latestOnValueChange by rememberUpdatedState(onValueChange)
    val latestOnRelease by rememberUpdatedState(onRelease)

    return this.pointerInput(enabled, travelPx, isRtl, reduceMotion) {
        if (!enabled || travelPx <= 0f) return@pointerInput
        val velocityTracker = VelocityTracker()
        val direction = if (isRtl) -1f else 1f
        detectHorizontalDragGestures(
            onDragStart = {
                if (!reduceMotion) {
                    velocityTracker.resetTracking()
                }
                state.dragging = true
                state.startPress()
            },
            onDragCancel = {
                state.dragging = false
                state.resetVelocity()
                state.finishRelease(latestValue.coerceIn(0f, 1f))
            },
            onDragEnd = {
                state.dragging = false
                state.resetVelocity()
                val targetFraction = latestOnRelease(state.value.value.coerceIn(0f, 1f))
                state.finishRelease(targetFraction)
            },
            onHorizontalDrag = { change, dragAmount ->
                change.consume()
                val nextValue = (state.value.value + (dragAmount * direction) / travelPx).coerceIn(0f, 1f)
                state.snapValue(nextValue)
                latestOnValueChange(nextValue)
                if (!reduceMotion) {
                    velocityTracker.addPosition(change.uptimeMillis, change.position)
                    val pixelVelocity = velocityTracker.calculateVelocity().x
                    val fractionVelocity = (pixelVelocity / travelPx) * direction
                    state.snapVelocity(fractionVelocity)
                }
            },
        )
    }
}

@Composable
internal fun BoxScope.GlassDragThumb(
    state: GlassDragThumbState,
    thumbSize: DpSize = GLASS_THUMB_SIZE,
    travelPx: Float,
    value: Float,
    enabled: Boolean,
    reduceMotion: Boolean,
    velocityDivisor: Float,
    backdrop: Backdrop?,
    trackBackdrop: Backdrop,
    trackScaleMax: Float,
    lensHeight: Dp,
    lensAmount: Dp,
    shadowAlpha: Float = 0.05f,
    translationX: Density.(Float) -> Float,
    onValueChange: (Float) -> Unit,
    onRelease: (Float) -> Float,
    modifier: Modifier = Modifier,
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val capsuleShape = remember { RoundedCornerShape(percent = 50) }

    val scaledTrackBackdrop = rememberBackdrop(trackBackdrop) { drawBackdrop ->
        val p = state.pressProgress.value
        if (p != 0f) {
            scale(
                scaleX = lerp(2f / 3f, trackScaleMax, p),
                scaleY = lerp(0f, trackScaleMax, p),
            ) {
                drawBackdrop()
            }
        }
    }
    val thumbBackdrop = if (backdrop != null) {
        rememberCombinedBackdrop(backdrop, scaledTrackBackdrop)
    } else {
        scaledTrackBackdrop
    }

    Box(
        modifier = modifier
            .align(Alignment.CenterStart)
            .size(thumbSize)
            .drawBackdrop(
                backdrop = thumbBackdrop,
                shape = { capsuleShape },
                effects = {
                    val p = state.pressProgress.value
                    blur(8f.dp.toPx() * (1f - p))
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        lens(
                            refractionHeight = lensHeight.toPx() * p,
                            refractionAmount = lensAmount.toPx() * p,
                            depthEffect = false,
                            chromaticAberration = true,
                        )
                    }
                },
                highlight = {
                    val p = state.pressProgress.value
                    Highlight.Ambient.copy(
                        width = Highlight.Ambient.width / 1.5f,
                        blurRadius = Highlight.Ambient.blurRadius / 1.5f,
                        alpha = p,
                    )
                },
                shadow = {
                    Shadow(
                        radius = 4.dp,
                        color = Color.Black.copy(alpha = shadowAlpha),
                    )
                },
                innerShadow = {
                    val p = state.pressProgress.value
                    InnerShadow(
                        radius = 4.dp * p,
                        alpha = p,
                    )
                },
                layerBlock = {
                    val v = if (reduceMotion) 0f else state.velocity.value / velocityDivisor
                    val sx = state.scaleX.value / (1f - (v * 0.75f).coerceIn(-0.2f, 0.2f))
                    val sy = state.scaleY.value * (1f - (v * 0.25f).coerceIn(-0.2f, 0.2f))
                    this.scaleX = sx
                    this.scaleY = sy
                    val ltrOffset = translationX(state.value.value)
                    this.translationX = if (isRtl) -ltrOffset else ltrOffset
                },
                onDrawSurface = {
                    val p = state.pressProgress.value
                    drawRect(Color.White.copy(alpha = 1f - p))
                },
            ),
    )
}
