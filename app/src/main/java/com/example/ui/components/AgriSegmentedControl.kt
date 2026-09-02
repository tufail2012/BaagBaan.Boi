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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
 * - Floating pill-shaped outer container with soft elevation shadow and crisp outline.
 * - Real backdrop blur of the content behind via Haze.
 * - Sliding 3D Bubble / Droplet active indicator with horizontal spring and wobble physics.
 * - Clean, semi-transparent active palette tint with raised 3D specular highlight and drop shadow.
 * - High-contrast theme-aware typography with smooth color transitions.
 */
@Composable
fun AgriSegmentedControl(
    selectedMode: Int, // 0 = New Entry, 1 = Records, 2 = Analytics etc.
    onModeSelected: (Int) -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    newEntryLabel: String = "New Entry",
    recordsLabel: String = "Records",
    accentColor: Color = MaterialTheme.colorScheme.primary
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
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    val haptic = LocalHapticFeedback.current
    val isDark = isAppInDarkMode()
    val isAmoled = isAppInAmoledMode()
    val surfaceColor = MaterialTheme.colorScheme.surface

    val containerShape = RoundedCornerShape(percent = 50)
    val itemShape = RoundedCornerShape(percent = 50)

    // Translucent glass tint letting real background colors diffuse through cleanly
    val glassTint = when {
        isAmoled -> Color(0xFF000000).copy(alpha = 0.28f)
        isDark -> Color(0xFF0B132B).copy(alpha = 0.22f)
        else -> Color(0xFFFFFFFF).copy(alpha = 0.18f)
    }

    // Translucent liquid glass gradient with subtle accent color diffusion
    val translucentGlassBrush = Brush.verticalGradient(
        colors = when {
            isAmoled -> listOf(
                Color.White.copy(alpha = 0.08f),
                Color(0xFF09090B).copy(alpha = 0.18f),
                accentColor.copy(alpha = 0.03f),
                Color.Black.copy(alpha = 0.26f)
            )
            isDark -> listOf(
                Color.White.copy(alpha = 0.12f),
                Color(0xFF1E293B).copy(alpha = 0.15f),
                accentColor.copy(alpha = 0.04f),
                Color(0xFF0F172A).copy(alpha = 0.22f)
            )
            else -> listOf(
                Color.White.copy(alpha = 0.38f),
                Color(0xFFF8FAFC).copy(alpha = 0.12f),
                accentColor.copy(alpha = 0.03f),
                Color.White.copy(alpha = 0.25f)
            )
        }
    )

    // Thin bright 1dp glass rim with specular top highlight and accent refraction
    val glassRimBrush = Brush.verticalGradient(
        colors = if (isDark || isAmoled) {
            listOf(
                Color.White.copy(alpha = 0.65f), // Bright specular top rim
                Color.White.copy(alpha = 0.25f), // Clear glass sides
                accentColor.copy(alpha = 0.30f), // Accent color refraction
                Color.White.copy(alpha = 0.15f)  // Soft bottom rim
            )
        } else {
            listOf(
                Color.White.copy(alpha = 0.95f), // Crisp bright top rim
                Color.White.copy(alpha = 0.40f), // Translucent sides
                accentColor.copy(alpha = 0.25f), // Soft accent diffusion
                Color.White.copy(alpha = 0.35f)  // Subtle bottom rim
            )
        }
    )

    // Outer floating pill container
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .height(50.dp)
            // 1. Soft floating 3D elevation shadow
            .shadow(
                elevation = 10.dp,
                shape = containerShape,
                spotColor = if (isDark || isAmoled) Color.Black.copy(alpha = 0.50f) else Color(0x24000000),
                ambientColor = if (isDark || isAmoled) Color.Black.copy(alpha = 0.30f) else Color(0x10000000)
            )
            .clip(containerShape)
            // 2. Real Haze backdrop blur with explicit backgroundColor
            .hazeEffect(
                state = hazeState,
                style = HazeStyle(
                    backgroundColor = surfaceColor, // Kept in every theme to prevent crashes
                    tint = HazeTint(glassTint),
                    blurRadius = 24.dp,
                    noiseFactor = 0.02f
                )
            )
            // 3. Translucent liquid glass gradient & color diffusion
            .background(brush = translucentGlassBrush, shape = containerShape)
            // 4. Subtle top inner specular reflection / edge sheen
            .drawWithContent {
                drawContent()
                val w = size.width
                val highlightHeight = 1.2.dp.toPx()
                val margin = 14.dp.toPx()
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = if (isDark || isAmoled) 0.55f else 0.80f),
                            Color.Transparent
                        ),
                        startX = margin,
                        endX = w - margin
                    ),
                    topLeft = Offset(margin, 1.dp.toPx()),
                    size = Size(w - (margin * 2), highlightHeight)
                )
            }
            // 5. Thin bright 1dp glass rim
            .border(
                border = BorderStroke(width = 1.dp, brush = glassRimBrush),
                shape = containerShape
            )
            .padding(4.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val totalWidth = maxWidth
            val itemCount = items.size.coerceAtLeast(1)
            val slotWidth = totalWidth / itemCount
            val targetOffset = slotWidth * selectedIndex

            // Natural smooth fluid spring slide
            val animatedOffsetX by animateDpAsState(
                targetValue = targetOffset,
                animationSpec = spring(
                    dampingRatio = 0.72f, // Natural fluid spring physics
                    stiffness = 320f
                ),
                label = "segmentedSlide"
            )

            // Fluid Water-like Sliding Liquid Pill Indicator
            Box(
                modifier = Modifier
                    .offset(x = animatedOffsetX)
                    .align(Alignment.CenterStart)
                    .width(slotWidth)
                    .fillMaxHeight()
                    .bubbleDropletPillIndicator(
                        shape = itemShape,
                        accentColor = accentColor,
                        isDark = isDark,
                        isAmoled = isAmoled
                    )
            )

            // Tab Text Items Row
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, item ->
                    val isSelected = index == selectedIndex

                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            if (isDark) accentColor else accentColor
                        } else {
                            if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        },
                        animationSpec = tween(durationMillis = 200),
                        label = "tabTextColor"
                    )

                    // 3D Embossed lift, scale, and subtle rotation tilt
                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1.05f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = 0.72f,
                            stiffness = 320f
                        ),
                        label = "tabScale"
                    )

                    val liftY by animateDpAsState(
                        targetValue = if (isSelected) (-1.5).dp else 0.dp,
                        animationSpec = spring(
                            dampingRatio = 0.72f,
                            stiffness = 320f
                        ),
                        label = "tabLift"
                    )

                    val rotX by animateFloatAsState(
                        targetValue = if (isSelected) 4f else 0f,
                        animationSpec = spring(
                            dampingRatio = 0.72f,
                            stiffness = 320f
                        ),
                        label = "tabRotX"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .testTag(item.testTag)
                            .clip(itemShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    if (selectedIndex != index) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onItemSelected(index)
                                    }
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.title,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = textColor,
                            maxLines = 1,
                            modifier = Modifier.graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationY = liftY.toPx()
                                rotationX = rotX
                                cameraDistance = 16f * density
                            }
                        )
                    }
                }
            }
        }
    }
}
