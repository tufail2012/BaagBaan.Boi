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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.ui.theme.getSectionAccentColor

/**
 * Pruning Sub-Tabs (Summer Pruning / Winter Pruning)
 * Features:
 * - Floating pill container with smooth backdrop elevation.
 * - Sliding 3D Bubble / Droplet Indicator with spring and wobble physics.
 * - High-contrast theme-aware iconography and labels.
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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .height(46.dp)
            .shadow(
                elevation = 3.dp,
                shape = containerShape,
                spotColor = if (isDark) Color.Black.copy(alpha = 0.35f) else Color(0x20000000),
                ambientColor = if (isDark) Color.Black.copy(alpha = 0.15f) else Color(0x10000000)
            )
            .clip(containerShape)
            .background(containerBgColor)
            .border(BorderStroke(1.dp, containerBorderColor), containerShape)
            .padding(3.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val totalWidth = maxWidth
            val slotWidth = totalWidth / 2
            val targetOffset = slotWidth * selectedIndex

            val animatedOffsetX by animateDpAsState(
                targetValue = targetOffset,
                animationSpec = spring(
                    dampingRatio = 0.62f,
                    stiffness = 380f
                ),
                label = "pruningSlide"
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

            // Tab Text and Icons Row
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                subTabs.forEachIndexed { index, tabName ->
                    val isSelected = index == selectedIndex
                    val isSummer = tabName.contains("Summer", ignoreCase = true)
                    val icon = if (isSummer) Icons.Default.WbSunny else Icons.Default.AcUnit

                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            accentColor
                        } else {
                            if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        },
                        animationSpec = tween(durationMillis = 200),
                        label = "pruningTabColor"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .testTag("subtab_${tabName.lowercase().replace(" ", "_")}")
                            .clip(itemShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    if (selectedIndex != index) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSelectSubTab(tabName)
                                    }
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = contentColor,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tabName,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = contentColor
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Rootstock Sub-Tabs (M9-T337, MM111, Geneva dropdown)
 * Features:
 * - Floating pill container with smooth backdrop elevation.
 * - Sliding 3D Bubble / Droplet Indicator with spring and wobble physics.
 * - Dropdown picker for Geneva variants with selection indicator.
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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .height(46.dp)
            .shadow(
                elevation = 3.dp,
                shape = containerShape,
                spotColor = if (isDark) Color.Black.copy(alpha = 0.35f) else Color(0x20000000),
                ambientColor = if (isDark) Color.Black.copy(alpha = 0.15f) else Color(0x10000000)
            )
            .clip(containerShape)
            .background(containerBgColor)
            .border(BorderStroke(1.dp, containerBorderColor), containerShape)
            .padding(3.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val totalWidth = maxWidth
            val totalWeight = 3.2f
            val width0 = totalWidth * (1.0f / totalWeight)
            val width1 = totalWidth * (1.0f / totalWeight)
            val width2 = totalWidth * (1.2f / totalWeight)

            val targetOffset = when (selectedIndex) {
                0 -> 0.dp
                1 -> width0
                else -> width0 + width1
            }

            val targetWidth = when (selectedIndex) {
                0 -> width0
                1 -> width1
                else -> width2
            }

            val animatedOffsetX by animateDpAsState(
                targetValue = targetOffset,
                animationSpec = spring(
                    dampingRatio = 0.62f,
                    stiffness = 380f
                ),
                label = "rootstockSlide"
            )

            val animatedWidth by animateDpAsState(
                targetValue = targetWidth,
                animationSpec = spring(
                    dampingRatio = 0.62f,
                    stiffness = 380f
                ),
                label = "rootstockWidth"
            )

            // Sliding 3D Bubble / Droplet Indicator
            Box(
                modifier = Modifier
                    .offset(x = animatedOffsetX + wobbleOffset.value.dp)
                    .align(Alignment.CenterStart)
                    .width(animatedWidth)
                    .fillMaxHeight()
                    .bubbleDropletPillIndicator(
                        shape = itemShape,
                        accentColor = accentColor,
                        isDark = isDark,
                        isAmoled = isAmoled
                    )
            )

            // Sub-Tab Options Row
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. M9-T337
                val isM9Selected = selectedIndex == 0
                val m9Color by animateColorAsState(
                    targetValue = if (isM9Selected) accentColor else if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                    label = "m9Color"
                )
                Box(
                    modifier = Modifier
                        .weight(1.0f)
                        .fillMaxHeight()
                        .testTag("subtab_m9_t337")
                        .clip(itemShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                if (selectedIndex != 0) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelectSubTab("M9-T337", null)
                                }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "M9-T337",
                        fontSize = 12.sp,
                        fontWeight = if (isM9Selected) FontWeight.Bold else FontWeight.Medium,
                        color = m9Color
                    )
                }

                // 2. MM111
                val isMM111Selected = selectedIndex == 1
                val mm111Color by animateColorAsState(
                    targetValue = if (isMM111Selected) accentColor else if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                    label = "mm111Color"
                )
                Box(
                    modifier = Modifier
                        .weight(1.0f)
                        .fillMaxHeight()
                        .testTag("subtab_mm111")
                        .clip(itemShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                if (selectedIndex != 1) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelectSubTab("MM111", null)
                                }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "MM111",
                        fontSize = 12.sp,
                        fontWeight = if (isMM111Selected) FontWeight.Bold else FontWeight.Medium,
                        color = mm111Color
                    )
                }

                // 3. Geneva Dropdown
                val isGenevaActive = selectedIndex == 2
                val genevaColor by animateColorAsState(
                    targetValue = if (isGenevaActive) accentColor else if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                    label = "genevaColor"
                )
                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight()
                        .testTag("subtab_geneva")
                        .clip(itemShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                genevaMenuExpanded = true
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = activeGenevaLabel,
                            fontSize = 12.sp,
                            fontWeight = if (isGenevaActive) FontWeight.Bold else FontWeight.Medium,
                            color = genevaColor,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Expand Geneva Menu",
                            tint = genevaColor,
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
}
