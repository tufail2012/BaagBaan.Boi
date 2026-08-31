package com.example.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.glass.liquidFrostedGlass
import dev.chrisbanes.haze.HazeState

/**
 * Frosted Liquid Glass modifier for Floating Chrome, Pill Headers, FABs, and Overlays.
 * Delegates to the unified central Frosted Liquid Glass engine.
 */
@Composable
fun Modifier.frostedGlassChrome(
    hazeState: HazeState?,
    isDark: Boolean = isAppInDarkMode(),
    accentColor: Color = MaterialTheme.colorScheme.primary,
    shape: Shape = RoundedCornerShape(percent = 50),
    elevation: Dp = 12.dp,
    borderWidth: Dp = 1.2.dp
): Modifier {
    return this.liquidFrostedGlass(
        hazeState = hazeState,
        isDark = isDark,
        accentColor = accentColor,
        shape = shape,
        elevation = elevation,
        borderWidth = borderWidth,
        blurRadius = 26.dp,
        frostTintAlpha = 0.08f,
        surfaceOpacity = 0.10f,
        refractionStrength = 0.35f,
        chromaticAberration = 0.07f,
        highlightStrength = 0.85f
    )
}

