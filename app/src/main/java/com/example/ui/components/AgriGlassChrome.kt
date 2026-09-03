package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Material 3 Chrome / Container modifier for floating headers, chips, and overlays.
 * In Dark Mode, provides a rich glossy dark bevel surface with a crisp specular top rim,
 * perfectly matching the reference screenshot.
 */
@Composable
fun Modifier.frostedGlassChrome(
    isDark: Boolean = isAppInDarkMode(),
    isAmoled: Boolean = isAppInAmoledMode(),
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
    if (isDark || isAmoled) {
        val darkBgBrush = if (isAmoled) {
            // AMOLED Mode: pure true pitch black foundation
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF161418),
                    Color(0xFF0C0B0E),
                    Color(0xFF000000)
                )
            )
        } else {
            // Dark Mode: dark charcoal / slate gray tones with subtle depth (no pure black)
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF2C2833),
                    Color(0xFF221F28),
                    Color(0xFF19171E)
                )
            )
        }
        val borderBrush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = if (isAmoled) 0.35f else 0.38f),
                Color.White.copy(alpha = if (isAmoled) 0.12f else 0.15f),
                Color.White.copy(alpha = if (isAmoled) 0.04f else 0.08f)
            )
        )

        return this
            .then(
                if (elevation > 0.dp) {
                    Modifier.shadow(
                        elevation = elevation,
                        shape = shape,
                        clip = false,
                        spotColor = Color.Black.copy(alpha = if (isAmoled) 0.60f else 0.40f),
                        ambientColor = Color.Black.copy(alpha = if (isAmoled) 0.40f else 0.25f)
                    )
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .background(brush = darkBgBrush, shape = shape)
            .drawWithContent {
                drawContent()
                val w = size.width
                val highlightH = 1.5.dp.toPx()
                val margin = 16.dp.toPx()
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = if (isAmoled) 0.28f else 0.32f),
                            Color.Transparent
                        ),
                        startX = margin,
                        endX = w - margin
                    ),
                    topLeft = Offset(margin, 1.dp.toPx()),
                    size = Size(w - (margin * 2), highlightH)
                )
            }
            .border(width = borderWidth, brush = borderBrush, shape = shape)
    }

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
