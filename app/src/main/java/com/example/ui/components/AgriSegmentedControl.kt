package com.example.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
 * Standard Material Design 3 Segmented Control (New Entry / Records toggle).
 * Replaced all glass/haze/blur with standard Compose Material3 SingleChoiceSegmentedButtonRow + SegmentedButton.
 * Selected state uses solid primaryContainer fill, zero glow, zero blur, zero translucency.
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

    SingleChoiceSegmentedButtonRow(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        items.forEachIndexed { index, item ->
            val isSelected = index == selectedIndex
            SegmentedButton(
                selected = isSelected,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onItemSelected(index)
                },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = items.size
                ),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    activeBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    inactiveBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                icon = {},
                modifier = Modifier
                    .weight(1f)
                    .testTag(item.testTag)
            ) {
                Text(
                    text = item.title,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}
