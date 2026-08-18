package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.ui.AppThemeMode

/**
 * High-performance, lightweight Branded Pull-to-Refresh container.
 *
 * Utilizes Material 3's native PullToRefreshBox with a clean, floating Eco/Leaf indicator.
 * All translations, scale, and rotations are decoupled from the composition phase and driven
 * directly via draw-phase `Modifier.graphicsLayer` for stutter-free 60/120fps performance.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrandedPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    themeMode: AppThemeMode? = null,
    content: @Composable () -> Unit
) {
    if (!enabled) {
        Box(modifier = modifier) {
            content()
        }
        return
    }

    val state = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier,
        state = state,
        indicator = {
            FloatingLeafRefreshIndicator(
                state = state,
                isRefreshing = isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    ) {
        content()
    }
}

/**
 * Clean floating leaf indicator without background container or border shape.
 * Evaluates live progress in draw phase to guarantee zero recomposition overhead.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FloatingLeafRefreshIndicator(
    state: PullToRefreshState,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "leaf_spin_transition")
    val spinningAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "leaf_rotation_angle"
    )

    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .padding(top = 12.dp)
            .graphicsLayer {
                val fraction = state.distanceFraction.coerceIn(0f, 2f)
                val isVisible = isRefreshing || fraction > 0.05f

                alpha = if (!isVisible) {
                    0f
                } else if (isRefreshing) {
                    1f
                } else {
                    (fraction * 1.6f).coerceIn(0f, 1f)
                }

                val scale = if (isRefreshing) {
                    1.1f
                } else {
                    (0.35f + fraction * 0.75f).coerceIn(0.35f, 1.2f)
                }
                scaleX = scale
                scaleY = scale

                rotationZ = if (isRefreshing) {
                    spinningAngle
                } else {
                    fraction * 220f
                }

                translationY = if (isRefreshing) {
                    20.dp.toPx()
                } else {
                    (fraction * 32.dp.toPx()).coerceAtMost(56.dp.toPx())
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Eco,
            contentDescription = if (isRefreshing) "Refreshing..." else "Pull down to refresh",
            tint = primaryColor,
            modifier = Modifier.size(34.dp)
        )
    }
}
