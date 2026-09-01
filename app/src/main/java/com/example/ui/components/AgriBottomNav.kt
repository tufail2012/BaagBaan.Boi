package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.LaunchedEffect
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
 * - Sliding 3D Bubble / Droplet indicator with spring physics and responsive horizontal wobble/shake feedback.
 * - Clean, semi-transparent active palette tint with raised 3D specular highlight and drop shadow.
 * - High-contrast unselected and selected navigation icons with haptic feedback.
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

    // Snappy tactile horizontal shake / vibration animation on tab switch
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(selectedIndex) {
        shakeOffset.snapTo(0f)
        shakeOffset.animateTo(
            targetValue = 0f,
            animationSpec = keyframes {
                durationMillis = 420
                0f at 0
                (-11f) at 60 using FastOutSlowInEasing
                11f at 130 using FastOutSlowInEasing
                (-8f) at 200 using FastOutSlowInEasing
                7f at 270 using FastOutSlowInEasing
                (-3.5f) at 340 using FastOutSlowInEasing
                1.5f at 380 using FastOutSlowInEasing
                0f at 420
            }
        )
    }

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

            // Layer 2: Interactive Tabs with Sliding 3D Bubble / Droplet Indicator
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp)
            ) {
                val totalWidth = maxWidth
                val itemCount = navItems.size
                val slotWidth = totalWidth / itemCount
                val pillWidth = minOf(54.dp, slotWidth - 2.dp)
                val pillHeight = 48.dp

                val targetIndicatorOffset = (slotWidth * selectedIndex) + (slotWidth - pillWidth) / 2

                val animatedOffsetX by animateDpAsState(
                    targetValue = targetIndicatorOffset,
                    animationSpec = spring(
                        dampingRatio = 0.62f, // Springy bounce physics
                        stiffness = 380f
                    ),
                    label = "bottomNavPillSlide"
                )

                val dropletPillShape = RoundedCornerShape(percent = 50)

                // Clean Sliding Pill Indicator with Shake Effect
                Box(
                    modifier = Modifier
                        .offset(x = animatedOffsetX + shakeOffset.value.dp)
                        .align(Alignment.CenterStart)
                        .width(pillWidth)
                        .height(pillHeight)
                        .bubbleDropletPillIndicator(
                            shape = dropletPillShape,
                            accentColor = activeSectionAccent,
                            isDark = isDark,
                            isAmoled = isAmoled
                        )
                )

                // Navigation Tab Icons Row
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    navItems.forEachIndexed { index, item ->
                        val isSelected = index == selectedIndex
                        val unselectedColor = if (isDark) Color(0xFFCBD5E1) else Color(0xFF64748B)
                        val selectedColor = if (isDark) activeSectionAccent.copy(alpha = 0.98f) else activeSectionAccent

                        val iconColor by animateColorAsState(
                            targetValue = if (isSelected) selectedColor else unselectedColor,
                            animationSpec = tween(durationMillis = 200),
                            label = "navIconColor"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp)
                                .testTag(item.testTag)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onCategorySelected(item.serviceCategory)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                tint = iconColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
