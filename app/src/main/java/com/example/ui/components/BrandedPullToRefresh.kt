package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.R
import com.example.ui.AppThemeMode
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Branded Pull-to-Refresh container featuring the Baagbaan Boi Apple/Leaf brand icon.
 *
 * Behaviors:
 * - Apple/Leaf icon gradually appears & scales with pull distance
 * - Rotates as the user pulls down
 * - Haptic click triggered when threshold is crossed
 * - Smooth continuous spinning while refresh operation is active
 * - Smoothly glides back up upon completion without abrupt jumps
 * - Respects Light, Dark, AMOLED themes and dynamic accent colors
 */
@Composable
fun BrandedPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    themeMode: AppThemeMode? = null,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val refreshThresholdPx = with(density) { 76.dp.toPx() }
    val maxPullDistancePx = with(density) { 130.dp.toPx() }
    val holdingOffsetPx = with(density) { 56.dp.toPx() }

    val pullOffset = remember { Animatable(0f) }
    var hasTriggeredHaptic by remember { mutableStateOf(false) }

    // Synchronize holding state with external isRefreshing boolean
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            if (pullOffset.value < holdingOffsetPx) {
                pullOffset.animateTo(
                    targetValue = holdingOffsetPx,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
        } else {
            hasTriggeredHaptic = false
            pullOffset.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
            )
        }
    }

    val nestedScrollConnection = remember(enabled, isRefreshing, refreshThresholdPx, maxPullDistancePx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!enabled || isRefreshing) return Offset.Zero

                // When user scrolls up while pull indicator is visible, collapse it first
                if (available.y < 0 && pullOffset.value > 0f) {
                    val consumed = available.y
                    val newOffset = (pullOffset.value + consumed).coerceAtLeast(0f)
                    coroutineScope.launch {
                        pullOffset.snapTo(newOffset)
                    }
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (!enabled || isRefreshing) return Offset.Zero

                // When user pulls down at top of list
                if (available.y > 0) {
                    val dragMultiplier = 0.48f * (1f - (pullOffset.value / maxPullDistancePx).coerceIn(0f, 0.85f))
                    val delta = available.y * dragMultiplier
                    val newOffset = (pullOffset.value + delta).coerceAtMost(maxPullDistancePx)

                    if (newOffset >= refreshThresholdPx && !hasTriggeredHaptic) {
                        hasTriggeredHaptic = true
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    } else if (newOffset < refreshThresholdPx && hasTriggeredHaptic) {
                        hasTriggeredHaptic = false
                    }

                    coroutineScope.launch {
                        pullOffset.snapTo(newOffset)
                    }
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (!enabled || isRefreshing) return Velocity.Zero

                if (pullOffset.value >= refreshThresholdPx) {
                    onRefresh()
                    pullOffset.animateTo(
                        targetValue = holdingOffsetPx,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                } else if (pullOffset.value > 0f) {
                    hasTriggeredHaptic = false
                    pullOffset.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                }
                return Velocity.Zero
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) {
        // Main Screen Content
        content()

        // Branded Apple/Leaf Refresh Indicator
        if (pullOffset.value > 0f || isRefreshing) {
            val pullFraction = (pullOffset.value / refreshThresholdPx).coerceIn(0f, 1.5f)
            val indicatorAlpha = (pullFraction * 1.6f).coerceIn(0f, 1f)
            val indicatorScale = (0.4f + pullFraction * 0.6f).coerceIn(0.4f, 1.15f)

            val infiniteTransition = rememberInfiniteTransition(label = "apple_spin")
            val spinRotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 900, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "apple_continuous_spin"
            )

            val effectiveRotation = if (isRefreshing) {
                spinRotation
            } else {
                pullFraction * 180f
            }

            val indicatorYOffset = with(density) {
                (pullOffset.value * 0.72f - 44.dp.toPx()).coerceAtLeast(8.dp.toPx()).roundToInt()
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(100f)
                    .offset { IntOffset(x = 0, y = indicatorYOffset) }
                    .alpha(indicatorAlpha)
                    .scale(indicatorScale)
            ) {
                BrandedRefreshIndicatorPill(
                    rotationDegrees = effectiveRotation,
                    isRefreshing = isRefreshing,
                    themeMode = themeMode
                )
            }
        }
    }
}

/**
 * Visual circular pill container displaying the official Apple/Leaf brand asset.
 */
@Composable
private fun BrandedRefreshIndicatorPill(
    rotationDegrees: Float,
    isRefreshing: Boolean,
    themeMode: AppThemeMode?,
    modifier: Modifier = Modifier
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK, AppThemeMode.AMOLED -> true
        null -> MaterialTheme.colorScheme.background.red < 0.25f
    }
    val isAmoled = themeMode == AppThemeMode.AMOLED ||
            (isDark && (MaterialTheme.colorScheme.background == Color(0xFF000000) || MaterialTheme.colorScheme.surface == Color(0xFF000000)))

    val backgroundColor = when {
        isAmoled -> Color(0xFF0D0D0D)
        isDark -> Color(0xFF1E1E1E)
        else -> Color.White
    }

    val borderColor = when {
        isAmoled -> Color(0xFF2E2E2E)
        isDark -> MaterialTheme.colorScheme.outline.copy(alpha = 0.40f)
        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
    }

    Surface(
        modifier = modifier
            .size(44.dp)
            .shadow(
                elevation = if (isDark) 0.dp else 4.dp,
                shape = CircleShape,
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)
            ),
        shape = CircleShape,
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 2.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(7.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_apple_logo),
                contentDescription = if (isRefreshing) "Refreshing..." else "Pull to refresh",
                modifier = Modifier
                    .size(28.dp)
                    .rotate(rotationDegrees)
            )
        }
    }
}
