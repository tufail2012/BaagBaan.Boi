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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

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

private val FADE_RUN = 32.dp
private const val PEAK = 0.75f
private const val SCRIM_PEAK = 0.42f
private const val SCRIM_STOPS = 12

/**
 * Reusable Global Top Header Scroll Scrim.
 *
 * Behavior:
 * - At scroll offset = 0: NO visible blur, NO shade, and NO tint (clean & transparent).
 * - When content starts scrolling underneath the header: the blur/fade gradually and smoothly appears.
 * - Purely neutral: no accent color tint, uses HazeMaterials.ultraThin(pageColor).
 * - Progressive vertical gradient without hard horizontal lines or opaque bounding cards.
 * - Preserves complete sharpness and readability of header controls.
 */
@OptIn(ExperimentalHazeApi::class, ExperimentalHazeMaterialsApi::class)
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
    val topBarHeightEquivalent = customHeight ?: (statusBarTop + 54.dp)
    val height = topBarHeightEquivalent + FADE_RUN

    val effectiveHazeState = hazeState ?: LocalAppGlassHazeState.current

    val themeBgColor = MaterialTheme.colorScheme.background
    val pageColor = remember(isDark, isAmoled, themeBgColor) {
        when {
            isAmoled -> Color.Black
            isDark -> Color(0xFF0F172A)
            else -> themeBgColor
        }
    }
    val scrimColor = pageColor

    // Dynamic scroll progress strictly driven by the actual scroll offset:
    // 0f when at rest (offset <= 0), smoothly ramping up to 1f over the first 80px of scroll.
    val progress by remember(scrollOffset, scrollOffsetProvider) {
        derivedStateOf {
            val offset = scrollOffsetProvider?.invoke() ?: scrollOffset
            (offset / 80f).coerceIn(0f, 1f)
        }
    }

    val colorStops = remember(scrimColor) {
        Array(SCRIM_STOPS) { i ->
            val t = i / (SCRIM_STOPS - 1f)
            t to scrimColor.copy(
                alpha = SCRIM_PEAK * (1f - EaseOutCubic.transform(t))
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer {
                alpha = progress
            }
            .then(
                if (effectiveHazeState != null && progress > 0.001f) {
                    Modifier.hazeEffect(
                        state = effectiveHazeState,
                        style = HazeMaterials.ultraThin(pageColor)
                    ) {
                        inputScale = HazeInputScale.Fixed(0.33f)
                        progressive = HazeProgressive.verticalGradient(
                            easing = EaseOutCubic,
                            startIntensity = PEAK,
                            endIntensity = 0f,
                        )
                    }
                } else {
                    Modifier
                }
            )
            .drawWithCache {
                val scrimBrush = Brush.verticalGradient(colorStops = *colorStops)
                onDrawWithContent {
                    drawContent()
                    if (progress > 0.001f) {
                        drawRect(brush = scrimBrush)
                    }
                }
            }
    )
}
