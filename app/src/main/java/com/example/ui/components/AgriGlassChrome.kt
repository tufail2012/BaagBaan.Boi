package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Standard Material 3 Chrome / Container modifier for floating headers, chips, and overlays.
 * Completely solid Material Design 3 surfaceContainer with standard tonal elevation and crisp border.
 */
@Composable
fun Modifier.frostedGlassChrome(
    isDark: Boolean = isAppInDarkMode(),
    accentColor: Color = MaterialTheme.colorScheme.primary,
    shape: Shape = RoundedCornerShape(percent = 50),
    elevation: Dp = 3.dp,
    borderWidth: Dp = 1.dp,
    blurRadius: Dp = 0.dp,
    frostTintAlpha: Float = 0f,
    surfaceOpacity: Float = 1f,
    refractionStrength: Float = 0f,
    chromaticAberration: Float = 0f,
    highlightStrength: Float = 0f,
    innerDepthStrength: Float = 0f
): Modifier {
    val containerColor = MaterialTheme.colorScheme.surfaceContainer
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    return this
        .then(
            if (elevation > 0.dp) {
                Modifier.shadow(elevation = elevation, shape = shape, clip = false)
            } else {
                Modifier
            }
        )
        .clip(shape)
        .background(color = containerColor, shape = shape)
        .border(width = borderWidth, color = borderColor, shape = shape)
}
