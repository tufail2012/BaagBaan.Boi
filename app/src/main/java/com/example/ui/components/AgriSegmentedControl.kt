package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
 * - Selected tab indicator with a raised, bubbly 3D appearance, subtle color shade, and liquid specular highlights.
 * - High-contrast theme-aware typography with smooth transition animations.
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
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
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
                        .then(
                            if (isSelected) {
                                Modifier
                                    // 1. Raised 3D depth shadow
                                    .shadow(
                                        elevation = 4.dp,
                                        shape = itemShape,
                                        spotColor = if (isDark) Color.Black.copy(alpha = 0.50f) else Color(0x30000000),
                                        ambientColor = if (isDark) Color.Black.copy(alpha = 0.25f) else Color(0x18000000)
                                    )
                                    // 2. Extremely subtle color shade background gradient
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = if (isDark) {
                                                listOf(
                                                    Color.White.copy(alpha = 0.16f),
                                                    accentColor.copy(alpha = 0.08f),
                                                    Color.White.copy(alpha = 0.03f),
                                                    Color.White.copy(alpha = 0.08f)
                                                )
                                            } else {
                                                listOf(
                                                    Color.White.copy(alpha = 0.70f),
                                                    accentColor.copy(alpha = 0.08f),
                                                    Color.White.copy(alpha = 0.15f),
                                                    Color.White.copy(alpha = 0.40f)
                                                )
                                            }
                                        ),
                                        shape = itemShape
                                    )
                                    // 3. Specular 3D liquid meniscus highlight & reflection arcs
                                    .drawWithContent {
                                        drawContent()

                                        val w = size.width
                                        val h = size.height

                                        // Upper Specular Highlight (Curved dome light reflection on top crest)
                                        val highlightHeight = h * 0.42f
                                        val highlightWidth = w * 0.78f
                                        val highlightX = (w - highlightWidth) / 2f
                                        val highlightY = 2.dp.toPx()

                                        drawOval(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.White.copy(alpha = if (isDark) 0.45f else 0.80f),
                                                    Color.White.copy(alpha = if (isDark) 0.12f else 0.25f),
                                                    Color.Transparent
                                                ),
                                                startY = highlightY,
                                                endY = highlightY + highlightHeight
                                            ),
                                            topLeft = Offset(highlightX, highlightY),
                                            size = Size(highlightWidth, highlightHeight)
                                        )

                                        // Bottom ambient bounce light arc
                                        val bottomReflectHeight = h * 0.22f
                                        val bottomReflectWidth = w * 0.60f
                                        val bottomX = (w - bottomReflectWidth) / 2f
                                        val bottomY = h - bottomReflectHeight - 2.dp.toPx()

                                        drawOval(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    Color.White.copy(alpha = if (isDark) 0.18f else 0.35f)
                                                ),
                                                startY = bottomY,
                                                endY = bottomY + bottomReflectHeight
                                            ),
                                            topLeft = Offset(bottomX, bottomY),
                                            size = Size(bottomReflectWidth, bottomReflectHeight)
                                        )
                                    }
                                    // 4. Refractive 3D Water Droplet Rim Border
                                    .border(
                                        BorderStroke(
                                            width = 1.dp,
                                            brush = Brush.verticalGradient(
                                                colors = if (isDark) {
                                                    listOf(
                                                        Color.White.copy(alpha = 0.50f),
                                                        accentColor.copy(alpha = 0.20f),
                                                        Color.White.copy(alpha = 0.25f)
                                                    )
                                                } else {
                                                    listOf(
                                                        Color.White.copy(alpha = 0.85f),
                                                        accentColor.copy(alpha = 0.25f),
                                                        Color.White.copy(alpha = 0.50f)
                                                    )
                                                }
                                            )
                                        ),
                                        shape = itemShape
                                    )
                            } else {
                                Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onItemSelected(index)
                                    }
                                )
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

