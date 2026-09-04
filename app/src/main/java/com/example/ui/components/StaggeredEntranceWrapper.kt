package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Shared staggered entrance animation wrapper for list records.
 * Animates opacity (0f -> 1f), subtle scale (0.95f -> 1f), and downward vertical translation (48dp -> 0dp)
 * with a smooth spring motion and a staggered delay based on list item index.
 *
 * Tracks already-animated item IDs to ensure the animation plays smoothly once on initial appearance
 * and preserves stability during scroll recycling and state updates.
 */
@Composable
fun StaggeredEntranceWrapper(
    itemId: Any,
    index: Int,
    animatedItemIds: MutableSet<Any>,
    modifier: Modifier = Modifier,
    initialOffsetY: Float = 48f,
    initialScale: Float = 0.95f,
    staggerDelayMillis: Long = 45L,
    maxStaggerIndex: Int = 12,
    dampingRatio: Float = 0.78f,
    stiffness: Float = 260f,
    content: @Composable () -> Unit
) {
    val isAlreadyAnimated = remember(itemId) { itemId in animatedItemIds }

    if (isAlreadyAnimated) {
        Box(modifier = modifier) {
            content()
        }
    } else {
        val alphaAnim = remember { Animatable(0f) }
        val slideAnim = remember { Animatable(initialOffsetY) }
        val scaleAnim = remember { Animatable(initialScale) }

        LaunchedEffect(itemId) {
            val cappedIndex = index.coerceAtMost(maxStaggerIndex)
            val delayMillis = cappedIndex * staggerDelayMillis
            if (delayMillis > 0) {
                delay(delayMillis)
            }
            animatedItemIds.add(itemId)

            launch {
                alphaAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 320,
                        easing = FastOutSlowInEasing
                    )
                )
            }
            launch {
                scaleAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = dampingRatio,
                        stiffness = stiffness
                    )
                )
            }
            slideAnim.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = dampingRatio,
                    stiffness = stiffness
                )
            )
        }

        Box(
            modifier = modifier.graphicsLayer {
                translationY = slideAnim.value.dp.toPx()
                alpha = alphaAnim.value
                scaleX = scaleAnim.value
                scaleY = scaleAnim.value
            }
        ) {
            content()
        }
    }
}

