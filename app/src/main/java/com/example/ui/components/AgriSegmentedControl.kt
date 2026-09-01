package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class SegmentedTabEntry(
    val title: String,
    val testTag: String
)

/**
 * Floating Pill Segmented Control (New Entry / Records toggle).
 * Features:
 * - Floating pill-shaped outer container with soft elevation shadow and crisp outline.
 * - Sliding 3D Bubble / Droplet active indicator with horizontal spring and wobble physics.
 * - Clean, semi-transparent active palette tint with raised 3D specular highlight and drop shadow.
 * - High-contrast theme-aware typography with smooth color transitions.
 */
@Composable
fun AgriSegmentedControl(
    selectedMode: Int, // 0 = New Entry, 1 = Records, 2 = Analytics etc.
    onModeSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    newEntryLabel: String = "New Entry",
    recordsLabel: String = "Records",
    accentColor: Color = MaterialTheme.colorScheme.primary,
    hazeState: Any? = null
) {
    val items = listOf(
        SegmentedTabEntry(title = newEntryLabel, testTag = "tab_new_entry"),
        SegmentedTabEntry(title = recordsLabel, testTag = "tab_records")
    )

    LiquidGlassSegmentedSwitcher(
        items = items,
        selectedIndex = selectedMode.coerceIn(0, items.size - 1),
        onItemSelected = onModeSelected,
        accentColor = accentColor,
        modifier = modifier
    )
}

@Composable
fun LiquidGlassSegmentedSwitcher(
    items: List<SegmentedTabEntry>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    hazeState: Any? = null
) {
    val haptic = LocalHapticFeedback.current
    val isDark = isAppInDarkMode()
    val isAmoled = isAppInAmoledMode()

    val containerShape = RoundedCornerShape(percent = 50)
    val itemShape = RoundedCornerShape(percent = 50)

    val containerBgColor = when {
        isAmoled -> Color(0xFF000000)
        isDark -> Color(0xFF1E293B)
        else -> Color(0xFFFFFFFF)
    }

    val containerBorderColor = when {
        isAmoled -> Color(0xFF262626)
        isDark -> Color(0xFF334155)
        else -> Color(0xFFE2E8F0)
    }

    // Snappy tactile horizontal wobble / shake animation on tab switch
    val wobbleOffset = remember { Animatable(0f) }
    LaunchedEffect(selectedIndex) {
        wobbleOffset.snapTo(0f)
        wobbleOffset.animateTo(
            targetValue = 0f,
            animationSpec = keyframes {
                durationMillis = 320
                0f at 0
                4f at 75 using FastOutSlowInEasing
                -3f at 150 using FastOutSlowInEasing
                1.5f at 225 using FastOutSlowInEasing
                0f at 320
            }
        )
    }

    // Outer floating pill container
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .height(50.dp)
            .shadow(
                elevation = 4.dp,
                shape = containerShape,
                spotColor = if (isDark) Color.Black.copy(alpha = 0.40f) else Color(0x25000000),
                ambientColor = if (isDark) Color.Black.copy(alpha = 0.20f) else Color(0x15000000)
            )
            .clip(containerShape)
            .background(containerBgColor)
            .border(BorderStroke(1.dp, containerBorderColor), containerShape)
            .padding(4.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val totalWidth = maxWidth
            val itemCount = items.size.coerceAtLeast(1)
            val slotWidth = totalWidth / itemCount
            val targetOffset = slotWidth * selectedIndex

            val animatedOffsetX by animateDpAsState(
                targetValue = targetOffset,
                animationSpec = spring(
                    dampingRatio = 0.62f, // Spring physics for natural bounce
                    stiffness = 380f
                ),
                label = "segmentedSlide"
            )

            // Sliding 3D Bubble / Droplet Indicator
            Box(
                modifier = Modifier
                    .offset(x = animatedOffsetX + wobbleOffset.value.dp)
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
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
