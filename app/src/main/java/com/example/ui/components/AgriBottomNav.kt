package com.example.ui.components

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.getSectionAccentColor
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

data class AgriNavItem(
    val title: String,
    val serviceCategory: String,
    val icon: ImageVector,
    val testTag: String
)

/**
 * Floating Pill Bottom Navigation Bar for Baagbaan BOI.
 * Features a floating rounded pill container with real backdrop blur via Haze (and solid fallback),
 * icon-only tabs (no labels), a glossy water-droplet styled selected tab indicator with specular highlight,
 * and high-contrast pure black/pure white unselected icons.
 */
@Composable
fun AgriBottomNav(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color? = null,
    hazeState: HazeState? = null
) {
    val navItems = remember {
        listOf(
            AgriNavItem("Local", "Local Plants", Icons.Outlined.LocalFlorist, "nav_local"),
            AgriNavItem("Imported", "Imported", Icons.Default.LocalShipping, "nav_imported"),
            AgriNavItem("Rootstocks", "Rootstocks", Icons.Default.Spa, "nav_rootstocks"),
            AgriNavItem("Site Visit", "Site Visit", Icons.Outlined.Assignment, "nav_site_visit"),
            AgriNavItem("Pruning", "Pruning", Icons.Default.ContentCut, "nav_pruning"),
            AgriNavItem("Garden", "Garden Planning", Icons.Default.Park, "nav_garden_planning")
        )
    }

    val haptic = LocalHapticFeedback.current
    val isDark = isAppInDarkMode()

    val selectedIndex = remember(selectedCategory) {
        val idx = navItems.indexOfFirst { item ->
            selectedCategory.equals(item.serviceCategory, ignoreCase = true) ||
                    (selectedCategory.equals("Local", ignoreCase = true) && item.serviceCategory.equals("Local Plants", ignoreCase = true)) ||
                    (selectedCategory.equals("Garden", ignoreCase = true) && item.serviceCategory.equals("Garden Planning", ignoreCase = true))
        }
        if (idx >= 0) idx else 0
    }

    val activeSectionAccent = remember(selectedCategory, accentColor) {
        accentColor ?: getSectionAccentColor(selectedCategory)
    }

    val containerShape = RoundedCornerShape(percent = 50)
    val surfaceColor = MaterialTheme.colorScheme.surface

    // Translucent pill container background: strong frosted blur translucency allowing content behind to show as soft shades
    val containerBgColor = if (isDark) {
        if (hazeState != null) Color(0xFF0F172A).copy(alpha = 0.52f)
        else surfaceColor.copy(alpha = 0.95f)
    } else {
        if (hazeState != null) Color(0xFFFFFFFF).copy(alpha = 0.50f)
        else surfaceColor.copy(alpha = 0.95f)
    }

    val containerBorder = BorderStroke(
        width = 1.dp,
        color = if (isDark) Color(0xFFFFFFFF).copy(alpha = 0.18f) else Color(0xFF000000).copy(alpha = 0.08f)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        // Floating pill container box (Height: 66.dp for comfortable breathing room)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp),
            contentAlignment = Alignment.Center
        ) {
            // Layer 1: Background Glass Pill (Strong blur effect with translucent glass surface)
            val hazeModifier = if (hazeState != null) {
                Modifier.hazeEffect(
                    state = hazeState,
                    style = HazeStyle(
                        backgroundColor = surfaceColor,
                        tint = HazeTint(containerBgColor),
                        blurRadius = 30.dp,
                        noiseFactor = 0.02f
                    )
                )
            } else {
                Modifier.background(containerBgColor)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .shadow(
                        elevation = 8.dp,
                        shape = containerShape,
                        spotColor = if (isDark) Color.Black.copy(alpha = 0.45f) else Color(0x30000000),
                        ambientColor = if (isDark) Color.Black.copy(alpha = 0.25f) else Color(0x18000000)
                    )
                    .clip(containerShape)
                    .then(hazeModifier)
                    .border(containerBorder, containerShape)
            )

            // Layer 2: Foreground Icons and Selected Pill (Crisp, sharp, zero blur)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                navItems.forEachIndexed { index, item ->
                    val isSelected = index == selectedIndex
                    AgriTabItem(
                        item = item,
                        isSelected = isSelected,
                        accentColor = activeSectionAccent,
                        isDark = isDark,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onCategorySelected(item.serviceCategory)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Individual icon-only navigation tab with a well-defined, small, soft-toned pill shape when selected.
 */
@Composable
private fun AgriTabItem(
    item: AgriNavItem,
    isSelected: Boolean,
    accentColor: Color,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Symmetrical, well-defined small pill shape
    val selectedPillShape = RoundedCornerShape(percent = 50)

    // Pure black in light theme, pure white in dark/AMOLED theme with no grey/muted tones
    val unselectedIconColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000)
    val selectedIconColor = accentColor

    Box(
        modifier = modifier
            .testTag(item.testTag)
            .height(58.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            // Selected item: Well-defined, small, soft-toned pill shape matching blurred aesthetic
            Box(
                modifier = Modifier
                    .width(46.dp)
                    .height(36.dp)
                    .shadow(
                        elevation = 4.dp,
                        shape = selectedPillShape,
                        spotColor = accentColor.copy(alpha = if (isDark) 0.40f else 0.30f),
                        ambientColor = accentColor.copy(alpha = if (isDark) 0.20f else 0.15f)
                    )
                    .clip(selectedPillShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                accentColor.copy(alpha = if (isDark) 0.28f else 0.20f),
                                accentColor.copy(alpha = if (isDark) 0.16f else 0.12f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = accentColor.copy(alpha = if (isDark) 0.50f else 0.35f),
                        shape = selectedPillShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = selectedIconColor,
                    modifier = Modifier.size(23.dp)
                )
            }
        } else {
            // Unselected tab: icon only, pure solid high-contrast color
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = unselectedIconColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}


