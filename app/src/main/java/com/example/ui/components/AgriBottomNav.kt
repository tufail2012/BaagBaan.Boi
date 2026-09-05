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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
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

    val activeSectionAccent = accentColor ?: MaterialTheme.colorScheme.primary

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
                val basePillWidth = minOf(54.dp, slotWidth - 2.dp)

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

                // Soft water droplet spreading animation on tab switch
                val dropletSpread = remember { Animatable(1f) }
                val dropletRipple = remember { Animatable(1f) }

                LaunchedEffect(selectedIndex) {
                    launch {
                        dropletSpread.snapTo(0.88f)
                        dropletSpread.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = 0.60f, // Gentle water droplet surface tension
                                stiffness = 250f
                            )
                        )
                    }
                    launch {
                        dropletRipple.snapTo(0f)
                        dropletRipple.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(
                                durationMillis = 450,
                                easing = FastOutSlowInEasing
                            )
                        )
                    }
                }

                val offsetDelta = (targetIndicatorOffset - animatedOffsetX).value
                val glideStretch = (kotlin.math.abs(offsetDelta) / slotWidth.value.coerceAtLeast(1f)).coerceIn(0f, 0.16f)
                val dynamicScaleX = dropletSpread.value * (1f + glideStretch * 0.45f)
                val dynamicScaleY = dropletSpread.value * (1f - glideStretch * 0.20f)

                val dropletPillShape = RoundedCornerShape(percent = 50)

                // Fluid Water-like Sliding Liquid Pill Indicator ("Water Glass" Look)
                val animatedAccentColor = activeSectionAccent
                val blobGradient = if (!isDark) {
                    // Clean "water glass" gradient: linear-gradient(135deg, rgba(255, 255, 255, 0.7) 0%, rgba(255, 255, 255, 0.35) 100%)
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.70f),
                            Color.White.copy(alpha = 0.35f)
                        ),
                        start = Offset.Zero,
                        end = Offset.Infinite
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.28f),
                            animatedAccentColor.copy(alpha = 0.15f),
                            Color.White.copy(alpha = 0.12f)
                        ),
                        start = Offset.Zero,
                        end = Offset.Infinite
                    )
                }

                // Subtle water droplet expanding ripple wave
                if (dropletRipple.value < 0.99f) {
                    val rippleProgress = dropletRipple.value
                    val rippleAlpha = ((1f - rippleProgress) * if (!isDark) 0.32f else 0.24f).coerceIn(0f, 1f)
                    val extraWidth = (rippleProgress * 14).dp
                    val extraHeight = (rippleProgress * 8).dp

                    Box(
                        modifier = Modifier
                            .offset(
                                x = animatedOffsetX - (extraWidth / 2),
                                y = -(extraHeight / 2)
                            )
                            .align(Alignment.CenterStart)
                            .width(basePillWidth + extraWidth)
                            .height(pillHeight + extraHeight)
                            .clip(dropletPillShape)
                            .border(
                                width = 1.dp,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = rippleAlpha * 0.7f),
                                        animatedAccentColor.copy(alpha = rippleAlpha * 0.3f),
                                        Color.Transparent
                                    )
                                ),
                                shape = dropletPillShape
                            )
                            .background(
                                color = Color.White.copy(alpha = rippleAlpha * 0.18f),
                                shape = dropletPillShape
                            )
                    )
                }

                Box(
                    modifier = Modifier
                        .offset(x = animatedOffsetX)
                        .align(Alignment.CenterStart)
                        .width(basePillWidth)
                        .height(pillHeight)
                        .graphicsLayer {
                            scaleX = dynamicScaleX
                            scaleY = dynamicScaleY
                        }
                        .shadow(
                            elevation = 3.dp,
                            shape = dropletPillShape,
                            spotColor = Color.Black.copy(alpha = if (isDark) 0.10f else 0.04f),
                            ambientColor = Color.Black.copy(alpha = if (isDark) 0.05f else 0.02f)
                        )
                        .then(
                            Modifier.hazeEffect(
                                state = hazeState,
                                style = HazeStyle(
                                    blurRadius = 12.dp,
                                    tints = listOf(
                                        HazeTint(color = animatedAccentColor.copy(alpha = if (isDark) 0.06f else 0.04f))
                                    ),
                                    backgroundColor = Color.Transparent
                                )
                            )
                        )
                        .clip(dropletPillShape)
                        .background(brush = blobGradient, shape = dropletPillShape)
                        .drawWithContent {
                            drawContent()
                            val w = size.width
                            val h = size.height
                            // Inset top specular highlight reflection (water meniscus reflection)
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = if (isDark) 0.55f else 0.80f),
                                        Color.White.copy(alpha = if (isDark) 0.15f else 0.25f),
                                        Color.Transparent
                                    ),
                                    startY = 0f,
                                    endY = h * 0.5f
                                ),
                                topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                                size = Size(w - 2.dp.toPx(), h - 2.dp.toPx()),
                                cornerRadius = CornerRadius(h / 2, h / 2),
                                style = Stroke(width = 1.dp.toPx())
                            )
                        }
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (!isDark) 0.65f else 0.45f),
                                    Color.White.copy(alpha = if (!isDark) 0.50f else 0.20f)
                                ),
                                start = Offset.Zero,
                                end = Offset.Infinite
                            ),
                            shape = dropletPillShape
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
                        val unselectedColor = if (isDark || isAmoled) Color(0xFF94A3B8) else Color(0xFF64748B)
                        // Active navigation icon dynamically pulls fill/stroke color from active theme's primary color palette
                        val selectedColor = animatedAccentColor

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
 * Frosted Glass Navigation Bar styling:
 * - Background Density:
 *   - Light Mode: rgba(255, 255, 255, 0.72) (tinted with 10% theme color).
 *   - Dark Mode: rgba(20, 20, 20, 0.75).
 * - Blur: backdrop-filter: blur(20px) saturate(180%).
 * - Edge Definition:
 *   - Clean upper highlight border: border-top: 1px solid rgba(255, 255, 255, 0.5).
 *   - Soft elevation shadow: box-shadow: 0 -4px 20px rgba(0, 0, 0, 0.04).
 */
fun Modifier.deepBlurNavBarBackground(
    hazeState: HazeState?,
    isDark: Boolean,
    isAmoled: Boolean,
    accentColor: Color,
    shape: Shape = RoundedCornerShape(percent = 50)
): Modifier {
    val hazeStyle = HazeStyle(
        backgroundColor = if (isAmoled) {
            Color.Black.copy(alpha = 0.85f)
        } else if (isDark) {
            // Dark Mode: background: rgba(20, 20, 20, 0.75)
            Color(0xFF141414).copy(alpha = 0.75f)
        } else {
            // Light Mode: background: rgba(255, 255, 255, 0.72)
            Color.White.copy(alpha = 0.72f)
        },
        blurRadius = 24.dp, // backdrop-filter: blur(20px)
        tints = listOf(
            HazeTint(
                color = accentColor.copy(alpha = if (isDark || isAmoled) 0.08f else 0.10f) // tinted with 10% theme color
            )
        ),
        noiseFactor = 0f
    )

    // Upper highlight border: border-top: 1px solid rgba(255, 255, 255, 0.5)
    val glassRimBrush = Brush.verticalGradient(
        colors = if (isDark || isAmoled) {
            listOf(
                Color.White.copy(alpha = 0.50f),
                accentColor.copy(alpha = 0.18f),
                Color.White.copy(alpha = 0.15f)
            )
        } else {
            listOf(
                Color.White.copy(alpha = 0.65f),
                accentColor.copy(alpha = 0.20f),
                Color.White.copy(alpha = 0.35f)
            )
        }
    )

    return this
        // Soft elevation shadow: box-shadow: 0 -4px 20px rgba(0, 0, 0, 0.04)
        .shadow(
            elevation = 6.dp,
            shape = shape,
            spotColor = Color.Black.copy(alpha = if (isAmoled) 0.30f else if (isDark) 0.18f else 0.04f),
            ambientColor = Color.Black.copy(alpha = if (isDark || isAmoled) 0.08f else 0.02f)
        )
        .clip(shape)
        // Strengthened backdrop blur
        .then(
            if (hazeState != null) {
                Modifier.hazeEffect(state = hazeState, style = hazeStyle)
            } else {
                Modifier
            }
        )
        // Increased Background Density:
        // Light Mode: background: rgba(255, 255, 255, 0.72) (tinted with 10% theme color)
        // Dark Mode: background: rgba(20, 20, 20, 0.75)
        .background(
            brush = when {
                isAmoled -> {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F0F0F).copy(alpha = 0.88f),
                            accentColor.copy(alpha = 0.06f),
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                }
                isDark -> {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1C1C1C).copy(alpha = 0.78f),
                            accentColor.copy(alpha = 0.06f),
                            Color(0xFF141414).copy(alpha = 0.75f)
                        )
                    )
                }
                else -> {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.76f),
                            accentColor.copy(alpha = 0.10f),
                            Color.White.copy(alpha = 0.72f)
                        )
                    )
                }
            },
            shape = shape
        )
        // Edge Definition:
        // Clean upper highlight border: border-top: 1px solid rgba(255, 255, 255, 0.5)
        .drawWithContent {
            drawContent()
            val w = size.width
            val h = size.height

            // Soft noise grain overlay
            drawRect(
                brush = SoftNoiseTexture.getOrCreateBrush(),
                alpha = if (isDark || isAmoled) 0.05f else 0.06f
            )

            // Upper highlight border: border-top: 1px solid rgba(255, 255, 255, 0.5)
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isDark || isAmoled) 0.45f else 0.50f),
                        Color.White.copy(alpha = if (isDark || isAmoled) 0.12f else 0.18f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = 4.dp.toPx()
                ),
                topLeft = Offset(1.dp.toPx(), 0.5.dp.toPx()),
                size = Size(w - 2.dp.toPx(), h - 1.dp.toPx()),
                cornerRadius = CornerRadius(h / 2, h / 2),
                style = Stroke(width = 1.dp.toPx())
            )

            // Top specular shine with blended reflective color sheen
            val margin = 20.dp.toPx()
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        accentColor.copy(alpha = if (isDark || isAmoled) 0.15f else 0.20f),
                        Color.White.copy(alpha = if (isDark || isAmoled) 0.35f else 0.60f),
                        accentColor.copy(alpha = if (isDark || isAmoled) 0.15f else 0.20f),
                        Color.Transparent
                    ),
                    startX = margin,
                    endX = w - margin
                ),
                topLeft = Offset(margin, 1.dp.toPx()),
                size = Size(w - (margin * 2), 1.dp.toPx())
            )
        }
        .border(
            width = 1.dp,
            brush = glassRimBrush,
            shape = shape
        )
}
