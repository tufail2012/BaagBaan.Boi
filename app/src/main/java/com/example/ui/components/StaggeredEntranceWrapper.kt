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

import androidx.compose.ui.platform.LocalContext
import com.example.ui.animation.IosMotion

/**
 * Shared staggered entrance animation wrapper for list records.
 * Animates opacity (0f -> 1f) and vertical translation (24dp -> 0dp)
 * with iOS spring motion (dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium)
 * and a 40ms stagger delay for the first 8 items.
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
    initialOffsetY: Float = 24f,
    initialScale: Float = 1.0f,
    staggerDelayMillis: Long = 40L,
    maxStaggerIndex: Int = 8,
    dampingRatio: Float = IosMotion.EnterDampingRatio,
    stiffness: Float = IosMotion.EnterStiffness,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val isReducedMotion = remember(context) { IosMotion.isReducedMotion(context) }
    val isAlreadyAnimated = remember(itemId) { itemId in animatedItemIds }

    if (isAlreadyAnimated || isReducedMotion || index >= maxStaggerIndex) {
        animatedItemIds.add(itemId)
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
                    animationSpec = spring(
                        dampingRatio = dampingRatio,
                        stiffness = stiffness
                    )
                )
            }
            if (initialScale != 1.0f) {
                launch {
                    scaleAnim.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = dampingRatio,
                            stiffness = stiffness
                        )
                    )
                }
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

