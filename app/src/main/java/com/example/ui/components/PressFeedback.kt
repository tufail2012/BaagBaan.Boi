package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

import androidx.compose.ui.platform.LocalContext
import com.example.ui.animation.IosMotion

/**
 * Apple-style press feedback: responds on pointer-down (not release),
 * scales down using iOS spring specs (dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium).
 */
@Composable
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.96f
): Modifier {
    val context = LocalContext.current
    val isReducedMotion = remember(context) { IosMotion.isReducedMotion(context) }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (!isReducedMotion && isPressed) pressedScale else 1f,
        animationSpec = IosMotion.PressSpring,
        label = "pressScale"
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
