package com.example.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.example.ui.animation.IosMotion
import com.example.ui.components.backdrop.backdrops.layerBackdrop
import com.example.ui.components.backdrop.backdrops.rememberLayerBackdrop
import com.example.ui.haptics.Haptic
import com.example.ui.haptics.rememberHaptics
import com.kyant.backdrop.Backdrop

private val TOUCH_WIDTH = 64.dp
private val TOUCH_HEIGHT = 48.dp
private val TRACK_WIDTH = 64.dp
private val TRACK_HEIGHT = 28.dp
private val THUMB_START_X = 2.dp
private val THUMB_END_X = 22.dp
private val DRAG_WIDTH = 20.dp
private val TOGGLE_LENS_HEIGHT = 5.dp
private val TOGGLE_LENS_AMOUNT = 10.dp
private const val TOGGLE_VELOCITY_DIVISOR = 50f
private const val TOGGLE_TRACK_SCALE_MAX = 0.75f
private const val TOGGLE_SHADOW_ALPHA = 0.05f

@Composable
fun GlassRefractToggle(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    description: String? = null,
    activeColor: Color = Color(0xFF34C759),
    inactiveColor: Color = Color(0xFF787880).copy(alpha = 0.36f),
    backdrop: Backdrop? = null,
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
    val dragWidthPx = with(density) { DRAG_WIDTH.toPx() }
    val haptics = rememberHaptics()
    val interactive = enabled && onCheckedChange != null

    val currentChecked by rememberUpdatedState(checked)
    val currentOnChange by rememberUpdatedState(onCheckedChange)

    val targetValue = if (checked) 1f else 0f
    val thumbState = rememberGlassDragThumbState(
        value = targetValue,
        reduceMotion = reduceMotion,
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    LaunchedEffect(isPressed, thumbState.dragging) {
        if (interactive && (isPressed || thumbState.dragging)) {
            thumbState.startPress()
        } else if (!isPressed && !thumbState.dragging && thumbState.releaseJob?.isActive != true && thumbState.pressProgress.value > 0f) {
            thumbState.finishRelease(if (currentChecked) 1f else 0f)
        }
    }

    val capsuleShape = remember { RoundedCornerShape(percent = 50) }
    val trackBackdrop = rememberLayerBackdrop()
    val vendoredBackdrop = remember(backdrop) { backdrop?.asVendoredBackdrop() }

    val onThumbRelease: (Float) -> Float = { releasedFraction ->
        val targetBool = releasedFraction >= 0.5f
        val finalTarget = if (targetBool) 1f else 0f
        if (targetBool != currentChecked) {
            haptics.play(if (targetBool) Haptic.ToggleOn else Haptic.ToggleOff)
            currentOnChange?.invoke(targetBool)
        }
        finalTarget
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
                    thumbState.startPress()
                    val targetFloat = if (newValue) 1f else 0f
                    if (newValue != currentChecked) {
                        haptics.play(if (newValue) Haptic.ToggleOn else Haptic.ToggleOff)
                        currentOnChange?.invoke(newValue)
                    }
                    thumbState.finishRelease(targetFloat, holdPressForTap = true)
                },
            )
            .glassThumbDragGestures(
                state = thumbState,
                enabled = interactive,
                travelPx = dragWidthPx,
                reduceMotion = reduceMotion,
                currentValue = targetValue,
                onValueChange = {},
                onRelease = onThumbRelease,
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(width = TRACK_WIDTH, height = TRACK_HEIGHT)
                .layerBackdrop(trackBackdrop)
                .clip(capsuleShape)
                .drawBehind {
                    val fraction = thumbState.animatedValue
                    drawRect(color = lerp(inactiveColor, activeColor, fraction))
                }
                .border(0.5.dp, Color.White.copy(alpha = 0.22f), capsuleShape),
        )

        GlassDragThumb(
            state = thumbState,
            thumbSize = GLASS_THUMB_SIZE,
            travelPx = dragWidthPx,
            value = targetValue,
            enabled = enabled,
            reduceMotion = reduceMotion,
            velocityDivisor = TOGGLE_VELOCITY_DIVISOR,
            backdrop = vendoredBackdrop,
            trackBackdrop = trackBackdrop,
            trackScaleMax = TOGGLE_TRACK_SCALE_MAX,
            lensHeight = TOGGLE_LENS_HEIGHT,
            lensAmount = TOGGLE_LENS_AMOUNT,
            shadowAlpha = TOGGLE_SHADOW_ALPHA,
            translationX = { fraction ->
                lerp(THUMB_START_X.toPx(), THUMB_END_X.toPx(), fraction.coerceIn(0f, 1f))
            },
            onValueChange = {},
            onRelease = onThumbRelease,
        )
    }
}
