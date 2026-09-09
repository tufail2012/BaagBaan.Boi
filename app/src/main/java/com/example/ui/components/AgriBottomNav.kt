package com.example.ui.components

import android.os.Build
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

data class AgriNavItem(
    val title: String,
    val serviceCategory: String,
    val icon: ImageVector,
    val testTag: String
)

/**
 * Floating Pill Bottom Navigation Bar for Baagbaan BOI.
 * Features:
 * - Real optical liquid-glass material sampled from the live app backdrop.
 * - Hardware AGSL lens refraction & chromatic dispersion (Android 13+ / API 33+).
 * - Multi-stage specular reflection, upper crest highlight, and horizon bounce.
 * - Sliding 3D Bubble / Droplet indicator with spring physics and responsive horizontal wobble/shake feedback.
 * - Clean, semi-transparent active palette tint with raised 3D specular highlight.
 * - High-contrast unselected and selected navigation icons with haptic feedback.
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun AgriBottomNav(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    accentColor: Color? = null,
    backdrop: Backdrop? = null
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

    val activeSectionAccent = accentColor ?: MaterialTheme.colorScheme.primary

    val containerShape = RoundedCornerShape(percent = 50)
    val container = MaterialTheme.colorScheme.surface

    val glassHazeStyle = remember(isDark, container) {
        HazeStyle(
            backgroundColor = container,
            tint = HazeTint(
                if (isDark) {
                    container.copy(alpha = 0.20f)
                } else {
                    Color.White.copy(alpha = 0.12f)
                }
            ),
            blurRadius = 24.dp,
            noiseFactor = 0f
        )
    }

    // Translucent liquid glass lens surface providing physical body while preserving backdrop color & refraction
    val glassSurfaceBrush = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.05f),
                container.copy(alpha = 0.12f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.14f),
                Color.White.copy(alpha = 0.05f)
            )
        )
    }

    val glassBorderBrush = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.40f),
                Color.White.copy(alpha = 0.14f),
                Color.White.copy(alpha = 0.05f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.75f),
                Color.White.copy(alpha = 0.28f),
                Color.White.copy(alpha = 0.10f)
            )
        )
    }

    val localDensity = LocalDensity.current
    // Frosted diffusion: 14dp blurs out legible text & sharp images into soft forms without washing into a flat blob
    val blurRadiusPx = with(localDensity) { 14.dp.toPx() }
    // Refraction height: 28dp (of 34dp radius) leaves the central strip softer while the curved perimeter acts as an optical lens
    val refractionHeightPx = with(localDensity) { 28.dp.toPx() }
    // Refraction amount: 26dp provides smooth, restrained optical curvature and chromatic dispersion along the rim
    val refractionAmountPx = with(localDensity) { 26.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .shadow(
                    elevation = 10.dp,
                    shape = containerShape,
                    spotColor = if (isDark) Color.Black.copy(alpha = 0.55f) else Color.Black.copy(alpha = 0.08f),
                    ambientColor = if (isDark) Color.Black.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.04f)
                )
                .clip(containerShape)
                .border(
                    BorderStroke(
                        width = 0.5.dp,
                        brush = glassBorderBrush
                    ),
                    containerShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // LAYER 1: OUTER REFRACTING LIQUID-GLASS SURFACE
            // Live content backdrop capture -> Vibrancy -> Blur -> Lens Refraction + Dispersion ->
            // Subtle translucent surface tint -> Directional specular crest highlight
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(containerShape)
                    .then(
                        if (backdrop != null) {
                            Modifier.drawBackdrop(
                                backdrop = backdrop,
                                shape = { containerShape },
                                effects = {
                                    vibrancy()
                                    blur(blurRadiusPx)
                                    lens(
                                        refractionHeight = refractionHeightPx,
                                        refractionAmount = refractionAmountPx,
                                        depthEffect = true,
                                        chromaticAberration = true
                                    )
                                },
                                highlight = {
                                    Highlight(
                                        width = 0.8.dp,
                                        blurRadius = 0.5.dp,
                                        alpha = if (isDark) 0.50f else 0.70f,
                                        style = HighlightStyle.Default
                                    )
                                },
                                shadow = null,
                                onDrawSurface = {
                                    drawRect(glassSurfaceBrush)
                                }
                            )
                        } else {
                            Modifier
                                .hazeEffect(
                                    state = hazeState,
                                    style = glassHazeStyle
                                )
                                .background(glassSurfaceBrush)
                        }
                    )
                    .drawBehind {
                        val w = size.width
                        val h = size.height

                        // 1. Crisp top specular curved highlight along the upper crest (directional light reflection)
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(
                                        alpha = if (isDark) 0.40f else 0.55f
                                    ),
                                    Color.White.copy(
                                        alpha = if (isDark) 0.12f else 0.18f
                                    ),
                                    Color.Transparent
                                ),
                                startY = 0f,
                                endY = h * 0.35f
                            ),
                            topLeft = Offset(0.8.dp.toPx(), 0.8.dp.toPx()),
                            size = Size(w - 1.6.dp.toPx(), h - 1.6.dp.toPx()),
                            cornerRadius = CornerRadius(h / 2f, h / 2f),
                            style = Stroke(width = 0.9.dp.toPx())
                        )

                        // 2. Subtle lower internal reflection (horizon bounce)
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(
                                        alpha = if (isDark) 0.03f else 0.06f
                                    ),
                                    Color.White.copy(
                                        alpha = if (isDark) 0.06f else 0.10f
                                    )
                                ),
                                startY = h * 0.70f,
                                endY = h - 1.dp.toPx()
                            ),
                            topLeft = Offset(1.2.dp.toPx(), 1.2.dp.toPx()),
                            size = Size(w - 2.4.dp.toPx(), h - 2.4.dp.toPx()),
                            cornerRadius = CornerRadius(h / 2f, h / 2f),
                            style = Stroke(width = 0.6.dp.toPx())
                        )
                    }
            )

            // LAYER 2: EXISTING ACTIVE DROPLET & NAVIGATION ICONS (Sitting crisply ABOVE the glass)
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp)
            ) {
                val totalWidth = maxWidth
                val itemCount = navItems.size
                val slotWidth = totalWidth / itemCount
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
                    // Subtle translucent liquid glass gradient
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.28f),
                            animatedAccentColor.copy(alpha = 0.12f),
                            Color.White.copy(alpha = 0.16f)
                        ),
                        start = Offset.Zero,
                        end = Offset.Infinite
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.18f),
                            animatedAccentColor.copy(alpha = 0.10f),
                            Color.White.copy(alpha = 0.08f)
                        ),
                        start = Offset.Zero,
                        end = Offset.Infinite
                    )
                }

                // Subtle water droplet expanding ripple wave
                if (dropletRipple.value < 0.99f) {
                    val rippleProgress = dropletRipple.value
                    val rippleAlpha = ((1f - rippleProgress) * if (!isDark) 0.16f else 0.12f).coerceIn(0f, 1f)
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
                                        Color.White.copy(alpha = rippleAlpha * 0.5f),
                                        animatedAccentColor.copy(alpha = rippleAlpha * 0.2f),
                                        Color.Transparent
                                    )
                                ),
                                shape = dropletPillShape
                            )
                            .background(
                                color = Color.White.copy(alpha = rippleAlpha * 0.10f),
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
                        // no independent backdrop sampling here, it sits on top of
                        // the container's own hazeEffect above, same as BitChord's
                        // selected-tab highlight box
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
                                        Color.White.copy(alpha = if (isDark) 0.40f else 0.60f),
                                        Color.White.copy(alpha = if (isDark) 0.10f else 0.18f),
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
                            width = 0.8.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (!isDark) 0.45f else 0.30f),
                                    Color.White.copy(alpha = if (!isDark) 0.25f else 0.12f)
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
