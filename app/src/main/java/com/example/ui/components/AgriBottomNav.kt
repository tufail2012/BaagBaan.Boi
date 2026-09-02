package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
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
import androidx.compose.ui.graphics.graphicsLayer
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
 * - Real backdrop blur of the content behind via Haze.
 * - Sliding 3D Bubble / Droplet indicator with spring physics and responsive horizontal wobble/shake feedback.
 * - Clean, semi-transparent active palette tint with raised 3D specular highlight and drop shadow.
 * - High-contrast unselected and selected navigation icons with haptic feedback.
 */
@Composable
fun AgriBottomNav(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    accentColor: Color? = null
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
    val surfaceColor = MaterialTheme.colorScheme.surface

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

    // Translucent glass tint letting real background colors diffuse through cleanly
    val glassTint = when {
        isAmoled -> Color(0xFF000000).copy(alpha = 0.28f)
        isDark -> Color(0xFF0B132B).copy(alpha = 0.22f)
        else -> Color(0xFFFFFFFF).copy(alpha = 0.18f)
    }

    // Translucent liquid glass gradient with subtle accent color diffusion
    val translucentGlassBrush = Brush.verticalGradient(
        colors = when {
            isAmoled -> listOf(
                Color.White.copy(alpha = 0.08f),
                Color(0xFF09090B).copy(alpha = 0.18f),
                activeSectionAccent.copy(alpha = 0.03f),
                Color.Black.copy(alpha = 0.26f)
            )
            isDark -> listOf(
                Color.White.copy(alpha = 0.12f),
                Color(0xFF1E293B).copy(alpha = 0.15f),
                activeSectionAccent.copy(alpha = 0.04f),
                Color(0xFF0F172A).copy(alpha = 0.22f)
            )
            else -> listOf(
                Color.White.copy(alpha = 0.38f),
                Color(0xFFF8FAFC).copy(alpha = 0.12f),
                activeSectionAccent.copy(alpha = 0.03f),
                Color.White.copy(alpha = 0.25f)
            )
        }
    )

    // Thin bright 1dp glass rim with specular top highlight and accent refraction
    val glassRimBrush = Brush.verticalGradient(
        colors = if (isDark || isAmoled) {
            listOf(
                Color.White.copy(alpha = 0.65f), // Bright specular top rim
                Color.White.copy(alpha = 0.25f), // Clear glass sides
                activeSectionAccent.copy(alpha = 0.30f), // Accent color refraction
                Color.White.copy(alpha = 0.15f)  // Soft bottom rim
            )
        } else {
            listOf(
                Color.White.copy(alpha = 0.95f), // Crisp bright top rim
                Color.White.copy(alpha = 0.40f), // Translucent sides
                activeSectionAccent.copy(alpha = 0.25f), // Soft accent diffusion
                Color.White.copy(alpha = 0.35f)  // Subtle bottom rim
            )
        }
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // 1. Soft floating 3D elevation shadow
                    .shadow(
                        elevation = 12.dp,
                        shape = containerShape,
                        spotColor = if (isDark || isAmoled) Color.Black.copy(alpha = 0.50f) else Color(0x28000000),
                        ambientColor = if (isDark || isAmoled) Color.Black.copy(alpha = 0.30f) else Color(0x12000000)
                    )
                    .clip(containerShape)
                    // 2. Real Haze backdrop blur with explicit backgroundColor
                    .hazeEffect(
                        state = hazeState,
                        style = HazeStyle(
                            backgroundColor = surfaceColor, // Kept in every theme to prevent crashes
                            tint = HazeTint(glassTint),
                            blurRadius = 24.dp,
                            noiseFactor = 0.02f
                        )
                    )
                    // 3. Translucent liquid glass gradient & color diffusion
                    .background(brush = translucentGlassBrush, shape = containerShape)
                    // 4. Subtle top inner specular reflection / edge sheen
                    .drawWithContent {
                        drawContent()
                        val w = size.width
                        val highlightHeight = 1.2.dp.toPx()
                        val margin = 16.dp.toPx()
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = if (isDark || isAmoled) 0.55f else 0.80f),
                                    Color.Transparent
                                ),
                                startX = margin,
                                endX = w - margin
                            ),
                            topLeft = Offset(margin, 1.dp.toPx()),
                            size = Size(w - (margin * 2), highlightHeight)
                        )
                    }
                    // 5. Thin bright 1dp glass rim
                    .border(
                        border = BorderStroke(width = 1.dp, brush = glassRimBrush),
                        shape = containerShape
                    )
            )

            // Layer 2: Interactive Tabs with Fluid Liquid Indicator
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp)
            ) {
                val totalWidth = maxWidth
                val itemCount = navItems.size
                val slotWidth = totalWidth / itemCount
                val basePillWidth = minOf(54.dp, slotWidth - 2.dp)
                val pillHeight = 48.dp

                val targetIndicatorOffset = (slotWidth * selectedIndex) + (slotWidth - basePillWidth) / 2

                // Smooth, natural fluid spring slide animation
                val animatedOffsetX by animateDpAsState(
                    targetValue = targetIndicatorOffset,
                    animationSpec = spring(
                        dampingRatio = 0.72f, // Natural fluid spring physics
                        stiffness = 320f
                    ),
                    label = "bottomNavPillSlide"
                )

                val dropletPillShape = RoundedCornerShape(percent = 50)

                // Fluid Water-like Sliding Liquid Pill Indicator
                Box(
                    modifier = Modifier
                        .offset(x = animatedOffsetX)
                        .align(Alignment.CenterStart)
                        .width(basePillWidth)
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

                        // 3D Embossed lift, scale, and subtle rotation tilt
                        val scale by animateFloatAsState(
                            targetValue = if (isSelected) 1.10f else 1.0f,
                            animationSpec = spring(
                                dampingRatio = 0.72f,
                                stiffness = 320f
                            ),
                            label = "navIconScale"
                        )

                        val liftY by animateDpAsState(
                            targetValue = if (isSelected) (-2).dp else 0.dp,
                            animationSpec = spring(
                                dampingRatio = 0.72f,
                                stiffness = 320f
                            ),
                            label = "navIconLift"
                        )

                        val rotX by animateFloatAsState(
                            targetValue = if (isSelected) 6f else 0f,
                            animationSpec = spring(
                                dampingRatio = 0.72f,
                                stiffness = 320f
                            ),
                            label = "navIconRotX"
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
                                modifier = Modifier
                                    .size(24.dp)
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                        translationY = liftY.toPx()
                                        rotationX = rotX
                                        cameraDistance = 16f * density
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}
