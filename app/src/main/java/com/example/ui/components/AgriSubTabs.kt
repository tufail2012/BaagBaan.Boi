package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.getSectionAccentColor

/**
 * Pruning Sub-Tabs (Summer Pruning / Winter Pruning)
 * Styled with standard Compose Material 3 SegmentedButton with solid fills and clean M3 typography.
 */
@Composable
fun PruningSubTabs(
    selectedSubTab: String,
    onSelectSubTab: (String) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = getSectionAccentColor("Pruning"),
    hazeState: Any? = null
) {
    val subTabs = listOf("Summer Pruning", "Winter Pruning")
    val selectedIndex = if (selectedSubTab.contains("Winter", ignoreCase = true)) 1 else 0
    val haptic = LocalHapticFeedback.current

    SingleChoiceSegmentedButtonRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .height(44.dp)
    ) {
        subTabs.forEachIndexed { index, tabName ->
            val isSelected = index == selectedIndex
            val isSummer = tabName.contains("Summer", ignoreCase = true)
            val icon = if (isSummer) Icons.Default.WbSunny else Icons.Default.AcUnit

            SegmentedButton(
                selected = isSelected,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSelectSubTab(tabName)
                },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = subTabs.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    activeBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    inactiveBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("subtab_${tabName.lowercase().replace(" ", "_")}")
            ) {
                Text(
                    text = tabName,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Rootstock Sub-Tabs (M9-T337, MM111, Geneva dropdown)
 * Styled with standard Compose Material 3 SegmentedButton with solid fills.
 */
@Composable
fun RootstockSubTabs(
    selectedSubTab: String,
    selectedGenevaOption: String?,
    onSelectSubTab: (String, String?) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = getSectionAccentColor("Rootstocks"),
    hazeState: Any? = null
) {
    var genevaMenuExpanded by remember { mutableStateOf(false) }
    val genevaOptions = listOf("G41", "G214", "G11", "G35", "G969", "G890")
    val haptic = LocalHapticFeedback.current

    val isGenevaSelected = selectedSubTab.startsWith("Geneva") || genevaOptions.contains(selectedSubTab)
    val selectedIndex = when {
        selectedSubTab.equals("MM111", ignoreCase = true) -> 1
        isGenevaSelected -> 2
        else -> 0 // M9-T337
    }

    val activeGenevaLabel = if (selectedGenevaOption != null) {
        "Geneva ($selectedGenevaOption)"
    } else {
        "Geneva"
    }

    SingleChoiceSegmentedButtonRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .height(44.dp)
    ) {
        // 1. M9-T337
        SegmentedButton(
            selected = selectedIndex == 0,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onSelectSubTab("M9-T337", null)
            },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
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
                .testTag("subtab_m9_t337")
        ) {
            Text(
                text = "M9-T337",
                fontSize = 12.sp,
                fontWeight = if (selectedIndex == 0) FontWeight.Bold else FontWeight.Medium
            )
        }

        // 2. MM111
        SegmentedButton(
            selected = selectedIndex == 1,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onSelectSubTab("MM111", null)
            },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
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
                .testTag("subtab_mm111")
        ) {
            Text(
                text = "MM111",
                fontSize = 12.sp,
                fontWeight = if (selectedIndex == 1) FontWeight.Bold else FontWeight.Medium
            )
        }

        // 3. Geneva Dropdown
        SegmentedButton(
            selected = selectedIndex == 2,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                genevaMenuExpanded = true
            },
            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
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
                .weight(1.2f)
                .testTag("subtab_geneva")
        ) {
            Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = activeGenevaLabel,
                        fontSize = 12.sp,
                        fontWeight = if (selectedIndex == 2) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Expand Geneva Menu",
                        modifier = Modifier.size(16.dp)
                    )
                }

                DropdownMenu(
                    expanded = genevaMenuExpanded,
                    onDismissRequest = { genevaMenuExpanded = false }
                ) {
                    Text(
                        text = "Geneva Rootstocks",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )

                    genevaOptions.forEach { option ->
                        val isOptionSelected = selectedGenevaOption == option && isGenevaSelected
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = option,
                                        fontWeight = if (isOptionSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isOptionSelected) accentColor else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isOptionSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = accentColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                genevaMenuExpanded = false
                                onSelectSubTab("Geneva", option)
                            }
                        )
                    }
                }
            }
        }
    }
}
