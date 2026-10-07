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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.getSectionAccentColor
import com.kyant.backdrop.Backdrop
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/**
 * Pruning Sub-Tabs (Summer Pruning / Winter Pruning)
 * Features:
 * - Floating pill container with smooth backdrop elevation.
 * - Sliding 3D Bubbly Glass Capsule Indicator with spring and wobble physics.
 * - High-contrast theme-aware iconography and labels dynamically matching palette.
 */
@Composable
fun PruningSubTabs(
    selectedSubTab: String,
    onSelectSubTab: (String) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    hazeState: HazeState? = null,
    backdrop: Backdrop? = null
) {
    val indicatorAccent = com.example.ui.theme.paletteSecondaryAccent()
    val subTabs = listOf("Summer Pruning", "Winter Pruning")
    val selectedIndex = if (selectedSubTab.contains("Winter", ignoreCase = true)) 1 else 0
    val haptic = LocalHapticFeedback.current
    val isDark = isAppInDarkMode()
    val isAmoled = isAppInAmoledMode()
    val scrollGlass = LocalScrollGlassSource.current
    val glassBackdrop: Backdrop? = scrollGlass?.combined ?: backdrop
    val useRealGlass = glassBackdrop != null && isGlassSupported()
    val realGlassIndicatorColor = glassIndicatorColor().copy(alpha = 0.5f)

    val containerShape = RoundedCornerShape(percent = 50)
    val itemShape = RoundedCornerShape(percent = 50)

    // Track Background: rgba(255, 255, 255, 0.08); border: 1px solid rgba(255, 255, 255, 0.12); border-radius: 9999px;
    val containerBgBrush = if (isDark || isAmoled) {
        SolidColor(Color.White.copy(alpha = 0.08f))
    } else {
        // rgba(255, 255, 255, 0.5) with backdrop-filter: blur(10px) and subtle theme tint
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.55f),
                accentColor.copy(alpha = 0.04f),
                Color.White.copy(alpha = 0.50f)
            )
        )
    }

    val containerBorderBrush = if (isDark || isAmoled) {
        SolidColor(Color.White.copy(alpha = 0.12f))
    } else {
        SolidColor(Color(0xFF000000).copy(alpha = 0.08f))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .height(48.dp)
            .then(
                if (useRealGlass) {
                    Modifier.recordsLiquidGlass(
                        backdrop = glassBackdrop,
                        shape = containerShape
                    )
                } else {
                    Modifier
                        .shadow(
                            elevation = 3.dp,
                            shape = containerShape,
                            spotColor = Color.Black.copy(alpha = if (isDark) 0.12f else 0.04f),
                            ambientColor = Color.Black.copy(alpha = if (isDark) 0.06f else 0.02f)
                        )
                        .clip(containerShape)
                        .then(
                            if (hazeState != null) {
                                Modifier.hazeEffect(
                                    state = hazeState,
                                    style = HazeStyle(
                                        blurRadius = 10.dp,
                                        tints = listOf(
                                            HazeTint(color = accentColor.copy(alpha = if (isDark) 0.08f else 0.04f))
                                        ),
                                        backgroundColor = Color.Transparent
                                    )
                                )
                            } else Modifier
                        )
                        .background(containerBgBrush, shape = containerShape)
                        .border(BorderStroke(1.dp, containerBorderBrush), containerShape)
                }
            )
            .padding(4.dp)
    ) {
        val activeTextColor = accentColor
        val inactiveTextColor = if (isDark || isAmoled) Color.White.copy(alpha = 0.60f) else Color.Black.copy(alpha = 0.60f)

        AgriMotionTabs(
            titles = subTabs,
            selectedIndex = selectedIndex,
            onSelect = { index -> onSelectSubTab(subTabs[index]) },
            selectedTextColor = activeTextColor,
            unselectedTextColor = inactiveTextColor,
            testTags = subTabs.map { "subtab_${it.lowercase().replace(" ", "_")}" },
            pillInset = 4.dp,
            pill = { pillModifier ->
                Box(
                    modifier = pillModifier.then(
                        if (useRealGlass) {
                            Modifier
                                .clip(itemShape)
                                .background(realGlassIndicatorColor, itemShape)
                        } else {
                            Modifier.bubblyGlassCapsuleIndicator(
                                hazeState = hazeState,
                                shape = itemShape,
                                accentColor = indicatorAccent,
                                isDark = isDark,
                                isAmoled = isAmoled
                            )
                        }
                    )
                )
            },
            tabContent = { index, isSelected, textColor, scale ->
                val tabName = subTabs[index]
                val isSummer = tabName.contains("Summer", ignoreCase = true)
                val icon = if (isSummer) Icons.Default.WbSunny else Icons.Default.AcUnit
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = tabName,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor
                    )
                }
            }
        )
    }
}

/**
 * Rootstock Sub-Tabs (M9-T337, MM111, Geneva dropdown)
 * Features:
 * - Floating translucent pill container strip with light background tint and blur.
 * - Sliding 3D Bubbly Glass Capsule Indicator with spring and wobble physics.
 * - Dynamic theme color harmonization (#D32F2F in coral/red, etc.).
 * - Dropdown picker for Geneva variants with selection indicator.
 */
@Composable
fun RootstockSubTabs(
    selectedSubTab: String,
    selectedGenevaOption: String?,
    onSelectSubTab: (String, String?) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    hazeState: HazeState? = null,
    backdrop: Backdrop? = null
) {
    val indicatorAccent = com.example.ui.theme.paletteSecondaryAccent()
    var genevaMenuExpanded by remember { mutableStateOf(false) }
    val genevaOptions = listOf("G41", "G214", "G11", "G35", "G969", "G890")
    val haptic = LocalHapticFeedback.current
    val isDark = isAppInDarkMode()
    val isAmoled = isAppInAmoledMode()
    val scrollGlass = LocalScrollGlassSource.current
    val glassBackdrop: Backdrop? = scrollGlass?.combined ?: backdrop
    val useRealGlass = glassBackdrop != null && isGlassSupported()
    val realGlassIndicatorColor = glassIndicatorColor().copy(alpha = 0.5f)

    val containerShape = RoundedCornerShape(percent = 50)
    val itemShape = RoundedCornerShape(percent = 50)

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

    // Track Background: rgba(255, 255, 255, 0.08); border: 1px solid rgba(255, 255, 255, 0.12); border-radius: 9999px;
    val containerBgBrush = if (isDark || isAmoled) {
        SolidColor(Color.White.copy(alpha = 0.08f))
    } else {
        // rgba(255, 255, 255, 0.5) with backdrop-filter: blur(10px) and subtle theme tint
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.55f),
                accentColor.copy(alpha = 0.04f),
                Color.White.copy(alpha = 0.50f)
            )
        )
    }

    val containerBorderBrush = if (isDark || isAmoled) {
        SolidColor(Color.White.copy(alpha = 0.12f))
    } else {
        SolidColor(Color(0xFF000000).copy(alpha = 0.08f))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .height(48.dp)
            .then(
                if (useRealGlass) {
                    Modifier.recordsLiquidGlass(
                        backdrop = glassBackdrop,
                        shape = containerShape
                    )
                } else {
                    Modifier
                        .shadow(
                            elevation = 3.dp,
                            shape = containerShape,
                            spotColor = Color.Black.copy(alpha = if (isDark) 0.12f else 0.04f),
                            ambientColor = Color.Black.copy(alpha = if (isDark) 0.06f else 0.02f)
                        )
                        .clip(containerShape)
                        .then(
                            if (hazeState != null) {
                                Modifier.hazeEffect(
                                    state = hazeState,
                                    style = HazeStyle(
                                        blurRadius = 10.dp,
                                        tints = listOf(
                                            HazeTint(color = accentColor.copy(alpha = if (isDark) 0.08f else 0.04f))
                                        ),
                                        backgroundColor = Color.Transparent
                                    )
                                )
                            } else Modifier
                        )
                        .background(containerBgBrush, shape = containerShape)
                        .border(BorderStroke(1.dp, containerBorderBrush), containerShape)
                }
            )
            .padding(4.dp)
    ) {
        val rootstockTitles = listOf("M9-T337", "MM111", activeGenevaLabel)
        val activeTextColor = accentColor
        val inactiveTextColor = if (isDark || isAmoled) Color.White.copy(alpha = 0.60f) else Color.Black.copy(alpha = 0.60f)

        AgriMotionTabs(
            titles = rootstockTitles,
            selectedIndex = selectedIndex,
            onSelect = { index ->
                when (index) {
                    0 -> onSelectSubTab("M9-T337", null)
                    1 -> onSelectSubTab("MM111", null)
                    2 -> genevaMenuExpanded = true
                }
            },
            selectedTextColor = activeTextColor,
            unselectedTextColor = inactiveTextColor,
            testTags = listOf("subtab_m9_t337", "subtab_mm111", "subtab_geneva"),
            pillInset = 4.dp,
            pill = { pillModifier ->
                Box(
                    modifier = pillModifier.then(
                        if (useRealGlass) {
                            Modifier
                                .clip(itemShape)
                                .background(realGlassIndicatorColor, itemShape)
                        } else {
                            Modifier.bubblyGlassCapsuleIndicator(
                                hazeState = hazeState,
                                shape = itemShape,
                                accentColor = indicatorAccent,
                                isDark = isDark,
                                isAmoled = isAmoled
                            )
                        }
                    )
                )
            },
            tabContent = { index, isSelected, textColor, scale ->
                if (index == 2) {
                    // Geneva Dropdown tab
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                    ) {
                        Text(
                            text = activeGenevaLabel,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = textColor,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Expand Geneva Menu",
                            tint = textColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    val genevaMenuShape = RoundedCornerShape(20.dp)
                    DropdownMenu(
                        expanded = genevaMenuExpanded,
                        onDismissRequest = { genevaMenuExpanded = false },
                        shape = genevaMenuShape,
                        containerColor = Color.Transparent,
                        border = null,
                        shadowElevation = 0.dp,
                        tonalElevation = 0.dp,
                        modifier = Modifier.widthIn(min = 200.dp).dropdownLiquidGlass(hazeState = hazeState, shape = genevaMenuShape)
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
                } else {
                    Text(
                        text = rootstockTitles[index],
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor,
                        modifier = Modifier.graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                    )
                }
            }
        )
    }
}
