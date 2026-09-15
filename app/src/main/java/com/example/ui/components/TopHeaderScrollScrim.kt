package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

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
 * Renders behind the floating top header across the screen width:
 * 1. Provides a smooth, progressive vertical gradient fade from the background tint at the top
 *    down to transparent, eliminating any hard horizontal bottom line or opaque card bounding box.
 * 2. Uses real-time backdrop blur (via HazeState) to softly diffuse any scrollable content passing
 *    behind the header area, keeping header controls, titles, and icons 100% readable.
 * 3. Uses [drawWithCache] to ensure zero per-pixel recomposition, zero allocations during scrolling,
 *    and rock-solid visual stability when scrolling pauses.
 */
@Composable
fun TopHeaderScrollScrim(
    modifier: Modifier = Modifier,
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

    val hazeStyle = remember(isDark, isAmoled, accentColor) {
        HazeStyle(
            backgroundColor = Color.Transparent,
            blurRadius = 14.dp,
            tints = listOf(
                HazeTint(
                    color = (accentColor ?: Color.Transparent).copy(
                        alpha = if (isAmoled) 0.08f else if (isDark) 0.10f else 0.05f
                    )
                )
            ),
            noiseFactor = 0f
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(totalHeight)
            .then(
                if (effectiveHazeState != null) {
                    Modifier.hazeEffect(state = effectiveHazeState, style = hazeStyle)
                } else {
                    Modifier
                }
            )
            .drawWithCache {
                // Progressive cubic-ease gradient fading out downwards to completely transparent
                val scrimBrush = Brush.verticalGradient(
                    0.00f to baseColor.copy(alpha = if (isAmoled) 0.95f else if (isDark) 0.92f else 0.88f),
                    0.35f to baseColor.copy(alpha = if (isAmoled) 0.86f else if (isDark) 0.82f else 0.76f),
                    0.65f to baseColor.copy(alpha = if (isAmoled) 0.55f else if (isDark) 0.50f else 0.45f),
                    0.85f to baseColor.copy(alpha = if (isAmoled) 0.22f else if (isDark) 0.20f else 0.16f),
                    1.00f to Color.Transparent
                )
                onDrawWithContent {
                    drawContent()
                    drawRect(brush = scrimBrush)
                }
            }
    )
}
