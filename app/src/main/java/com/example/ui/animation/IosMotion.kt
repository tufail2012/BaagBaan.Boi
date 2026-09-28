package com.example.ui.animation

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Shared iOS-style motion and animation specifications for Baagbaan Boi.
 */
object IosMotion {
    const val PressDampingRatio: Float = 0.6f
    const val PressStiffness: Float = Spring.StiffnessMedium

    val PressSpring = spring<Float>(
        dampingRatio = PressDampingRatio,
        stiffness = PressStiffness
    )

    const val EnterDampingRatio: Float = 0.6f
    const val EnterStiffness: Float = Spring.StiffnessMedium

    val EnterSpring = spring<Float>(
        dampingRatio = EnterDampingRatio,
        stiffness = EnterStiffness
    )

    val ScreenTransitionEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    const val ScreenTransitionDurationMillis: Int = 350

    val ScreenSlideSpec = tween<IntOffset>(
        durationMillis = ScreenTransitionDurationMillis,
        easing = ScreenTransitionEasing
    )

    val ScreenFadeSpec = tween<Float>(
        durationMillis = ScreenTransitionDurationMillis,
        easing = ScreenTransitionEasing
    )

    /**
     * Checks if the user or system has enabled reduced motion / disabled animations.
     */
    fun isReducedMotion(context: Context): Boolean {
        return try {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            scale == 0f
        } catch (_: Exception) {
            false
        }
    }

    /**
     * iOS forward screen transition: new screen slides in from right (100%),
     * outgoing screen slides left by 30% (parallax) and dims slightly.
     */
    fun forwardTransition(reducedMotion: Boolean = false): ContentTransform {
        if (reducedMotion) {
            return fadeIn(animationSpec = tween(0)) togetherWith fadeOut(animationSpec = tween(0))
        }
        val enter = slideInHorizontally(animationSpec = ScreenSlideSpec) { fullWidth -> fullWidth }
        val exit = slideOutHorizontally(animationSpec = ScreenSlideSpec) { fullWidth -> -(fullWidth * 0.30f).toInt() } +
                fadeOut(animationSpec = ScreenFadeSpec, targetAlpha = 0.70f)
        return (enter togetherWith exit).apply {
            targetContentZIndex = 1f
        }
    }

    /**
     * iOS back screen transition: exact reverse of forward screen transition.
     * Returning screen slides in from -30% left and brightens from dim,
     * outgoing screen slides out to right (100%).
     */
    fun backTransition(reducedMotion: Boolean = false): ContentTransform {
        if (reducedMotion) {
            return fadeIn(animationSpec = tween(0)) togetherWith fadeOut(animationSpec = tween(0))
        }
        val enter = slideInHorizontally(animationSpec = ScreenSlideSpec) { fullWidth -> -(fullWidth * 0.30f).toInt() } +
                fadeIn(animationSpec = ScreenFadeSpec, initialAlpha = 0.70f)
        val exit = slideOutHorizontally(animationSpec = ScreenSlideSpec) { fullWidth -> fullWidth }
        return (enter togetherWith exit).apply {
            targetContentZIndex = -1f
        }
    }

    /**
     * iOS crossfade transition for top-level tabs and root destinations (no slide).
     */
    fun tabCrossfade(reducedMotion: Boolean = false): ContentTransform {
        if (reducedMotion) {
            return fadeIn(animationSpec = tween(0)) togetherWith fadeOut(animationSpec = tween(0))
        }
        return fadeIn(animationSpec = tween(220, easing = ScreenTransitionEasing)) togetherWith
                fadeOut(animationSpec = tween(180, easing = ScreenTransitionEasing))
    }
}

/**
 * iOS-style spring pressable modifier.
 * Tracks pressed state via MutableInteractionSource + collectIsPressedAsState,
 * animates scale with animateFloatAsState using spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
 * applies it via graphicsLayer (scaleX/scaleY) so it causes no recomposition.
 * Sets indication = null (iOS has no ripple).
 */
@Composable
fun Modifier.iosPressable(
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    enabled: Boolean = true,
    scaleDown: Float = 0.96f,
    onClick: () -> Unit
): Modifier {
    val context = LocalContext.current
    val reducedMotion = remember(context) { IosMotion.isReducedMotion(context) }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (!reducedMotion && enabled && isPressed) scaleDown else 1f,
        animationSpec = IosMotion.PressSpring,
        label = "iosPressableScale"
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}

/**
 * Convenient overload of iosPressable with onClick as first parameter.
 */
@Composable
fun Modifier.iosPressable(
    onClick: () -> Unit,
    scaleDown: Float = 0.96f
): Modifier = iosPressable(
    interactionSource = remember { MutableInteractionSource() },
    enabled = true,
    scaleDown = scaleDown,
    onClick = onClick
)

/**
 * Modifier for components that already manage their own click handling (e.g. Button, FAB, Card)
 * to provide iOS spring press feedback.
 */
@Composable
fun Modifier.iosPressScale(
    interactionSource: InteractionSource,
    scaleDown: Float = 0.96f,
    enabled: Boolean = true
): Modifier {
    val context = LocalContext.current
    val reducedMotion = remember(context) { IosMotion.isReducedMotion(context) }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (!reducedMotion && enabled && isPressed) scaleDown else 1f,
        animationSpec = IosMotion.PressSpring,
        label = "iosPressScaleOnly"
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * iOS-style list entrance animation modifier:
 * Fade + slide-up (24dp) using IosMotion.EnterSpring,
 * staggered 40ms per item, only for the first 8 items (index < 8),
 * and only on first composition (not on every scroll).
 */
@Composable
fun Modifier.iosListEntrance(
    index: Int,
    key: Any? = null,
    animatedKeys: MutableSet<Any>? = null
): Modifier {
    val context = LocalContext.current
    val reducedMotion = remember(context) { IosMotion.isReducedMotion(context) }
    if (reducedMotion || index >= 8) return this

    val itemKey = key ?: index
    val alreadyAnimated = remember(itemKey) {
        animatedKeys?.contains(itemKey) == true
    }
    if (alreadyAnimated) return this

    val alphaAnim = remember(itemKey) { Animatable(0f) }
    val offsetAnim = remember(itemKey) { Animatable(24f) }
    val density = LocalDensity.current
    val offsetPx = with(density) { 24.dp.toPx() }

    LaunchedEffect(itemKey) {
        val delayMillis = index * 40L
        if (delayMillis > 0L) {
            delay(delayMillis)
        }
        animatedKeys?.add(itemKey)
        launch {
            alphaAnim.animateTo(
                targetValue = 1f,
                animationSpec = IosMotion.EnterSpring
            )
        }
        launch {
            offsetAnim.animateTo(
                targetValue = 0f,
                animationSpec = IosMotion.EnterSpring
            )
        }
    }

    return this.graphicsLayer {
        alpha = alphaAnim.value
        translationY = (offsetAnim.value / 24f) * offsetPx
    }
}
