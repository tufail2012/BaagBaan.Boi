package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/**
 * Frosted Liquid Glass modifier for Floating Chrome, Pill Headers, FABs, and Overlays.
 * Follows the exact visual language from AgriBottomNav and AgriSegmentedControl:
 * - Real-time backdrop blur (26.dp) via HazeState
 * - Dynamic Light/Dark/AMOLED optical tinting
 * - Specular vertical rim highlight
 * - Multi-layer ambient and directional elevation shadow
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
    val screenBgColor = MaterialTheme.colorScheme.background
    val isAmoled = isDark && (screenBgColor.luminance() < 0.01f || screenBgColor == Color.Black)

    val hazeStyle = remember(isDark, isAmoled, accentColor, screenBgColor) {
        when {
            isAmoled -> HazeStyle(
                backgroundColor = screenBgColor,
                tint = HazeTint(accentColor.copy(alpha = 0.07f)),
                blurRadius = 26.dp
            )
            isDark -> HazeStyle(
                backgroundColor = screenBgColor,
                tint = HazeTint(Color(0xFF0F172A).copy(alpha = 0.10f)),
                blurRadius = 26.dp
            )
            else -> HazeStyle(
                backgroundColor = screenBgColor,
                tint = HazeTint(Color.White.copy(alpha = 0.10f)),
                blurRadius = 26.dp
            )
        }
    }

    val shadowAmbient = if (isDark) Color.Black.copy(alpha = 0.25f) else Color(0xFF0F172A).copy(alpha = 0.06f)
    val shadowSpot = if (isDark) Color.Black.copy(alpha = 0.35f) else accentColor.copy(alpha = 0.10f)

    val baseGlassOverlayBrush = Brush.verticalGradient(
        when {
            isAmoled -> listOf(Color(0xFF141414).copy(alpha = 0.08f), Color(0xFF070707).copy(alpha = 0.05f))
            isDark -> listOf(Color(0xFF1E293B).copy(alpha = 0.12f), Color(0xFF0F172A).copy(alpha = 0.08f))
            else -> listOf(Color.White.copy(alpha = 0.14f), Color.White.copy(alpha = 0.07f))
        }
    )

    val specularBorderBrush = Brush.verticalGradient(
        when {
            isAmoled -> listOf(Color.White.copy(alpha = 0.60f), accentColor.copy(alpha = 0.35f), Color.White.copy(alpha = 0.15f))
            isDark -> listOf(Color.White.copy(alpha = 0.48f), Color.White.copy(alpha = 0.12f))
            else -> listOf(Color.White.copy(alpha = 0.88f), Color.White.copy(alpha = 0.22f))
        }
    )

    return this
        .shadow(
            elevation = elevation,
            shape = shape,
            clip = false,
            ambientColor = shadowAmbient,
            spotColor = shadowSpot
        )
        .clip(shape)
        .then(
            if (hazeState != null) {
                Modifier.hazeEffect(state = hazeState, style = hazeStyle)
            } else {
                Modifier
            }
        )
        .background(baseGlassOverlayBrush)
        .border(
            width = borderWidth,
            brush = specularBorderBrush,
            shape = shape
        )
}
