package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
 * Features:
 * - Translucent, blurred glass backdrop container utilizing Haze backdrop effects.
 * - Prominent, larger floating pill indicator with a raised, bubbly, 3D appearance.
 * - Semi-transparent, barely visible water droplet aesthetic with minimal color tint and specular refraction.
 * - High-contrast unselected navigation icons with haptic feedback.
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
    val isAmoled = isAppInAmoledMode()

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

    // Translucent blurred glass container background
    val containerBgColor = when {
        isAmoled -> Color(0xFF000000).copy(alpha = if (hazeState != null) 0.50f else 0.88f)
        isDark -> Color(0xFF0F172A).copy(alpha = if (hazeState != null) 0.45f else 0.85f)
        else -> Color(0xFFFFFFFF).copy(alpha = if (hazeState != null) 0.42f else 0.85f)
    }

    val containerBorder = BorderStroke(
        width = 1.dp,
        color = if (isDark) Color(0xFFFFFFFF).copy(alpha = 0.16f) else Color(0xFF000000).copy(alpha = 0.08f)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        // Floating pill container box (Height: 68.dp for comfortable breathing room)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp),
            contentAlignment = Alignment.Center
        ) {
            // Layer 1: Translucent Blurred Glass Pill Container
            val hazeModifier = if (hazeState != null) {
                Modifier.hazeEffect(
                    state = hazeState,
                    style = HazeStyle(
                        backgroundColor = surfaceColor,
                        tint = HazeTint(containerBgColor),
                        blurRadius = 32.dp,
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

            // Layer 2: Interactive Tabs with 3D Bubbly Water Droplet Indicator
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp),
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
 * Individual navigation tab item featuring a raised, 3D bubbly water droplet indicator when selected.
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
    val dropletPillShape = RoundedCornerShape(percent = 50)

    val unselectedIconColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF0F172A)
    val selectedIconColor = if (isDark) {
        accentColor.copy(alpha = 0.95f)
    } else {
        accentColor.copy(alpha = 0.90f)
    }

    Box(
        modifier = modifier
            .testTag(item.testTag)
            .height(60.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            // Raised 3D Bubbly Water Droplet Floating Pill (Enlarged to 54.dp x 48.dp)
            Box(
                modifier = Modifier
                    .width(54.dp)
                    .height(48.dp)
                    // 1. 3D Bottom Depth Shadow
                    .shadow(
                        elevation = 6.dp,
                        shape = dropletPillShape,
                        spotColor = if (isDark) Color.Black.copy(alpha = 0.60f) else Color(0x35000000),
                        ambientColor = if (isDark) Color.Black.copy(alpha = 0.35f) else Color(0x20000000)
                    )
                    .clip(dropletPillShape)
                    // 2. Ultra-translucent, barely visible liquid glass body with minimal color tint
                    .background(
                        Brush.verticalGradient(
                            colors = if (isDark) {
                                listOf(
                                    Color.White.copy(alpha = 0.18f),
                                    accentColor.copy(alpha = 0.06f),
                                    Color.White.copy(alpha = 0.04f),
                                    Color.White.copy(alpha = 0.10f)
                                )
                            } else {
                                listOf(
                                    Color.White.copy(alpha = 0.60f),
                                    accentColor.copy(alpha = 0.06f),
                                    Color.White.copy(alpha = 0.12f),
                                    Color.White.copy(alpha = 0.35f)
                                )
                            }
                        )
                    )
                    // 3. Specular 3D liquid meniscus highlight & reflection arcs
                    .drawWithContent {
                        drawContent()

                        val w = size.width
                        val h = size.height

                        // Upper Specular Highlight (Curved dome reflection of light on droplet crest)
                        val highlightHeight = h * 0.40f
                        val highlightWidth = w * 0.72f
                        val highlightX = (w - highlightWidth) / 2f
                        val highlightY = 2.dp.toPx()

                        drawOval(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (isDark) 0.50f else 0.85f),
                                    Color.White.copy(alpha = if (isDark) 0.15f else 0.30f),
                                    Color.Transparent
                                ),
                                startY = highlightY,
                                endY = highlightY + highlightHeight
                            ),
                            topLeft = Offset(highlightX, highlightY),
                            size = Size(highlightWidth, highlightHeight)
                        )

                        // Bottom ambient light bounce arc (gives 3D spherical liquid volume)
                        val bottomReflectHeight = h * 0.22f
                        val bottomReflectWidth = w * 0.55f
                        val bottomX = (w - bottomReflectWidth) / 2f
                        val bottomY = h - bottomReflectHeight - 2.dp.toPx()

                        drawOval(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = if (isDark) 0.20f else 0.40f)
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
                            width = 1.2.dp,
                            brush = Brush.verticalGradient(
                                colors = if (isDark) {
                                    listOf(
                                        Color.White.copy(alpha = 0.55f),
                                        Color.White.copy(alpha = 0.12f),
                                        Color.White.copy(alpha = 0.28f)
                                    )
                                } else {
                                    listOf(
                                        Color.White.copy(alpha = 0.90f),
                                        Color.White.copy(alpha = 0.25f),
                                        Color.White.copy(alpha = 0.55f)
                                    )
                                }
                            )
                        ),
                        shape = dropletPillShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = selectedIconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        } else {
            // Unselected Tab: crisp high-contrast icon
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = unselectedIconColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}



