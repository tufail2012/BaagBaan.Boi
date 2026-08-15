package com.example.ui.components.security

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.hypot

data class PatternDot(val row: Int, val col: Int) {
    val id: Int get() = row * 3 + col
}

@Composable
fun PatternLockView(
    onPatternCompleted: (List<Int>) -> Unit,
    isError: Boolean = false,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val selectedDots = remember { mutableStateListOf<PatternDot>() }
    var currentTouchPosition by remember { mutableStateOf<Offset?>(null) }
    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    val dotColor = MaterialTheme.colorScheme.outlineVariant
    val activeColor = if (isError) errorColor else primaryColor

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedVisibility(visible = !errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage ?: "",
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        Box(
            modifier = Modifier
                .size(280.dp)
                .testTag("pattern_lock_canvas")
                .pointerInput(isError) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            selectedDots.clear()
                            val dot = findClosestDot(offset, size.width.toFloat(), size.height.toFloat())
                            if (dot != null) {
                                selectedDots.add(dot)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            currentTouchPosition = offset
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val offset = change.position
                            currentTouchPosition = offset
                            val dot = findClosestDot(offset, size.width.toFloat(), size.height.toFloat())
                            if (dot != null && !selectedDots.contains(dot)) {
                                selectedDots.add(dot)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        },
                        onDragEnd = {
                            currentTouchPosition = null
                            if (selectedDots.isNotEmpty()) {
                                onPatternCompleted(selectedDots.map { it.id })
                            }
                        },
                        onDragCancel = {
                            currentTouchPosition = null
                            selectedDots.clear()
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.size(280.dp)) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val dotRadius = 10.dp.toPx()
                val activeDotRadius = 14.dp.toPx()
                val haloRadius = 26.dp.toPx()
                val lineWidth = 4.dp.toPx()

                fun getDotCenter(dot: PatternDot): Offset {
                    val x = (dot.col + 0.5f) * (canvasWidth / 3f)
                    val y = (dot.row + 0.5f) * (canvasHeight / 3f)
                    return Offset(x, y)
                }

                // Draw connecting lines between selected dots
                for (i in 0 until selectedDots.size - 1) {
                    val start = getDotCenter(selectedDots[i])
                    val end = getDotCenter(selectedDots[i + 1])
                    drawLine(
                        color = activeColor,
                        start = start,
                        end = end,
                        strokeWidth = lineWidth,
                        cap = StrokeCap.Round
                    )
                }

                // Draw connecting line to current touch point
                currentTouchPosition?.let { touchPos ->
                    if (selectedDots.isNotEmpty()) {
                        val lastDotCenter = getDotCenter(selectedDots.last())
                        drawLine(
                            color = activeColor.copy(alpha = 0.7f),
                            start = lastDotCenter,
                            end = touchPos,
                            strokeWidth = lineWidth,
                            cap = StrokeCap.Round
                        )
                    }
                }

                // Draw 3x3 Dots
                for (row in 0..2) {
                    for (col in 0..2) {
                        val dot = PatternDot(row, col)
                        val center = getDotCenter(dot)
                        val isSelected = selectedDots.contains(dot)

                        if (isSelected) {
                            // Outer glowing halo
                            drawCircle(
                                color = activeColor.copy(alpha = 0.2f),
                                radius = haloRadius,
                                center = center
                            )
                            // Inner filled circle
                            drawCircle(
                                color = activeColor,
                                radius = activeDotRadius,
                                center = center
                            )
                        } else {
                            drawCircle(
                                color = dotColor,
                                radius = dotRadius,
                                center = center
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun findClosestDot(touch: Offset, width: Float, height: Float, thresholdDp: Float = 44f): PatternDot? {
    val hitRadiusPx = thresholdDp * 2.5f
    for (row in 0..2) {
        for (col in 0..2) {
            val cx = (col + 0.5f) * (width / 3f)
            val cy = (row + 0.5f) * (height / 3f)
            val distance = hypot(touch.x - cx, touch.y - cy)
            if (distance <= hitRadiusPx) {
                return PatternDot(row, col)
            }
        }
    }
    return null
}
