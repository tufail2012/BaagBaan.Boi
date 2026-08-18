package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Shared staggered entrance animation wrapper for list records.
 * Animates opacity (0f -> 1f) and downward vertical translation (40dp -> 0dp)
 * with a staggered delay based on list item index, capped at 10 items (400ms max delay).
 *
 * Tracks already-animated item IDs to ensure the animation only plays once on initial appearance
 * and never replays during scroll recycling.
 */
@Composable
fun StaggeredEntranceWrapper(
    itemId: Any,
    index: Int,
    animatedItemIds: MutableSet<Any>,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val alreadyAnimated = itemId in animatedItemIds
    val density = LocalDensity.current

    if (alreadyAnimated) {
        Box(modifier = modifier) {
            content()
        }
    } else {
        val alphaAnim = remember { Animatable(0f) }
        val slideAnim = remember { Animatable(40f) }

        LaunchedEffect(itemId) {
            val cappedIndex = index.coerceAtMost(10)
            val delayMillis = cappedIndex * 40L
            if (delayMillis > 0) {
                delay(delayMillis)
            }
            animatedItemIds.add(itemId)
            launch {
                alphaAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                )
            }
            slideAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            )
        }

        val offsetYPx = with(density) { slideAnim.value.dp.toPx() }.roundToInt()

        Box(
            modifier = modifier
                .offset { IntOffset(0, offsetYPx) }
                .alpha(alphaAnim.value)
        ) {
            content()
        }
    }
}
