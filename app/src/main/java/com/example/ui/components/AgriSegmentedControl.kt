package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AgriSegmentedControl(
    selectedMode: Int, // 0 = New Entry, 1 = Records, 2 = Analytics
    onModeSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    newEntryLabel: String = "New Entry",
    recordsLabel: String = "Records"
) {
    val containerShape = RoundedCornerShape(32.dp)
    val isDark = isAppInDarkMode()

    val containerGlassGradient = if (!isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.70f),
                Color(0xFFE2E8F0).copy(alpha = 0.50f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1E293B).copy(alpha = 0.65f),
                Color(0xFF0F172A).copy(alpha = 0.78f)
            )
        )
    }

    val containerGlassBorder = if (!isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.95f),
                Color.White.copy(alpha = 0.40f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.28f),
                Color.White.copy(alpha = 0.08f)
            )
        )
    }

    val containerShadowColor = if (!isDark) {
        Color(0xFF0F172A).copy(alpha = 0.12f)
    } else {
        Color.Black.copy(alpha = 0.55f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .height(52.dp)
            .shadow(
                elevation = 12.dp,
                shape = containerShape,
                clip = false,
                ambientColor = containerShadowColor,
                spotColor = containerShadowColor
            )
            .clip(containerShape)
            .background(containerGlassGradient)
            .border(
                width = 1.2.dp,
                brush = containerGlassBorder,
                shape = containerShape
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isNewEntry = selectedMode == 0
            val isRecords = selectedMode == 1

            // New Entry Tab
            SegmentedTabItem(
                text = newEntryLabel,
                isSelected = isNewEntry,
                isDark = isDark,
                onClick = { onModeSelected(0) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("tab_new_entry")
            )

            // Records Tab
            SegmentedTabItem(
                text = recordsLabel,
                isSelected = isRecords,
                isDark = isDark,
                onClick = { onModeSelected(1) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("tab_records")
            )
        }
    }
}

@Composable
private fun SegmentedTabItem(
    text: String,
    isSelected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary

    val textColor by animateColorAsState(
        targetValue = if (isSelected) {
            if (isDark) onPrimaryColor else primaryColor
        } else {
            if (isDark) Color(0xFF94A3B8) else Color(0xFF455A64)
        },
        label = "tab_text"
    )

    val itemShape = RoundedCornerShape(28.dp)
    val haptic = LocalHapticFeedback.current

    val itemShadowColor = if (!isDark) {
        Color(0xFF0F172A).copy(alpha = 0.12f)
    } else {
        Color.Black.copy(alpha = 0.45f)
    }

    val selectedGlassGradient = if (!isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.95f),
                Color(0xFFF1F5F9).copy(alpha = 0.90f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                primaryColor.copy(alpha = 0.95f),
                primaryColor.copy(alpha = 0.85f)
            )
        )
    }

    val selectedBorder = if (!isDark) {
        BorderStroke(1.dp, Color.White)
    } else {
        BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .then(
                if (isSelected) {
                    Modifier
                        .shadow(
                            elevation = if (isDark) 4.dp else 3.dp,
                            shape = itemShape,
                            clip = false,
                            ambientColor = itemShadowColor,
                            spotColor = itemShadowColor
                        )
                        .clip(itemShape)
                        .background(selectedGlassGradient)
                        .border(selectedBorder, shape = itemShape)
                } else {
                    Modifier
                        .clip(itemShape)
                        .background(Color.Transparent)
                }
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = primaryColor.copy(alpha = 0.20f)),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = textColor
        )
    }
}
