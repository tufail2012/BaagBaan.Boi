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
import androidx.compose.ui.graphics.Shape
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
            // Layer 1: Frosted Liquid Glass Background with Deep Blur & Zero Transparency
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .deepBlurNavBarBackground(
                        hazeState = hazeState,
                        isDark = isDark,
                        isAmoled = isAmoled,
                        accentColor = activeSectionAccent,
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
                val isLight = !isDark && !isAmoled
                val pillHeight = 48.dp
                val basePillWidth = if (isLight) minOf(48.dp, slotWidth - 4.dp) else minOf(54.dp, slotWidth - 2.dp)

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
                            hazeState = hazeState,
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
                        val unselectedColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF7E8B9B)
                        val selectedColor = if (isDark) Color.White else activeSectionAccent

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

/**
 * Gradient Color Blur Navigation Bar Modifier for Baagbaan BOI.
 * Features:
 * - Real optical backdrop blur via Haze with an elegant tinted color gradient.
 * - Multi-stop vibrant color gradient overlay blending seamlessly across the bar.
 * - Specular sheen highlight, frosted micro-grain noise, and subtle refractive rim.
 */
fun Modifier.deepBlurNavBarBackground(
    hazeState: HazeState?,
    isDark: Boolean,
    isAmoled: Boolean,
    accentColor: Color,
    shape: Shape = RoundedCornerShape(percent = 50)
): Modifier {
    // Dynamic complementary color pairing for rich chromatic gradient blur
    val secondaryGradientColor = if (isDark || isAmoled) {
        Color(
            red = (accentColor.red * 0.45f + 0.15f).coerceIn(0f, 1f),
            green = (accentColor.green * 0.35f + 0.10f).coerceIn(0f, 1f),
            blue = (accentColor.blue * 0.70f + 0.30f).coerceIn(0f, 1f)
        )
    } else {
        Color(
            red = (accentColor.red * 0.35f + 0.65f).coerceIn(0f, 1f),
            green = (accentColor.green * 0.50f + 0.50f).coerceIn(0f, 1f),
            blue = (accentColor.blue * 0.85f + 0.15f).coerceIn(0f, 1f)
        )
    }

    val hazeStyle = HazeStyle(
        backgroundColor = if (isAmoled) {
            Color.Black.copy(alpha = 0.70f)
        } else if (isDark) {
            Color(0xFF0F0E13).copy(alpha = 0.70f)
        } else {
            Color(0xFFFAF5F2).copy(alpha = 0.65f)
        },
        blurRadius = 32.dp, // Optical backdrop blur
        tints = listOf(
            HazeTint(
                color = if (isDark || isAmoled) {
                    accentColor.copy(alpha = 0.30f)
                } else {
                    accentColor.copy(alpha = 0.22f)
                }
            )
        ),
        noiseFactor = 0.20f
    )

    val glassRimBrush = Brush.linearGradient(
        colors = if (isDark || isAmoled) {
            listOf(
                Color.White.copy(alpha = 0.45f),
                accentColor.copy(alpha = 0.40f),
                secondaryGradientColor.copy(alpha = 0.30f),
                Color.White.copy(alpha = 0.10f)
            )
        } else {
            listOf(
                Color.White.copy(alpha = 0.95f),
                accentColor.copy(alpha = 0.50f),
                secondaryGradientColor.copy(alpha = 0.40f),
                Color.White.copy(alpha = 0.65f)
            )
        }
    )

    return this
        // 1. Soft 3D floating drop shadow with ambient color glow
        .shadow(
            elevation = 18.dp,
            shape = shape,
            spotColor = if (isDark || isAmoled) Color.Black.copy(alpha = 0.55f) else Color(0x35000000),
            ambientColor = accentColor.copy(alpha = if (isDark || isAmoled) 0.30f else 0.22f)
        )
        .clip(shape)
        // 2. Real optical backdrop blur via Haze
        .then(
            if (hazeState != null) {
                Modifier.hazeEffect(state = hazeState, style = hazeStyle)
            } else {
                Modifier
            }
        )
        // 3. Vibrant chromatic gradient blur overlay
        .background(
            brush = if (isDark || isAmoled) {
                Brush.linearGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.32f),
                        Color(0xFF1E1B28).copy(alpha = 0.82f),
                        secondaryGradientColor.copy(alpha = 0.28f),
                        Color(0xFF0F0E16).copy(alpha = 0.88f)
                    )
                )
            } else {
                Brush.linearGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.28f),
                        Color(0xFFFFF7F2).copy(alpha = 0.82f),
                        secondaryGradientColor.copy(alpha = 0.25f),
                        Color(0xFFF3E8FF).copy(alpha = 0.85f)
                    )
                )
            },
            shape = shape
        )
        // 4. Soft noise grain overlay and specular top highlight
        .drawWithContent {
            drawContent()
            val w = size.width
            val highlightHeight = 1.5.dp.toPx()
            val margin = 12.dp.toPx()

            // Soft procedural micro-grain overlay for tactile frosted noisy blur
            drawRect(
                brush = SoftNoiseTexture.getOrCreateBrush(),
                alpha = if (isDark || isAmoled) 0.14f else 0.16f
            )

            // Top specular shine with soft gradient refraction
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = if (isDark || isAmoled) 0.55f else 0.85f),
                        Color.Transparent
                    ),
                    startX = margin,
                    endX = w - margin
                ),
                topLeft = Offset(margin, 1.dp.toPx()),
                size = Size(w - (margin * 2), highlightHeight)
            )
        }
        // 5. Crisp refractive gradient glass border
        .border(
            width = 1.dp,
            brush = glassRimBrush,
            shape = shape
        )
}
