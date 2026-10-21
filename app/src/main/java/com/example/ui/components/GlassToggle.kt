package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val TOUCH_WIDTH = 56.dp
private val TOUCH_HEIGHT = 48.dp
private val TRACK_WIDTH = 56.dp
private val TRACK_HEIGHT = 32.dp
private val THUMB_SIZE = 26.dp
private val THUMB_PADDING = 3.dp
private val TRAVEL = TRACK_WIDTH - THUMB_SIZE - THUMB_PADDING * 2f

/**
 * Animated toggle with a glass look: gradient track, top highlight, rim and a lifted thumb.
 * It does not sample or blur the content behind it, so it has no backdrop crash path.
 *
 * checked: current state.
 * onCheckedChange: null means read-only. The toggle then ignores taps and drags.
 * description: accessibility label, usually the row title.
 * activeColor: track color when on. Pass the screen accent.
 */
@Composable
fun GlassToggle(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    description: String? = null,
    activeColor: Color = Color(0xFFD32F2F),
    inactiveColor: Color = Color(0xFF8E8E93).copy(alpha = 0.45f),
) {
    val density = LocalDensity.current
    val travelPx = with(density) { TRAVEL.toPx() }
    val padPx = with(density) { THUMB_PADDING.toPx() }
    val scope = rememberCoroutineScope()
    val interactive = enabled && onCheckedChange != null

    val currentChecked by rememberUpdatedState(checked)
    val currentOnChange by rememberUpdatedState(onCheckedChange)

    // 0f = off, 1f = on. Drag moves this directly.
    val progress = remember { Animatable(if (checked) 1f else 0f) }
    var dragging by remember { mutableStateOf(false) }

    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()

    // Follow the checked value with a spring, but not while the thumb is being dragged.
    // If the parent rejects a change, checked stays the same and the thumb springs back.
    LaunchedEffect(checked, dragging) {
        if (!dragging) {
            progress.animateTo(
                targetValue = if (checked) 1f else 0f,
                animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow),
            )
        }
    }

    val pressAmount by animateFloatAsState(
        targetValue = if (interactive && (isPressed || dragging)) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "glassTogglePress",
    )

    Box(
        modifier = modifier
            .size(width = TOUCH_WIDTH, height = TOUCH_HEIGHT)
            .alpha(if (enabled) 1f else 0.4f)
            .semantics {
                if (description != null) this.contentDescription = description
            }
            .toggleable(
                value = checked,
                interactionSource = interaction,
                indication = null,
                enabled = interactive,
                role = Role.Switch,
                onValueChange = { newValue -> currentOnChange?.invoke(newValue) },
            )
            .pointerInput(interactive, travelPx) {
                if (!interactive) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { dragging = true },
                    onDragCancel = { dragging = false },
                    onDragEnd = {
                        dragging = false
                        val target = progress.value >= 0.5f
                        if (target != currentChecked) currentOnChange?.invoke(target)
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            progress.snapTo((progress.value + dragAmount / travelPx).coerceIn(0f, 1f))
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = TRACK_WIDTH, height = TRACK_HEIGHT)
                .drawBehind {
                    val radius = CornerRadius(size.height / 2f)
                    val fill = lerp(inactiveColor, activeColor, progress.value)

                    // Base fill
                    drawRoundRect(color = fill, cornerRadius = radius)

                    // Glass highlight across the top half
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.White.copy(alpha = 0.30f), Color.Transparent),
                            startY = 0f,
                            endY = size.height * 0.75f,
                        ),
                        cornerRadius = radius,
                    )

                    // Rim
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.28f),
                        cornerRadius = radius,
                        style = Stroke(width = 1.dp.toPx()),
                    )
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .offset { IntOffset((padPx + travelPx * progress.value).roundToInt(), 0) }
                    .size(THUMB_SIZE)
                    .graphicsLayer {
                        scaleX = 1f + 0.16f * pressAmount
                        scaleY = 1f - 0.06f * pressAmount
                    }
                    .shadow(elevation = 4.dp, shape = CircleShape)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.White, Color(0xFFEDEDF0)),
                        ),
                        shape = CircleShape,
                    )
                    .border(width = 0.6.dp, color = Color.White.copy(alpha = 0.8f), shape = CircleShape),
            )
        }
    }
}
