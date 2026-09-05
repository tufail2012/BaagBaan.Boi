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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
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

    // Fully rounded pill (Border Radius: 24px)
    val containerShape = RoundedCornerShape(24.dp)
    val itemShape = RoundedCornerShape(20.dp)

    // Pill Container (Track):
    // Background: Subtle translucent tint matching the active screen palette (rgba(255, 255, 255, 0.35) or a 5–8% primary palette tint).
    val trackBgBrush = if (isDark || isAmoled) {
        Brush.verticalGradient(
            listOf(
                accentColor.copy(alpha = 0.08f),
                if (isAmoled) Color.Black.copy(alpha = 0.50f) else Color(0xFF0F172A).copy(alpha = 0.40f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.35f),
                accentColor.copy(alpha = 0.06f),
                Color.White.copy(alpha = 0.35f)
            )
        )
    }

    // Border: 1px solid rgba(255, 255, 255, 0.4)
    val trackBorderBrush = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = if (isDark) 0.30f else 0.45f),
            accentColor.copy(alpha = if (isDark) 0.15f else 0.20f),
            Color.White.copy(alpha = if (isDark) 0.20f else 0.35f)
        )
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .height(48.dp)
            .shadow(
                elevation = 3.dp,
                shape = containerShape,
                spotColor = Color.Black.copy(alpha = if (isDark) 0.12f else 0.04f),
                ambientColor = Color.Black.copy(alpha = if (isDark) 0.06f else 0.02f)
            )
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
            .clip(containerShape)
            .background(trackBgBrush, shape = containerShape)
            .border(BorderStroke(1.dp, trackBorderBrush), shape = containerShape)
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

            // Soft water droplet spreading & expanding ripple wave on tab switch
            val dropletSpread = remember { Animatable(1f) }
            val dropletRipple = remember { Animatable(1f) }

            LaunchedEffect(selectedIndex) {
                launch {
                    dropletSpread.snapTo(0.88f)
                    dropletSpread.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = 0.60f, // Gentle water droplet surface tension
                            stiffness = 250f
                        )
                    )
                }
                launch {
                    dropletRipple.snapTo(0f)
                    dropletRipple.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = 450,
                            easing = FastOutSlowInEasing
                        )
                    )
                }
            }

            val offsetDelta = (targetOffset - animatedOffsetX).value
            val glideStretch = (kotlin.math.abs(offsetDelta) / slotWidth.value.coerceAtLeast(1f)).coerceIn(0f, 0.16f)
            val dynamicScaleX = dropletSpread.value * (1f + glideStretch * 0.40f)
            val dynamicScaleY = dropletSpread.value * (1f - glideStretch * 0.18f)

            // Subtle water droplet expanding ripple wave
            if (dropletRipple.value < 0.99f) {
                val rippleProgress = dropletRipple.value
                val rippleAlpha = ((1f - rippleProgress) * if (!isDark && !isAmoled) 0.32f else 0.24f).coerceIn(0f, 1f)
                val extraWidth = (rippleProgress * 14).dp
                val extraHeight = (rippleProgress * 6).dp

                Box(
                    modifier = Modifier
                        .offset(
                            x = animatedOffsetX - (extraWidth / 2),
                            y = -(extraHeight / 2)
                        )
                        .align(Alignment.CenterStart)
                        .width(slotWidth + extraWidth)
                        .fillMaxHeight()
                        .clip(itemShape)
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = rippleAlpha * 0.7f),
                                    accentColor.copy(alpha = rippleAlpha * 0.3f),
                                    Color.Transparent
                                )
                            ),
                            shape = itemShape
                        )
                        .background(
                            color = Color.White.copy(alpha = rippleAlpha * 0.18f),
                            shape = itemShape
                        )
                )
            }

            // Active Pill ("Water Glass" Look):
            // background: linear-gradient(135deg, rgba(255, 255, 255, 0.7) 0%, rgba(255, 255, 255, 0.35) 100%);
            // backdrop-filter: blur(12px);
            // box-shadow: inset 0 1px 1px rgba(255, 255, 255, 0.8), 0 4px 12px rgba(0, 0, 0, 0.05);
            // border: 1px solid rgba(255, 255, 255, 0.6);
            Box(
                modifier = Modifier
                    .offset(x = animatedOffsetX)
                    .align(Alignment.CenterStart)
                    .width(slotWidth)
                    .fillMaxHeight()
                    .graphicsLayer {
                        scaleX = dynamicScaleX
                        scaleY = dynamicScaleY
                    }
                    .waterGlassPillIndicator(
                        hazeState = hazeState,
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

                    // Pill Typography & Theming:
                    // Active Tab Text: Must dynamically adapt to the active screen's primary color palette (e.g., #D32F2F in Garden Planning, #2E7D32 in Local Plants, etc.), with font-weight: 700.
                    // Inactive Tab Text: Subdued theme color or high-contrast neutral (rgba(0, 0, 0, 0.55) or 60% opacity of theme text color), font-weight: 500.
                    val activeTextColor = accentColor
                    val inactiveTextColor = if (isDark || isAmoled) {
                        Color.White.copy(alpha = 0.60f)
                    } else {
                        Color.Black.copy(alpha = 0.55f)
                    }

                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) activeTextColor else inactiveTextColor,
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

/**
 * Clean "Water Glass / Liquid" aesthetic modifier for the active indicator pill.
 * Specifications:
 * - background: linear-gradient(135deg, rgba(255, 255, 255, 0.7) 0%, rgba(255, 255, 255, 0.35) 100%);
 * - backdrop-filter: blur(12px);
 * - box-shadow: inset 0 1px 1px rgba(255, 255, 255, 0.8), 0 4px 12px rgba(0, 0, 0, 0.05);
 * - border: 1px solid rgba(255, 255, 255, 0.6);
 */
@Composable
fun Modifier.waterGlassPillIndicator(
    hazeState: HazeState? = null,
    shape: Shape = RoundedCornerShape(20.dp),
    accentColor: Color = MaterialTheme.colorScheme.primary,
    isDark: Boolean = isAppInDarkMode(),
    isAmoled: Boolean = isAppInAmoledMode()
): Modifier {
    val activePillGradient = if (isDark || isAmoled) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.25f),
                accentColor.copy(alpha = 0.15f),
                Color.White.copy(alpha = 0.10f)
            ),
            start = Offset.Zero,
            end = Offset.Infinite
        )
    } else {
        // background: linear-gradient(135deg, rgba(255, 255, 255, 0.7) 0%, rgba(255, 255, 255, 0.35) 100%);
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.70f),
                Color.White.copy(alpha = 0.35f)
            ),
            start = Offset.Zero,
            end = Offset.Infinite
        )
    }

    val pillBorderBrush = if (isDark || isAmoled) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.50f),
                Color.White.copy(alpha = 0.25f)
            ),
            start = Offset.Zero,
            end = Offset.Infinite
        )
    } else {
        // border: 1px solid rgba(255, 255, 255, 0.6);
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.65f),
                Color.White.copy(alpha = 0.55f)
            ),
            start = Offset.Zero,
            end = Offset.Infinite
        )
    }

    return this
        // box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
        .shadow(
            elevation = 3.dp,
            shape = shape,
            spotColor = Color.Black.copy(alpha = if (isDark || isAmoled) 0.12f else 0.05f),
            ambientColor = Color.Black.copy(alpha = if (isDark || isAmoled) 0.06f else 0.02f)
        )
        .clip(shape)
        // backdrop-filter: blur(12px);
        .then(
            if (hazeState != null) {
                Modifier.hazeEffect(
                    state = hazeState,
                    style = HazeStyle(
                        blurRadius = 12.dp,
                        tints = listOf(
                            HazeTint(color = accentColor.copy(alpha = if (isDark) 0.06f else 0.03f))
                        ),
                        backgroundColor = Color.Transparent
                    )
                )
            } else Modifier
        )
        // background: linear-gradient(135deg, rgba(255, 255, 255, 0.7) 0%, rgba(255, 255, 255, 0.35) 100%);
        .background(
            brush = activePillGradient,
            shape = shape
        )
        // box-shadow: inset 0 1px 1px rgba(255, 255, 255, 0.8)
        .drawWithContent {
            drawContent()
            val w = size.width
            val h = size.height
            // Inset top specular highlight reflection (water meniscus reflection)
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isDark || isAmoled) 0.55f else 0.80f),
                        Color.White.copy(alpha = if (isDark || isAmoled) 0.15f else 0.25f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = h * 0.5f
                ),
                topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                size = Size(w - 2.dp.toPx(), h - 2.dp.toPx()),
                cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )
        }
        // border: 1px solid rgba(255, 255, 255, 0.6);
        .border(
            width = 1.dp,
            brush = pillBorderBrush,
            shape = shape
        )
}
