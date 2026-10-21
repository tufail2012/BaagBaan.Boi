package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ui.animation.IosMotion

/** How far the icon lifts at the top of the bounce. */
internal val NAV_ICON_BOUNCE_LIFT = 7.dp

/**
 * Value for one nav icon's bounce. It is 0 at rest.
 * When the tab becomes selected it goes 0 to 1 (icon lifts), then springs back past 0
 * (icon dips slightly) and settles at 0.
 * Use it in graphicsLayer:
 *   translationY = -NAV_ICON_BOUNCE_LIFT.toPx() * bounce.value
 *   scaleX = iconScale + 0.12f * bounce.value
 *   scaleY = iconScale + 0.12f * bounce.value
 * It does not play on first composition, and it snaps when reduced motion is on.
 */
@Composable
fun rememberSelectionBounce(isSelected: Boolean): Animatable<Float, AnimationVector1D> {
    val context = LocalContext.current
    val reduceMotion = remember(context) { IosMotion.isReducedMotion(context) }
    val bounce = remember { Animatable(0f) }
    var skippedFirst by remember { mutableStateOf(false) }

    LaunchedEffect(isSelected) {
        if (!skippedFirst) {
            skippedFirst = true
            return@LaunchedEffect
        }
        if (!isSelected) return@LaunchedEffect
        bounce.snapTo(0f)
        if (reduceMotion) return@LaunchedEffect
        bounce.animateTo(1f, tween(durationMillis = 110, easing = FastOutSlowInEasing))
        bounce.animateTo(0f, spring(dampingRatio = 0.38f, stiffness = Spring.StiffnessMediumLow))
    }
    return bounce
}
