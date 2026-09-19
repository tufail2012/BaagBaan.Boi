package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Indication
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import kotlinx.coroutines.launch

/**
 * App-wide press feedback (Apple HIG style): every Button, IconButton, and
 * clickable that doesn't set its own indication scales down instantly on
 * press (not on release), critically damped, no bounce. Set once via
 * LocalIndication at the app root.
 */
private class ScalePressIndicationNode(
    private val interactionSource: InteractionSource
) : Modifier.Node(), DrawModifierNode {
    private val animatedScale = Animatable(1f)

    override fun onAttach() {
        coroutineScope.launch {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> {
                        animatedScale.animateTo(
                            0.96f,
                            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 700f)
                        )
                    }
                    is PressInteraction.Release, is PressInteraction.Cancel -> {
                        animatedScale.animateTo(
                            1f,
                            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 700f)
                        )
                    }
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        scale(animatedScale.value) {
            this@draw.drawContent()
        }
    }
}

object ScalePressIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode {
        return ScalePressIndicationNode(interactionSource)
    }

    override fun equals(other: Any?) = other === this
    override fun hashCode(): Int = -1
}
