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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    modifier: Modifier = Modifier
) {
    val containerShape = RoundedCornerShape(32.dp)
    val isDark = isAppInDarkMode()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .height(52.dp),
        shape = containerShape,
        color = if (isDark) Color.Black else Color(0xFFECEFF1),
        border = if (isDark) BorderStroke(1.dp, Color(0xFF262626)) else null,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isNewEntry = selectedMode == 0
            val isRecords = selectedMode == 1

            // New Entry Tab
            SegmentedTabItem(
                text = "New Entry",
                isSelected = isNewEntry,
                isDark = isDark,
                onClick = { onModeSelected(0) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("tab_new_entry")
            )

            // Records Tab
            SegmentedTabItem(
                text = "Records",
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
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White
        } else Color.Transparent,
        label = "tab_bg"
    )

    val textColor by animateColorAsState(
        targetValue = if (isSelected) {
            if (isDark) primaryColor else primaryColor
        } else {
            if (isDark) Color(0xFFAAAAAA) else Color(0xFF546E7A)
        },
        label = "tab_text"
    )

    val itemShape = RoundedCornerShape(28.dp)
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(itemShape)
            .background(backgroundColor)
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
