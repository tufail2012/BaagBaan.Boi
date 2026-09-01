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

    // Floating pill container background: real blurred translucency when haze is active, solid semi-opaque fallback
    val containerBgColor = if (isDark) {
        if (hazeState != null) surfaceColor.copy(alpha = 0.65f)
        else surfaceColor.copy(alpha = 0.95f)
    } else {
        if (hazeState != null) surfaceColor.copy(alpha = 0.70f)
        else surfaceColor.copy(alpha = 0.95f)
    }

    val containerBorder = BorderStroke(
        width = 1.dp,
        color = if (isDark) Color(0xFFFFFFFF).copy(alpha = 0.16f) else Color(0xFF000000).copy(alpha = 0.09f)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        // Floating pill container box (Height increased to 66.dp for better breathing room)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp),
            contentAlignment = Alignment.Center
        ) {
            // Layer 1: Background Glass Pill (Isolated background layer with real backdrop blur)
            val hazeModifier = if (hazeState != null) {
                Modifier.hazeEffect(
                    state = hazeState,
                    style = HazeStyle(
                        backgroundColor = surfaceColor,
                        tint = HazeTint(containerBgColor),
                        blurRadius = 24.dp,
                        noiseFactor = 0.05f
                    )
                )
            } else {
                Modifier.background(containerBgColor)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .shadow(
                        elevation = 10.dp,
                        shape = containerShape,
                        spotColor = if (isDark) Color.Black.copy(alpha = 0.5f) else Color(0x35000000),
                        ambientColor = if (isDark) Color.Black.copy(alpha = 0.3f) else Color(0x20000000)
                    )
                    .clip(containerShape)
                    .then(hazeModifier)
                    .border(containerBorder, containerShape)
            )

            // Layer 2: Foreground Icons and Selected Droplet Pill (Crisp, sharp, zero blur)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                navItems.forEachIndexed { index, item ->
                    val isSelected = index == selectedIndex
                    DropletTabItem(
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
 * Individual icon-only navigation tab with glossy water-droplet pill highlight when selected.
 */
@Composable
private fun DropletTabItem(
    item: AgriNavItem,
    isSelected: Boolean,
    accentColor: Color,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dropletShape = RoundedCornerShape(
        topStart = 22.dp,
        topEnd = 16.dp,
        bottomStart = 16.dp,
        bottomEnd = 22.dp
    )

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
            // Selected item: Clearly visible Water droplet pill highlight sized to the icon
            Box(
                modifier = Modifier
                    .width(50.dp)
                    .height(44.dp)
                    .shadow(
                        elevation = 6.dp,
                        shape = dropletShape,
                        spotColor = accentColor.copy(alpha = 0.55f),
                        ambientColor = accentColor.copy(alpha = 0.30f)
                    )
                    .clip(dropletShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                accentColor.copy(alpha = if (isDark) 0.48f else 0.38f),
                                accentColor.copy(alpha = if (isDark) 0.28f else 0.20f)
                            )
                        )
                    )
                    .border(
                        width = 1.5.dp,
                        color = accentColor.copy(alpha = if (isDark) 0.65f else 0.50f),
                        shape = dropletShape
                    )
                    .drawWithContent {
                        drawContent()
                        // Specular highlight: soft white reflection arc near the top-left of the droplet
                        val highlightBrush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (isDark) 0.85f else 0.95f),
                                Color.White.copy(alpha = 0.35f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.28f, size.height * 0.24f),
                            radius = size.width * 0.40f
                        )
                        drawCircle(
                            brush = highlightBrush,
                            radius = size.width * 0.32f,
                            center = Offset(size.width * 0.28f, size.height * 0.24f)
                        )
                        // Soft top-left curved specular arc mimicking light glint on droplet
                        drawArc(
                            color = Color.White.copy(alpha = if (isDark) 0.75f else 0.90f),
                            startAngle = 180f,
                            sweepAngle = 105f,
                            useCenter = false,
                            topLeft = Offset(2.5f, 2.5f),
                            size = Size(size.width - 5f, size.height - 5f),
                            style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = selectedIconColor,
                    modifier = Modifier.size(25.dp)
                )
            }
        } else {
            // Unselected tab: icon only, pure solid color (pure black in light, pure white in dark)
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = unselectedIconColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}


