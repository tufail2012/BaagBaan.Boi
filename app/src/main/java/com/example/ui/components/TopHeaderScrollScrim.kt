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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

/**
 * Reusable Global Top Header Scroll Scrim.
 *
 * Behavior:
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
    val topBarHeightEquivalent = customHeight ?: (statusBarTop + 52.dp)
    val height = topBarHeightEquivalent + FADE_RUN

    val effectiveHazeState = hazeState ?: LocalAppGlassHazeState.current

    val themeBgColor = MaterialTheme.colorScheme.background
    val baseColor = remember(isDark, isAmoled, themeBgColor) {
        when {
            isAmoled -> Color.Black
            isDark -> Color(0xFF0F172A)
            else -> themeBgColor
        }
    }
    val pageColor = baseColor

    val scrim = remember(baseColor) {
        Brush.verticalGradient(
            colorStops = Array(12) { i ->
                val t = i / 11f
                t to baseColor.copy(
                    alpha = 0.42f * (1f - EaseOutCubic.transform(t))
                )
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .then(
                if (effectiveHazeState != null) {
                    Modifier.hazeEffect(
                        state = effectiveHazeState,
                        style = HazeMaterials.ultraThin(pageColor),
                    ) {
                        inputScale = HazeInputScale.Fixed(0.33f)

                        progressive = HazeProgressive.verticalGradient(
                            easing = EaseOutCubic,
                            startIntensity = 0.75f,
                            endIntensity = 0f,
                        )

                        noiseFactor = 0f
                    }
                } else {
                    Modifier
                }
            )
            .drawBehind {
                drawRect(brush = scrim)
            }
    )
}
