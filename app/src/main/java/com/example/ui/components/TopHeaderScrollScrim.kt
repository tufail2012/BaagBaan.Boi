package com.example.ui.components

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/**
 * CompositionLocal providing scroll offset for top header scrim when available.
 */
val LocalHeaderScrollOffset = compositionLocalOf { 0f }

/**
 * Calculates dynamic top padding for scrollable containers so that in resting state,
 * the first item is positioned naturally below the floating header without extra gap,
 * while allowing content to scroll smoothly underneath the header.
 */
@Composable
fun rememberScrollUnderHeaderTopPadding(extraPadding: Dp = 68.dp): Dp {
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    return statusBarTop + extraPadding
}

/**
 * Reusable Global Top Header Scroll Scrim.
 *
 * Behavior:
 * - At scroll offset = 0: NO visible blur, NO shade, and NO tint (clean & transparent).
 * - When content starts scrolling underneath the header: the blur/fade gradually and smoothly appears.
 * - Purely neutral: no accent color tint.
 * - Progressive vertical gradient without hard horizontal lines or opaque bounding cards.
 * - Preserves complete sharpness and readability of header controls.
 */
@OptIn(ExperimentalHazeApi::class)
@Composable
fun TopHeaderScrollScrim(
    modifier: Modifier = Modifier,
    scrollOffset: Float = LocalHeaderScrollOffset.current,
    scrollOffsetProvider: (() -> Float)? = null,
    hazeState: HazeState? = LocalAppGlassHazeState.current,
    accentColor: Color? = null,
    isDark: Boolean = isAppInDarkMode(),
    isAmoled: Boolean = isAppInAmoledMode(),
    customHeight: Dp? = null
) {
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val totalHeight = customHeight ?: (statusBarTop + 86.dp)

    val effectiveHazeState = hazeState ?: LocalAppGlassHazeState.current

    val themeBgColor = MaterialTheme.colorScheme.background
    val baseColor = remember(isDark, isAmoled, themeBgColor) {
        when {
            isAmoled -> Color.Black
            isDark -> Color(0xFF0F172A)
            else -> themeBgColor
        }
    }

    // Dynamic scroll progress strictly driven by the actual scroll offset:
    // 0f when at rest (offset <= 0), smoothly ramping up to 1f over the first 100px of scroll.
    val progress by remember(scrollOffset, scrollOffsetProvider) {
        derivedStateOf {
            val offset = scrollOffsetProvider?.invoke() ?: scrollOffset
            (offset / 100f).coerceIn(0f, 1f)
        }
    }

    // Neutral blur style: absolutely NO accentColor tint, purely soft backdrop diffusion
    val hazeStyle = remember(progress) {
        val currentBlur = (12.dp * progress).coerceAtLeast(0.dp)
        HazeStyle(
            backgroundColor = Color.Transparent,
            blurRadius = currentBlur,
            tints = emptyList(),
            noiseFactor = 0f
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(totalHeight)
            .then(
                if (effectiveHazeState != null && progress > 0.01f) {
                    Modifier.hazeEffect(state = effectiveHazeState, style = hazeStyle) {
                        // Sample at 1/3 resolution and upscale — the blur hides the
                        // upscale, so full-resolution pixels here are paid for but
                        // never seen. Same trade already made by the app's other
                        // liquid-glass surfaces.
                        inputScale = HazeInputScale.Fixed(0.33f)
                        // Ramps the blur radius from full (at the top of the strip)
                        // to nothing (at its bottom edge), so the strip has no hard
                        // line where the blur stops — it fades out the same way the
                        // scrim below it already does.
                        progressive = HazeProgressive.verticalGradient(
                            easing = EaseOutCubic,
                            startIntensity = 1f,
                            endIntensity = 0f
                        )
                    }
                } else {
                    Modifier
                }
            )
            .drawWithCache {
                val topAlpha = (if (isAmoled) 0.70f else if (isDark) 0.65f else 0.55f) * progress
                val scrimBrush = Brush.verticalGradient(
                    0.00f to baseColor.copy(alpha = topAlpha),
                    0.35f to baseColor.copy(alpha = topAlpha * 0.75f),
                    0.65f to baseColor.copy(alpha = topAlpha * 0.38f),
                    0.85f to baseColor.copy(alpha = topAlpha * 0.10f),
                    1.00f to Color.Transparent
                )
                onDrawWithContent {
                    drawContent()
                    // At scroll offset 0 (progress <= 0.001f), draw absolutely nothing (100% transparent)
                    if (progress > 0.001f) {
                        drawRect(brush = scrimBrush)
                    }
                }
            }
    )
}
