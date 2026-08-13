package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object BookingConfirmationState {
    var visible by mutableStateOf(false)
        private set

    fun show() {
        visible = true
    }

    fun hide() {
        visible = false
    }
}

@Composable
fun BookingConfirmationOverlay(
    modifier: Modifier = Modifier
) {
    if (!BookingConfirmationState.visible) return

    // Block back button while visible
    BackHandler(enabled = true) {}

    val scrimAlpha = remember { Animatable(0f) }
    val badgeScale = remember { Animatable(0f) }
    val checkProgress = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Step 1: Scrim fade in (~180ms)
        launch {
            scrimAlpha.animateTo(0.92f, animationSpec = tween(durationMillis = 180, easing = LinearEasing))
        }

        // Step 2: Badge spring scale in (~300ms)
        launch {
            delay(50)
            badgeScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }

        // Step 3: Checkmark path drawing (~350ms)
        launch {
            delay(180)
            checkProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
            )
        }

        // Step 4: Text fade (~250ms)
        launch {
            delay(320)
            textAlpha.animateTo(1f, animationSpec = tween(durationMillis = 250))
        }

        // Total visible sequence elapsed time ~ 2000ms
        delay(1700)

        // Step 5: Fade everything out (~250ms)
        launch { textAlpha.animateTo(0f, tween(200)) }
        launch { badgeScale.animateTo(0.8f, tween(200)) }
        scrimAlpha.animateTo(0f, tween(250))

        BookingConfirmationState.hide()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = scrimAlpha.value))
            .pointerInput(Unit) {
                // Consume all touch events to prevent clicking underneath
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent()
                    }
                }
            }
            .testTag("booking_confirmation_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Circular Badge
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(badgeScale.value)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                // Animated Checkmark on Canvas
                val checkmarkColor = Color.White
                Canvas(modifier = Modifier.size(48.dp)) {
                    val w = size.width
                    val h = size.height

                    val fullPath = Path().apply {
                        moveTo(w * 0.22f, h * 0.52f)
                        lineTo(w * 0.44f, h * 0.72f)
                        lineTo(w * 0.78f, h * 0.32f)
                    }

                    val pathMeasure = PathMeasure()
                    pathMeasure.setPath(fullPath, false)

                    val animatedPath = Path()
                    pathMeasure.getSegment(
                        startDistance = 0f,
                        stopDistance = pathMeasure.length * checkProgress.value,
                        destination = animatedPath,
                        startWithMoveTo = true
                    )

                    drawPath(
                        path = animatedPath,
                        color = checkmarkColor,
                        style = Stroke(
                            width = 5.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Staggered Text
            Text(
                text = "Booking Confirmed",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = textAlpha.value)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Record saved successfully",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = textAlpha.value * 0.75f)
            )
        }
    }
}
