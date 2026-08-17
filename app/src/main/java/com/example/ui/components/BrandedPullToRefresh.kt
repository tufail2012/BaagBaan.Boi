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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.R
import com.example.ui.AppThemeMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * High-performance Branded Pull-to-Refresh container featuring the Baagbaan Boi Apple/Leaf brand icon.
 *
 * Performance & Architecture:
 * - Pure synchronous drag tracking with zero coroutine allocations during scroll movement.
 * - Draw-phase visual updates via `Modifier.graphicsLayer` to eliminate recompositions during spin and pull.
 * - Non-blocking: Child LazyColumns remain fully scrollable and responsive during active refresh.
 * - Reusable single-job spring animations with cancellation safety.
 * - Resets cleanly on early releases, cancellations, and completed/failed data requests.
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

    val refreshThresholdPx = with(density) { 72.dp.toPx() }
    val maxPullDistancePx = with(density) { 128.dp.toPx() }
    val holdingOffsetPx = with(density) { 54.dp.toPx() }

    val currentOnRefresh by rememberUpdatedState(onRefresh)
    val currentIsRefreshing by rememberUpdatedState(isRefreshing)
    val currentEnabled by rememberUpdatedState(enabled)

    var dragDistance by remember { mutableFloatStateOf(0f) }
    var isAnimating by remember { mutableStateOf(false) }
    val settleAnim = remember { Animatable(0f) }
    var settleJob by remember { mutableStateOf<Job?>(null) }
    var hasTriggeredHaptic by remember { mutableStateOf(false) }

    // Synchronize holding state with external isRefreshing boolean
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            val startVal = if (isAnimating) settleAnim.value else dragDistance
            if (startVal < holdingOffsetPx) {
                isAnimating = true
                settleJob?.cancel()
                settleJob = coroutineScope.launch {
                    try {
                        settleAnim.snapTo(startVal)
                        settleAnim.animateTo(
                            targetValue = holdingOffsetPx,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                    } finally {
                        dragDistance = holdingOffsetPx
                        isAnimating = false
                    }
                }
            } else {
                dragDistance = holdingOffsetPx
            }
        } else {
            hasTriggeredHaptic = false
            val startVal = if (isAnimating) settleAnim.value else dragDistance
            if (startVal > 0f) {
                isAnimating = true
                settleJob?.cancel()
                settleJob = coroutineScope.launch {
                    try {
                        settleAnim.snapTo(startVal)
                        settleAnim.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
                        )
                    } finally {
                        dragDistance = 0f
                        isAnimating = false
                    }
                }
            } else {
                dragDistance = 0f
                isAnimating = false
            }
        }
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!currentEnabled || currentIsRefreshing) return Offset.Zero

                val currentOffset = if (isAnimating) settleAnim.value else dragDistance

                // When user scrolls up while pull indicator is visible, collapse it first
                if (available.y < 0f && currentOffset > 0f) {
                    settleJob?.cancel()
                    isAnimating = false
                    val consumed = available.y
                    val newOffset = (currentOffset + consumed).coerceAtLeast(0f)
                    dragDistance = newOffset
                    if (newOffset < refreshThresholdPx) {
                        hasTriggeredHaptic = false
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
                if (!currentEnabled || currentIsRefreshing) return Offset.Zero

                // When user pulls down at top of list
                if (available.y > 0f) {
                    settleJob?.cancel()
                    isAnimating = false
                    val currentOffset = dragDistance
                    val dragMultiplier = 0.44f * (1f - (currentOffset / maxPullDistancePx).coerceIn(0f, 0.85f))
                    val delta = available.y * dragMultiplier
                    val newOffset = (currentOffset + delta).coerceAtMost(maxPullDistancePx)

                    if (newOffset >= refreshThresholdPx && !hasTriggeredHaptic) {
                        hasTriggeredHaptic = true
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    } else if (newOffset < refreshThresholdPx && hasTriggeredHaptic) {
                        hasTriggeredHaptic = false
                    }

                    dragDistance = newOffset
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (!currentEnabled || currentIsRefreshing) return Velocity.Zero

                val currentOffset = if (isAnimating) settleAnim.value else dragDistance

                if (currentOffset >= refreshThresholdPx) {
                    // Trigger refresh without blocking
                    currentOnRefresh()
                    isAnimating = true
                    settleJob?.cancel()
                    settleJob = coroutineScope.launch {
                        try {
                            settleAnim.snapTo(currentOffset)
                            settleAnim.animateTo(
                                targetValue = holdingOffsetPx,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )
                        } finally {
                            dragDistance = holdingOffsetPx
                            isAnimating = false
                        }
                    }
                } else if (currentOffset > 0f) {
                    hasTriggeredHaptic = false
                    isAnimating = true
                    settleJob?.cancel()
                    settleJob = coroutineScope.launch {
                        try {
                            settleAnim.snapTo(currentOffset)
                            settleAnim.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                        } finally {
                            dragDistance = 0f
                            isAnimating = false
                        }
                    }
                }
                return Velocity.Zero
            }
        }
    }

    // High performance infinite rotation transition active during refresh
    val infiniteTransition = rememberInfiniteTransition(label = "apple_spin_transition")
    val spinRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "apple_spin_angle"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) {
        // 1. Main Screen Content (remains fully scrollable and responsive)
        content()

        // 2. Branded Apple/Leaf Refresh Indicator
        val visibleOffset = if (isAnimating) settleAnim.value else dragDistance
        if (visibleOffset > 0.5f || isRefreshing) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(100f)
                    .graphicsLayer {
                        val offset = if (isAnimating) settleAnim.value else dragDistance
                        val pullFraction = (offset / refreshThresholdPx).coerceIn(0f, 1.5f)
                        val indicatorAlpha = (pullFraction * 1.6f).coerceIn(0f, 1f)
                        val indicatorScale = (0.4f + pullFraction * 0.6f).coerceIn(0.4f, 1.15f)
                        val indicatorYOffset = (offset * 0.72f - 44.dp.toPx()).coerceAtLeast(8.dp.toPx())

                        translationY = indicatorYOffset
                        alpha = indicatorAlpha
                        scaleX = indicatorScale
                        scaleY = indicatorScale
                    }
            ) {
                BrandedRefreshIndicatorPill(
                    rotationDegreesProvider = {
                        if (isRefreshing) {
                            spinRotation
                        } else {
                            val offset = if (isAnimating) settleAnim.value else dragDistance
                            (offset / refreshThresholdPx).coerceIn(0f, 1.5f) * 180f
                        }
                    },
                    isRefreshing = isRefreshing,
                    themeMode = themeMode
                )
            }
        }
    }
}

/**
 * Visual circular pill container displaying the official Apple/Leaf brand asset.
 * Uses draw-phase graphicsLayer rotation to guarantee 0 recompositions during continuous spin.
 */
@Composable
private fun BrandedRefreshIndicatorPill(
    rotationDegreesProvider: () -> Float,
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
                    .graphicsLayer {
                        rotationZ = rotationDegreesProvider()
                    }
            )
        }
    }
}

