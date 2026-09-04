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

    // Outer glassmorphism container integrated directly into layout (no floating cutout)
    /* CSS glassmorphism specification:
     * background: rgba(<selected-palette-shade>, 0.12);
     * -webkit-backdrop-filter: blur(10px);
     * backdrop-filter: blur(10px);
     * border: 1px solid rgba(255, 255, 255, 0.25);
     */
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .height(48.dp)
            .then(
                Modifier.hazeEffect(
                    state = hazeState,
                    style = HazeStyle(
                        blurRadius = 10.dp,
                        tints = listOf(
                            HazeTint(color = accentColor.copy(alpha = if (isDark) 0.12f else 0.08f))
                        ),
                        backgroundColor = (if (isDark) Color(0xFF0F172A) else Color.White).copy(alpha = if (isDark) 0.45f else 0.65f)
                    )
                )
            )
            .clip(containerShape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        accentColor.copy(alpha = if (isDark) 0.16f else 0.10f),
                        (if (isDark) Color(0xFF0F172A) else Color.White).copy(alpha = if (isDark) 0.45f else 0.65f)
                    )
                ),
                shape = containerShape
            )
            .border(
                BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = if (isDark) 0.35f else 0.60f),
                            accentColor.copy(alpha = 0.22f),
                            Color.White.copy(alpha = 0.10f)
                        )
                    )
                ),
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
                                    accentColor.copy(alpha = rippleAlpha),
                                    Color.White.copy(alpha = rippleAlpha * 0.6f),
                                    Color.Transparent
                                )
                            ),
                            shape = itemShape
                        )
                        .background(
                            color = accentColor.copy(alpha = rippleAlpha * 0.15f),
                            shape = itemShape
                        )
                )
            }

            // Fluid Water-like Sliding Liquid Pill Indicator
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
                    .bubbleDropletPillIndicator(
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

                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            if (isDark || isAmoled) Color.White else Color.Black
                        } else {
                            if (isDark) Color(0xFFB0A8B8) else Color(0xFF475569)
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
