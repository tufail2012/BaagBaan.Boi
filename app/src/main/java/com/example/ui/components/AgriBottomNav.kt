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

                // Fluid Water-like Sliding Liquid Pill Indicator
                val animatedAccentColor = activeSectionAccent
                val blobGradient = if (!isDark) {
                    Brush.verticalGradient(
                        colors = listOf(
                            animatedAccentColor.copy(alpha = 0.38f),
                            animatedAccentColor.copy(alpha = 0.25f),
                            animatedAccentColor.copy(alpha = 0.12f)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(
                            animatedAccentColor.copy(alpha = 0.38f),
                            animatedAccentColor.copy(alpha = 0.25f),
                            Color(0xFF0F172A).copy(alpha = 0.70f)
                        )
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
                                        animatedAccentColor.copy(alpha = rippleAlpha),
                                        Color.White.copy(alpha = rippleAlpha * 0.6f),
                                        Color.Transparent
                                    )
                                ),
                                shape = dropletPillShape
                            )
                            .background(
                                color = animatedAccentColor.copy(alpha = rippleAlpha * 0.15f),
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
                            elevation = 8.dp,
                            shape = dropletPillShape,
                            spotColor = animatedAccentColor.copy(alpha = if (isDark) 0.5f else 0.35f),
                            ambientColor = animatedAccentColor.copy(alpha = 0.2f)
                        )
                        .clip(dropletPillShape)
                        .background(brush = blobGradient, shape = dropletPillShape)
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                colors = if (!isDark) {
                                    listOf(
                                        Color.White.copy(alpha = 0.75f),
                                        animatedAccentColor.copy(alpha = 0.35f),
                                        Color.Transparent
                                    )
                                } else {
                                    listOf(
                                        Color.White.copy(alpha = 0.3f),
                                        animatedAccentColor.copy(alpha = 0.4f),
                                        Color.Transparent
                                    )
                                }
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
                        val unselectedColor = if (isDark || isAmoled) Color(0xFF94A3B8) else Color(0xFF7E8B9B)
                        val selectedColor = if (isDark || isAmoled) Color.White else Color.Black

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
 * Refined Frosted Blur Navigation Bar Modifier for Baagbaan BOI.
 * Features:
 * - Enhanced Optical Blur: 48.dp blur radius via Haze for rich, deep optical dispersion.
 * - Minimal, Delicate Gradient: Greatly reduced color saturation and tinting for clean, pure frosted glass.
 * - Translucent Diffusion: Lower opacity base layers allowing the background content to blur through smoothly.
 * - Specular Sheen & Glass Rim: Crisp refractive top highlight and subtle dual-tone glass border.
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
            Color.Black.copy(alpha = 0.35f)
        } else if (isDark) {
            Color(0xFF16141D).copy(alpha = 0.28f)
        } else {
            Color.White.copy(alpha = 0.25f)
        },
        blurRadius = 64.dp, // Rich, deeper optical backdrop blur
        tints = listOf(
            HazeTint(
                color = if (isDark || isAmoled) {
                    accentColor.copy(alpha = 0.05f)
                } else {
                    accentColor.copy(alpha = 0.05f)
                }
            )
        ),
        noiseFactor = 0.08f
    )

    // Reflective glass rim brush applying specular white and accent refraction across edges
    val glassRimBrush = Brush.linearGradient(
        colors = if (isDark || isAmoled) {
            listOf(
                Color.White.copy(alpha = 0.40f), // Crisp specular reflection along top edge
                accentColor.copy(alpha = 0.24f), // Reflective edge sheen
                Color.White.copy(alpha = 0.12f), // Clear lateral glass sides
                accentColor.copy(alpha = 0.16f), // Ambient edge reflection
                Color.White.copy(alpha = 0.08f)  // Soft specular bottom return
            )
        } else {
            listOf(
                Color.White.copy(alpha = 0.95f), // Crisp specular reflection along top edge
                accentColor.copy(alpha = 0.28f), // Reflective edge sheen in light mode
                Color.White.copy(alpha = 0.50f), // Clear lateral glass sides
                accentColor.copy(alpha = 0.18f), // Ambient edge reflection
                Color.White.copy(alpha = 0.40f)  // Soft specular bottom return
            )
        }
    )

    return this
        // 1. Soft floating drop shadow with subtle ambient halo in the reflective accent tone
        .shadow(
            elevation = 16.dp,
            shape = shape,
            spotColor = if (isAmoled) Color.Black.copy(alpha = 0.55f) else if (isDark) Color.Black.copy(alpha = 0.35f) else Color(0x22000000),
            ambientColor = if (isDark || isAmoled) accentColor.copy(alpha = 0.14f) else accentColor.copy(alpha = 0.10f)
        )
        .clip(shape)
        // 2. Real optical backdrop blur via Haze with maximum clarity
        .then(
            if (hazeState != null) {
                Modifier.hazeEffect(state = hazeState, style = hazeStyle)
            } else {
                Modifier
            }
        )
        // 3. Uniform reflective glass body wash incorporating subtle reflective sheen consistently across any background
        .background(
            brush = when {
                isAmoled -> {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.10f),          // Specular top glass reflection
                            Color(0xFF100E14).copy(alpha = 0.25f),    // Translucent dark glass
                            accentColor.copy(alpha = 0.08f),          // Uniform reflective color sheen
                            Color(0xFF000000).copy(alpha = 0.35f)     // Pure black AMOLED foundation
                        )
                    )
                }
                isDark -> {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.14f),          // Specular top glass reflection
                            Color(0xFF221F2B).copy(alpha = 0.32f),    // Translucent dark charcoal glass
                            accentColor.copy(alpha = 0.10f),          // Uniform reflective color sheen
                            Color(0xFF14121A).copy(alpha = 0.35f)     // Dark charcoal gray foundation (non-pure-black)
                        )
                    )
                }
                else -> {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.65f),          // Specular top glass reflection
                            Color.White.copy(alpha = 0.25f),          // Translucent light glass
                            accentColor.copy(alpha = 0.10f),          // Uniform reflective color sheen
                            Color.White.copy(alpha = 0.35f)           // Base foundation
                        )
                    )
                }
            },
            shape = shape
        )
        // 4. Soft noise grain overlay and dual-tone reflective specular top highlight
        .drawWithContent {
            drawContent()
            val w = size.width
            val highlightHeight = 1.5.dp.toPx()
            val margin = 12.dp.toPx()

            // Soft procedural micro-grain overlay for tactile frosted noisy blur
            drawRect(
                brush = SoftNoiseTexture.getOrCreateBrush(),
                alpha = if (isDark || isAmoled) 0.08f else 0.10f
            )

            // Top specular shine with blended reflective color sheen
            val highlightWhiteAlpha = if (isDark || isAmoled) 0.38f else 0.70f
            val sheenAccentAlpha = if (isDark || isAmoled) 0.18f else 0.22f

            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        accentColor.copy(alpha = sheenAccentAlpha),
                        Color.White.copy(alpha = highlightWhiteAlpha),
                        accentColor.copy(alpha = sheenAccentAlpha),
                        Color.Transparent
                    ),
                    startX = margin,
                    endX = w - margin
                ),
                topLeft = Offset(margin, 1.dp.toPx()),
                size = Size(w - (margin * 2), highlightHeight)
            )
        }
        // 5. Crisp refractive glass border with reflective sheen
        .border(
            width = 1.dp,
            brush = glassRimBrush,
            shape = shape
        )
}
