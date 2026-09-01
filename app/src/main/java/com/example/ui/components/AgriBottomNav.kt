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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.getSectionAccentColor

data class AgriNavItem(
    val title: String,
    val serviceCategory: String,
    val icon: ImageVector,
    val testTag: String
)

/**
 * Floating Pill Bottom Navigation Bar for Baagbaan BOI.
 * Features a floating rounded pill container with backdrop blur (API 31+) or solid semi-opaque fallback,
 * icon-only tabs (no labels), a glossy water-droplet styled selected tab indicator with specular highlight,
 * and high-contrast pure black/pure white unselected icons.
 */
@Composable
fun AgriBottomNav(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color? = null,
    hazeState: Any? = null
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
    val isApi31Plus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

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

    // Floating pill container background: real blurred translucency for API 31+, solid semi-opaque for older OS
    val containerBgColor = if (isDark) {
        if (isApi31Plus) Color(0xFF0F172A).copy(alpha = 0.70f)
        else Color(0xFF1E293B).copy(alpha = 0.96f)
    } else {
        if (isApi31Plus) Color(0xFFFFFFFF).copy(alpha = 0.74f)
        else Color(0xFFFFFFFF).copy(alpha = 0.96f)
    }

    val containerBorder = BorderStroke(
        width = 1.dp,
        color = if (isDark) Color(0xFFFFFFFF).copy(alpha = 0.14f) else Color(0xFF000000).copy(alpha = 0.08f)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        // Floating pill container box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            contentAlignment = Alignment.Center
        ) {
            // Layer 1: Background Glass Pill (Isolated background layer)
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
                    .background(containerBgColor)
                    .border(containerBorder, containerShape)
            )

            // Layer 2: Foreground Icons and Selected Droplet Pill (Crisp, sharp, zero blur)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp),
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
        topStart = 20.dp,
        topEnd = 14.dp,
        bottomStart = 14.dp,
        bottomEnd = 20.dp
    )

    // Pure black in light theme, pure white in dark/AMOLED theme with no grey/muted tones
    val unselectedIconColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000)
    val selectedIconColor = accentColor

    Box(
        modifier = modifier
            .testTag(item.testTag)
            .height(56.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            // Selected item: Water droplet pill highlight sized to the icon
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(40.dp)
                    .shadow(
                        elevation = 6.dp,
                        shape = dropletShape,
                        spotColor = accentColor.copy(alpha = 0.5f),
                        ambientColor = accentColor.copy(alpha = 0.25f)
                    )
                    .clip(dropletShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                accentColor.copy(alpha = if (isDark) 0.38f else 0.26f),
                                accentColor.copy(alpha = if (isDark) 0.18f else 0.12f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = accentColor.copy(alpha = if (isDark) 0.50f else 0.35f),
                        shape = dropletShape
                    )
                    .drawWithContent {
                        drawContent()
                        // Specular highlight: soft white reflection arc near the top-left of the droplet
                        val highlightBrush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (isDark) 0.75f else 0.90f),
                                Color.White.copy(alpha = 0.25f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.26f, size.height * 0.24f),
                            radius = size.width * 0.38f
                        )
                        drawCircle(
                            brush = highlightBrush,
                            radius = size.width * 0.32f,
                            center = Offset(size.width * 0.26f, size.height * 0.24f)
                        )
                        // Soft top-left curved specular arc
                        drawArc(
                            color = Color.White.copy(alpha = if (isDark) 0.65f else 0.80f),
                            startAngle = 180f,
                            sweepAngle = 100f,
                            useCenter = false,
                            topLeft = Offset(2f, 2f),
                            size = Size(size.width - 4f, size.height - 4f),
                            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                        )
                    },
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
            // Unselected tab: icon only, pure solid color
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = unselectedIconColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

