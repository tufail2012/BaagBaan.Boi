package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

data class SegmentedTabEntry(
    val title: String,
    val testTag: String
)

/**
 * Floating Pill Segmented Control (New Entry / Records toggle).
 * Features:
 * - Pill Container (Track): 24dp rounded pill with subtle translucent tint matching active palette and 1px solid rgba(255,255,255,0.4) border.
 * - Active Pill ("Water Glass" Look): Clean, liquid aesthetic with 135deg translucent sheen, 12dp blur, inset highlight reflection, and drop shadow.
 * - Pill Typography & Theming: Active tab dynamically adapts to active screen's primary color palette with fontWeight 700, and inactive tab uses subdued high-contrast neutral with fontWeight 500.
 */
@Composable
fun AgriSegmentedControl(
    selectedMode: Int, // 0 = New Entry, 1 = Records, 2 = Analytics etc.
    onModeSelected: (Int) -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    newEntryLabel: String = "New Entry",
    recordsLabel: String = "Records",
    accentColor: Color = MaterialTheme.colorScheme.primary,
    backdrop: Backdrop? = null
) {
    val items = listOf(
        SegmentedTabEntry(title = newEntryLabel, testTag = "tab_new_entry"),
        SegmentedTabEntry(title = recordsLabel, testTag = "tab_records")
    )

    LiquidGlassSegmentedSwitcher(
        items = items,
        selectedIndex = selectedMode.coerceIn(0, items.size - 1),
        onItemSelected = onModeSelected,
        hazeState = hazeState,
        accentColor = accentColor,
        backdrop = backdrop,
        modifier = modifier
    )
}

@Composable
fun LiquidGlassSegmentedSwitcher(
    items: List<SegmentedTabEntry>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    backdrop: Backdrop? = null
) {
    val indicatorAccent = com.example.ui.theme.paletteSecondaryAccent()
    val haptic = LocalHapticFeedback.current
    val isDark = isAppInDarkMode()
    val isAmoled = isAppInAmoledMode()
    val scrollGlass = LocalScrollGlassSource.current
    val glassBackdrop: Backdrop? = scrollGlass?.combined ?: backdrop
    val useRealGlass = glassBackdrop != null && isGlassSupported()
    val realGlassIndicatorColor = glassIndicatorColor().copy(alpha = 0.5f)

    // Fully rounded pill (Border Radius: 9999px / 30px)
    val containerShape = RoundedCornerShape(percent = 50)
    val itemShape = RoundedCornerShape(percent = 50)

    // Pill Container (Track):
    // rgba(255, 255, 255, 0.08); border: 1px solid rgba(255, 255, 255, 0.12); border-radius: 9999px;
    val trackBgBrush = if (isDark || isAmoled) {
        SolidColor(Color.White.copy(alpha = 0.08f))
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.50f),
                accentColor.copy(alpha = 0.06f),
                Color.White.copy(alpha = 0.45f)
            )
        )
    }

    // Border: 1px solid rgba(255, 255, 255, 0.12)
    val trackBorderBrush = if (isDark || isAmoled) {
        SolidColor(Color.White.copy(alpha = 0.12f))
    } else {
        SolidColor(Color(0xFF000000).copy(alpha = 0.08f))
    }

    val activeTextColor = accentColor
    val inactiveTextColor = if (isDark || isAmoled) {
        Color.White.copy(alpha = 0.60f)
    } else {
        Color.Black.copy(alpha = 0.55f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .height(48.dp)
            .clip(containerShape)
            .liquidGlassNav(shape = containerShape, backdrop = glassBackdrop)
            .then(
                if (!isGlassSupported() || glassBackdrop == null) {
                    Modifier
                        .shadow(
                            elevation = 3.dp,
                            shape = containerShape,
                            spotColor = Color.Black.copy(alpha = if (isDark) 0.12f else 0.04f),
                            ambientColor = Color.Black.copy(alpha = if (isDark) 0.06f else 0.02f)
                        )
                        .clip(containerShape)
                        .then(
                            Modifier.hazeEffect(
                                state = hazeState,
                                style = HazeStyle(
                                    blurRadius = 12.dp,
                                    tints = listOf(
                                        HazeTint(color = accentColor.copy(alpha = if (isDark) 0.08f else 0.05f))
                                    ),
                                    backgroundColor = Color.Transparent
                                )
                            )
                        )
                        .background(trackBgBrush, shape = containerShape)
                        .border(BorderStroke(1.dp, trackBorderBrush), shape = containerShape)
                } else {
                    Modifier
                }
            )
            .glassEdge(containerShape)
            .padding(4.dp)
    ) {
        AgriMotionTabs(
            titles = items.map { it.title },
            selectedIndex = selectedIndex,
            onSelect = onItemSelected,
            selectedTextColor = activeTextColor,
            unselectedTextColor = inactiveTextColor,
            testTags = items.map { it.testTag },
            pillInset = 4.dp,
            pill = { pillModifier ->
                Box(
                    modifier = pillModifier.then(
                        if (useRealGlass) {
                            Modifier
                                .clip(itemShape)
                                .background(realGlassIndicatorColor, itemShape)
                        } else {
                            Modifier.bubblyGlassCapsuleIndicator(
                                hazeState = hazeState,
                                shape = itemShape,
                                accentColor = indicatorAccent,
                                isDark = isDark,
                                isAmoled = isAmoled
                            )
                        }
                    )
                )
            }
        )
    }
}

/**
 * 3D Dimensional "Bubbly Glass" Capsule (Liquid Capsule) Modifier.
 * Specifications:
 * - Border Radius: Full pill shape (border-radius: 9999px / 30px / RoundedCornerShape(percent = 50)).
 * - Multi-Layered Lighting & Gradients:
 *   - Surface Gradient:
 *     linear-gradient(180deg, rgba(255, 255, 255, 0.85) 0%, rgba(255, 255, 255, 0.4) 60%, rgba(var(--theme-primary-rgb), 0.15) 100%)
 *   - Top Specular Highlight & Depth (Inner Glow):
 *     inset 0 1.5px 2px 0 rgba(255, 255, 255, 0.95), /* Top light reflection */
 *     inset 0 -2px 3px 0 rgba(0, 0, 0, 0.04),         /* Bottom curvature shading */
 *     0 4px 12px 0 rgba(var(--theme-primary-rgb), 0.12) /* Ambient colored drop shadow */
 *   - Border: 1px solid rgba(255, 255, 255, 0.7)
 *   - Blur: backdrop-filter: blur(14px)
 */
@Composable
fun Modifier.bubblyGlassCapsuleIndicator(
    hazeState: HazeState? = null,
    shape: Shape = RoundedCornerShape(percent = 50),
    accentColor: Color = MaterialTheme.colorScheme.primary,
    isDark: Boolean = isAppInDarkMode(),
    isAmoled: Boolean = isAppInAmoledMode()
): Modifier {
    val surfaceGradient = if (isDark || isAmoled) {
        Brush.verticalGradient(
            colorStops = arrayOf(
                0.0f to Color.White.copy(alpha = 0.22f),
                0.50f to Color.White.copy(alpha = 0.08f),
                1.0f to accentColor.copy(alpha = 0.20f)
            )
        )
    } else {
        // Light Mode
        Brush.verticalGradient(
            colorStops = arrayOf(
                0.0f to Color.White.copy(alpha = 0.50f),
                0.50f to Color.White.copy(alpha = 0.22f),
                1.0f to accentColor.copy(alpha = 0.20f)
            )
        )
    }

    // Border: 1px solid rgba(255, 255, 255, 0.3)
    val bubblyBorderBrush = if (isDark || isAmoled) {
        SolidColor(Color.White.copy(alpha = 0.30f))
    } else {
        SolidColor(Color.White.copy(alpha = 0.65f))
    }

    return this
        // Drop shadow: 0 4px 14px rgba(var(--theme-primary-rgb), 0.2)
        .shadow(
            elevation = 4.dp,
            shape = shape,
            spotColor = accentColor.copy(alpha = if (isDark || isAmoled) 0.20f else 0.16f),
            ambientColor = accentColor.copy(alpha = if (isDark || isAmoled) 0.12f else 0.08f)
        )
        .clip(shape)
        // Blur: backdrop-filter: blur(14px)
        .then(
            if (hazeState != null) {
                Modifier.hazeEffect(
                    state = hazeState,
                    style = HazeStyle(
                        blurRadius = 14.dp,
                        tints = listOf(
                            HazeTint(color = accentColor.copy(alpha = if (isDark) 0.08f else 0.05f))
                        ),
                        backgroundColor = Color.Transparent
                    )
                )
            } else Modifier
        )
        // Surface Gradient
        .background(
            brush = surfaceGradient,
            shape = shape
        )
        // Top Specular Highlight & Depth (Inner Glow):
        // inset 0 1.5px 2px rgba(255, 255, 255, 0.4),
        // inset 0 -2px 3px rgba(0, 0, 0, 0.25)
        .drawWithContent {
            drawContent()
            val w = size.width
            val h = size.height
            val cornerRadius = CornerRadius(h / 2f, h / 2f)

            // Top specular light reflection: inset 0 1.5px 2px rgba(255, 255, 255, 0.4)
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isDark || isAmoled) 0.40f else 0.70f),
                        Color.White.copy(alpha = if (isDark || isAmoled) 0.15f else 0.25f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = 2.dp.toPx()
                ),
                topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                size = Size(w - 2.dp.toPx(), h - 2.dp.toPx()),
                cornerRadius = cornerRadius,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Bottom curvature shading: inset 0 -2px 3px rgba(0, 0, 0, 0.25)
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = if (isDark || isAmoled) 0.25f else 0.10f)
                    ),
                    startY = (h - 3.dp.toPx()).coerceAtLeast(0f),
                    endY = h
                ),
                topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                size = Size(w - 2.dp.toPx(), h - 2.dp.toPx()),
                cornerRadius = cornerRadius,
                style = Stroke(width = 2.dp.toPx())
            )
        }
        // Border: 1px solid rgba(255, 255, 255, 0.3)
        .border(
            width = 1.dp,
            brush = bubblyBorderBrush,
            shape = shape
        )
}

/**
 * Clean "Water Glass / Liquid" aesthetic modifier pointing to bubbly capsule.
 */
@Composable
fun Modifier.waterGlassPillIndicator(
    hazeState: HazeState? = null,
    shape: Shape = RoundedCornerShape(percent = 50),
    accentColor: Color = MaterialTheme.colorScheme.primary,
    isDark: Boolean = isAppInDarkMode(),
    isAmoled: Boolean = isAppInAmoledMode()
): Modifier = this.bubblyGlassCapsuleIndicator(
    hazeState = hazeState,
    shape = shape,
    accentColor = accentColor,
    isDark = isDark,
    isAmoled = isAmoled
)
