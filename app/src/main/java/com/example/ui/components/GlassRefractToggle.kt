package com.example.ui.components

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.example.ui.animation.IosMotion
import com.example.ui.haptics.Haptic
import com.example.ui.haptics.rememberHaptics
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import kotlin.math.abs
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private val TOUCH_WIDTH = 64.dp
private val TOUCH_HEIGHT = 48.dp
private val TRACK_WIDTH = 64.dp
private val TRACK_HEIGHT = 28.dp
private val THUMB_WIDTH = 40.dp
private val THUMB_HEIGHT = 24.dp
private val THUMB_START_X = 2.dp
private val THUMB_END_X = 22.dp
private val DRAG_WIDTH = 20.dp

@Composable
fun GlassRefractToggle(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    description: String? = null,
    activeColor: Color = Color(0xFF34C759),
    inactiveColor: Color = Color(0xFF787880).copy(alpha = 0.36f),
) {
    if (!isGlassSupported()) {
        GlassToggle(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            enabled = enabled,
            description = description,
            activeColor = activeColor,
            inactiveColor = inactiveColor,
        )
        return
    }

    val context = LocalContext.current
    val reduceMotion = remember(context) { IosMotion.isReducedMotion(context) }
    val density = LocalDensity.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val dragWidthPx = with(density) { DRAG_WIDTH.toPx() }
    val scope = rememberCoroutineScope()
    val haptics = rememberHaptics()
    val interactive = enabled && onCheckedChange != null

    val currentChecked by rememberUpdatedState(checked)
    val currentOnChange by rememberUpdatedState(onCheckedChange)

    val valueSpec: AnimationSpec<Float> = remember(reduceMotion) {
        if (reduceMotion) snap()
        else spring(dampingRatio = 1.0f, stiffness = 1000f, visibilityThreshold = 0.001f)
    }
    val pressSpec: AnimationSpec<Float> = remember(reduceMotion) {
        if (reduceMotion) snap()
        else spring(dampingRatio = 1.0f, stiffness = 1000f, visibilityThreshold = 0.001f)
    }
    val velocitySpec: AnimationSpec<Float> = remember(reduceMotion) {
        if (reduceMotion) snap()
        else spring(dampingRatio = 0.5f, stiffness = 300f, visibilityThreshold = 0.01f)
    }
    val scaleXSpec: AnimationSpec<Float> = remember(reduceMotion) {
        if (reduceMotion) snap()
        else spring(dampingRatio = 0.6f, stiffness = 250f, visibilityThreshold = 0.001f)
    }
    val scaleYSpec: AnimationSpec<Float> = remember(reduceMotion) {
        if (reduceMotion) snap()
        else spring(dampingRatio = 0.7f, stiffness = 250f, visibilityThreshold = 0.001f)
    }

    val value = remember { Animatable(if (checked) 1f else 0f) }
    val pressProgress = remember { Animatable(0f) }
    val velocity = remember { Animatable(0f) }
    val scaleX = remember { Animatable(1f) }
    val scaleY = remember { Animatable(1f) }

    var dragging by remember { mutableStateOf(false) }
    var releaseJob by remember { mutableStateOf<Job?>(null) }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    fun startPress() {
        releaseJob?.cancel()
        releaseJob = null
        scope.launch { pressProgress.animateTo(1f, pressSpec) }
        scope.launch { scaleX.animateTo(1.5f, scaleXSpec) }
        scope.launch { scaleY.animateTo(1.5f, scaleYSpec) }
    }

    fun finishRelease(targetValue: Float) {
        releaseJob?.cancel()
        releaseJob = scope.launch {
            launch { value.animateTo(targetValue, valueSpec) }
            withTimeoutOrNull(400L) {
                snapshotFlow { abs(value.value - targetValue) <= 0.025f }
                    .first { it }
            }
            launch { pressProgress.animateTo(0f, pressSpec) }
            launch { scaleX.animateTo(1f, scaleXSpec) }
            launch { scaleY.animateTo(1f, scaleYSpec) }
        }
    }

    // Only animate to the checked value when no release is running.
    // Otherwise the release animation and this one fight over value.
    LaunchedEffect(checked, dragging) {
        if (!dragging && releaseJob?.isActive != true) {
            value.animateTo(if (checked) 1f else 0f, valueSpec)
        }
    }

    LaunchedEffect(isPressed, dragging) {
        if (interactive && (isPressed || dragging)) {
            startPress()
        } else if (!isPressed && !dragging && releaseJob == null && pressProgress.value > 0f) {
            finishRelease(if (currentChecked) 1f else 0f)
        }
    }

    val capsuleShape = remember { RoundedCornerShape(percent = 50) }
    val trackBackdrop = rememberLayerBackdrop()
    val thumbBackdrop = rememberBackdrop(trackBackdrop) { drawBackdrop ->
        val p = pressProgress.value
        if (p != 0f) {
            scale(
                scaleX = lerp(2f / 3f, 0.75f, p),
                scaleY = lerp(0f, 0.75f, p),
            ) {
                drawBackdrop()
            }
        }
    }

    Box(
        modifier = modifier
            .size(width = TOUCH_WIDTH, height = TOUCH_HEIGHT)
            .alpha(if (enabled) 1f else 0.4f)
            .semantics {
                if (description != null) this.contentDescription = description
            }
            .toggleable(
                value = checked,
                interactionSource = interactionSource,
                indication = null,
                enabled = interactive,
                role = Role.Switch,
                onValueChange = { newValue ->
                    startPress()
                    val targetFloat = if (newValue) 1f else 0f
                    if (newValue != currentChecked) {
                        haptics.play(Haptic.Select)
                        currentOnChange?.invoke(newValue)
                    }
                    finishRelease(targetFloat)
                },
            )
            .pointerInput(interactive, dragWidthPx, isRtl, reduceMotion) {
                if (!interactive || dragWidthPx <= 0f) return@pointerInput
                val velocityTracker = VelocityTracker()
                val direction = if (isRtl) -1f else 1f
                detectHorizontalDragGestures(
                    onDragStart = {
                        velocityTracker.resetTracking()
                        dragging = true
                        startPress()
                    },
                    onDragCancel = {
                        dragging = false
                        scope.launch { velocity.animateTo(0f, velocitySpec) }
                        finishRelease(if (currentChecked) 1f else 0f)
                    },
                    onDragEnd = {
                        dragging = false
                        scope.launch { velocity.animateTo(0f, velocitySpec) }
                        val targetBool = value.value >= 0.5f
                        val targetFloat = if (targetBool) 1f else 0f
                        if (targetBool != currentChecked) {
                            haptics.play(Haptic.Select)
                            currentOnChange?.invoke(targetBool)
                        }
                        finishRelease(targetFloat)
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                        val vx = velocityTracker.calculateVelocity().x * direction
                        val nextValue = (value.value + (dragAmount * direction) / dragWidthPx).coerceIn(0f, 1f)
                        scope.launch {
                            value.snapTo(nextValue)
                        }
                        if (!reduceMotion) {
                            scope.launch {
                                velocity.animateTo(vx, velocitySpec)
                            }
                        }
                    },
                )
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(width = TRACK_WIDTH, height = TRACK_HEIGHT)
                .layerBackdrop(trackBackdrop)
                .clip(capsuleShape)
                .drawBehind {
                    val fraction = value.value.coerceIn(0f, 1f)
                    drawRect(color = lerp(inactiveColor, activeColor, fraction))
                },
        )

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(width = THUMB_WIDTH, height = THUMB_HEIGHT)
                .drawBackdrop(
                    backdrop = thumbBackdrop,
                    shape = { capsuleShape },
                    effects = {
                        val p = pressProgress.value
                        if (p != 0f) {
                            blur(8f.dp.toPx() * (1f - p))
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                lens(
                                    refractionHeight = 5f.dp.toPx() * p,
                                    refractionAmount = 10f.dp.toPx() * p,
                                    depthEffect = false,
                                    chromaticAberration = true,
                                )
                            }
                        }
                    },
                    highlight = {
                        val p = pressProgress.value
                        Highlight.Ambient.copy(
                            width = Highlight.Ambient.width / 1.5f,
                            blurRadius = Highlight.Ambient.blurRadius / 1.5f,
                            alpha = p,
                        )
                    },
                    shadow = {
                        Shadow(
                            radius = 4.dp,
                            offset = DpOffset(0.dp, 4.dp / 6f),
                            color = Color.Black.copy(alpha = 0.05f),
                        )
                    },
                    innerShadow = {
                        val p = pressProgress.value
                        InnerShadow(
                            radius = 4.dp * p,
                            alpha = p,
                        )
                    },
                    layerBlock = {
                        val v = if (reduceMotion) 0f else velocity.value / 50f
                        val sx = scaleX.value / (1f - (v * 0.75f).coerceIn(-0.2f, 0.2f))
                        val sy = scaleY.value * (1f - (v * 0.25f).coerceIn(-0.2f, 0.2f))
                        this.scaleX = sx
                        this.scaleY = sy
                        this.translationX = if (isRtl) {
                            lerp(-THUMB_START_X.toPx(), -THUMB_END_X.toPx(), value.value)
                        } else {
                            lerp(THUMB_START_X.toPx(), THUMB_END_X.toPx(), value.value)
                        }
                    },
                    onDrawSurface = {
                        val p = pressProgress.value
                        drawRect(Color.White.copy(alpha = 1f - p))
                    },
                ),
        )
    }
}
