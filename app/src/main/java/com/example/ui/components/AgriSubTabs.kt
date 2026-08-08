package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PruningSubTabs(
    selectedSubTab: String,
    onSelectSubTab: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val subTabs = listOf("Summer Pruning", "Winter Pruning")

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            subTabs.forEach { tabName ->
                val isSelected = selectedSubTab.equals(tabName, ignoreCase = true)
                val isSummer = tabName.contains("Summer", ignoreCase = true)
                val icon = if (isSummer) Icons.Default.WbSunny else Icons.Default.AcUnit
                val iconTint = if (isSummer) {
                    if (isSelected) Color(0xFFFF8F00) else Color(0xFFE65100)
                } else {
                    if (isSelected) Color(0xFF00ACC1) else Color(0xFF0288D1)
                }

                SubTabItem(
                    title = tabName,
                    icon = icon,
                    iconTint = iconTint,
                    isSelected = isSelected,
                    onClick = { onSelectSubTab(tabName) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("subtab_${tabName.lowercase().replace(" ", "_")}")
                )
            }
        }
    }
}

@Composable
fun RootstockSubTabs(
    selectedSubTab: String,
    selectedGenevaOption: String?,
    onSelectSubTab: (String, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var genevaMenuExpanded by remember { mutableStateOf(false) }
    val genevaOptions = listOf("G41", "G214", "G11", "G35", "G969", "G890")
    val haptic = LocalHapticFeedback.current

    val isGenevaSelected = selectedSubTab.startsWith("Geneva") || genevaOptions.contains(selectedSubTab)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // M9-T337 Tab
            val isM9Selected = selectedSubTab.equals("M9-T337", ignoreCase = true)
            SubTabItem(
                title = "M9-T337",
                isSelected = isM9Selected,
                onClick = { onSelectSubTab("M9-T337", null) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("subtab_m9_t337")
            )

            // MM111 Tab
            val isMM111Selected = selectedSubTab.equals("MM111", ignoreCase = true)
            SubTabItem(
                title = "MM111",
                isSelected = isMM111Selected,
                onClick = { onSelectSubTab("MM111", null) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("subtab_mm111")
            )

            // Geneva Dropdown Tab
            Box(
                modifier = Modifier.weight(1.25f)
            ) {
                val activeGenevaLabel = if (selectedGenevaOption != null) {
                    "Geneva ($selectedGenevaOption)"
                } else {
                    "Geneva"
                }

                SubTabItem(
                    title = activeGenevaLabel,
                    isSelected = isGenevaSelected,
                    hasDropdown = true,
                    onClick = {
                        genevaMenuExpanded = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("subtab_geneva")
                )

                DropdownMenu(
                    expanded = genevaMenuExpanded,
                    onDismissRequest = { genevaMenuExpanded = false }
                ) {
                    Text(
                        text = "Geneva Rootstocks",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
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
                                        color = if (isOptionSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isOptionSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary,
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

@Composable
private fun SubTabItem(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconTint: Color? = null,
    hasDropdown: Boolean = false
) {
    val haptic = LocalHapticFeedback.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
        label = "subtab_bg"
    )

    val textColor by animateColorAsState(
        targetValue = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "subtab_text"
    )

    val itemShape = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            .height(38.dp)
            .clip(itemShape)
            .background(backgroundColor)
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 1.dp,
                        color = primaryColor.copy(alpha = 0.35f),
                        shape = itemShape
                    )
                } else Modifier
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 6.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint ?: textColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = title,
                fontSize = 12.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = textColor,
                maxLines = 1
            )
            if (hasDropdown) {
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Expand Geneva Menu",
                    tint = textColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
