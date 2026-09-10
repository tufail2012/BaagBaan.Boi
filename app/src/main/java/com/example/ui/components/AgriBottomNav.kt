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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlin.math.abs
import kotlin.math.roundToInt

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
 * - Interactive Liquid Water Droplet lens active indicator with optical refraction, specular highlights,
 *   smooth tap glide via spring physics, and fluid drag-to-switch with elastic viscous stretch.
 * - High-contrast unselected and selected navigation icons with haptic feedback.
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
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
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

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

    val glassBorderBrush = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.30f),
                Color.White.copy(alpha = 0.12f),
                Color.White.copy(alpha = 0.04f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.50f),
                Color.White.copy(alpha = 0.20f),
                Color.White.copy(alpha = 0.06f)
            )
        )
    }

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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(containerShape)
                    .hazeEffect(
                        state = hazeState,
                        style = HazeMaterials.regular(container)
                    )
            )

            // LAYER 2: INTERACTIVE LIQUID WATER DROPLET LENS & NAVIGATION ICONS
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

                val slotWidthPx = with(density) { slotWidth.toPx() }
                val basePillWidthPx = with(density) { basePillWidth.toPx() }

                fun getTargetOffsetPx(index: Int): Float {
                    return (slotWidthPx * index) + (slotWidthPx - basePillWidthPx) / 2f
                }

                // Droplet animated position in pixels
                val dropletOffsetPx = remember { Animatable(getTargetOffsetPx(selectedIndex)) }
                var isDragging by remember { mutableStateOf(false) }
                var dragVelocity by remember { mutableFloatStateOf(0f) }

                // Elastic jelly bounce effect on snap
                val dropletSpread = remember { Animatable(1f) }
                val dropletRipple = remember { Animatable(1f) }

                // Sync position on external index change when not dragging
                LaunchedEffect(selectedIndex) {
                    if (!isDragging) {
                        val targetPx = getTargetOffsetPx(selectedIndex)
                        launch {
                            dropletOffsetPx.animateTo(
                                targetValue = targetPx,
                                animationSpec = spring(
                                    dampingRatio = 0.72f,
                                    stiffness = 340f
                                )
                            )
                        }
                        launch {
                            dropletSpread.snapTo(0.85f)
                            dropletSpread.animateTo(
                                targetValue = 1f,
                                animationSpec = spring(
                                    dampingRatio = 0.58f,
                                    stiffness = 260f
                                )
                            )
                        }
                        launch {
                            dropletRipple.snapTo(0f)
                            dropletRipple.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(
                                    durationMillis = 420,
                                    easing = FastOutSlowInEasing
                                )
                            )
                        }
                    }
                }

                // Calculate fluid stretch from dragging or spring motion
                val velocityStretch = (abs(dragVelocity) / 2400f).coerceIn(0f, 0.35f)
                val targetCenterPx = getTargetOffsetPx(selectedIndex)
                val motionLagPx = abs(targetCenterPx - dropletOffsetPx.value)
                val springStretch = (motionLagPx / slotWidthPx.coerceAtLeast(1f) * 0.28f).coerceIn(0f, 0.28f)
                val fluidStretch = if (isDragging) velocityStretch else springStretch

                val dynamicScaleX = dropletSpread.value * (1f + fluidStretch * 0.65f)
                val dynamicScaleY = dropletSpread.value * (1f - fluidStretch * 0.30f)

                val dropletPillShape = RoundedCornerShape(percent = 50)
                val animatedAccentColor = activeSectionAccent

                // Subtle water droplet expanding ripple wave
                if (dropletRipple.value < 0.99f) {
                    val rippleProgress = dropletRipple.value
                    val rippleAlpha = ((1f - rippleProgress) * if (!isDark) 0.16f else 0.12f).coerceIn(0f, 1f)
                    val extraWidth = (rippleProgress * 14).dp
                    val extraHeight = (rippleProgress * 8).dp

                    val currentOffsetDp = with(density) { dropletOffsetPx.value.toDp() }

                    Box(
                        modifier = Modifier
                            .offset(
                                x = currentOffsetDp - (extraWidth / 2),
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

                // -------------------------------------------------------------
                // VISUAL AESTHETIC: LIQUID WATER DROPLET LENS COMPONENT
                // - Ultra-transparent convex liquid glass / water droplet body
                // - Optical refraction & subtle magnification
                // - Specular gloss on top/bottom edges with surface tension curvature
                // - Elastic jelly/metaball fluid stretch during drag & snap
                // -------------------------------------------------------------
                val dropletOffsetDp = with(density) { dropletOffsetPx.value.toDp() }

                Box(
                    modifier = Modifier
                        .offset(x = dropletOffsetDp)
                        .align(Alignment.CenterStart)
                        .width(basePillWidth)
                        .height(pillHeight)
                        .graphicsLayer {
                            scaleX = dynamicScaleX
                            scaleY = dynamicScaleY
                        }
                        .clip(dropletPillShape)
                        // Ultra-transparent convex liquid tint with subtle ambient refraction
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    animatedAccentColor.copy(alpha = if (isDark) 0.16f else 0.12f),
                                    animatedAccentColor.copy(alpha = if (isDark) 0.08f else 0.06f),
                                    Color.White.copy(alpha = if (isDark) 0.04f else 0.10f)
                                ),
                                center = Offset(0.5f, 0.4f),
                                radius = 90f
                            ),
                            shape = dropletPillShape
                        )
                        // Optical specular gloss, meniscus surface tension & glass rim refraction
                        .drawWithContent {
                            drawContent()
                            val w = size.width
                            val h = size.height

                            // 1. Top specular gloss crest (water meniscus reflection)
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = if (isDark) 0.55f else 0.70f),
                                        Color.White.copy(alpha = if (isDark) 0.15f else 0.25f),
                                        Color.Transparent
                                    ),
                                    startY = 0f,
                                    endY = h * 0.48f
                                ),
                                topLeft = Offset(1.5.dp.toPx(), 1.dp.toPx()),
                                size = Size(w - 3.dp.toPx(), h * 0.48f),
                                cornerRadius = CornerRadius(h / 2, h / 2),
                                style = Stroke(width = 1.2.dp.toPx())
                            )

                            // 2. Bottom horizon caustics bounce (liquid refraction glow)
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.White.copy(alpha = if (isDark) 0.12f else 0.28f),
                                        Color.White.copy(alpha = if (isDark) 0.28f else 0.45f)
                                    ),
                                    startY = h * 0.60f,
                                    endY = h
                                ),
                                topLeft = Offset(2.dp.toPx(), h * 0.58f),
                                size = Size(w - 4.dp.toPx(), h * 0.40f),
                                cornerRadius = CornerRadius(h / 2, h / 2),
                                style = Stroke(width = 1.dp.toPx())
                            )
                        }
                        // Liquid droplet outer perimeter meniscus border
                        .border(
                            width = 0.9.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (!isDark) 0.55f else 0.38f),
                                    animatedAccentColor.copy(alpha = if (!isDark) 0.25f else 0.18f),
                                    Color.White.copy(alpha = if (!isDark) 0.30f else 0.15f)
                                ),
                                start = Offset.Zero,
                                end = Offset.Infinite
                            ),
                            shape = dropletPillShape
                        )
                )

                // -------------------------------------------------------------
                // DUAL INTERACTION: TAP & DIRECT DRAG TO SWITCH TABS
                // -------------------------------------------------------------
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(totalWidth, itemCount) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    isDragging = true
                                    dragVelocity = 0f
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val currentX = dropletOffsetPx.value
                                    val newX = (currentX + dragAmount.x).coerceIn(
                                        0f,
                                        (slotWidthPx * (itemCount - 1)) + (slotWidthPx - basePillWidthPx) / 2f
                                    )
                                    coroutineScope.launch {
                                        dropletOffsetPx.snapTo(newX)
                                    }
                                    dragVelocity = dragAmount.x * 60f

                                    // Haptic feedback when crossing into neighboring tab
                                    val hoveredIndex = ((newX + basePillWidthPx / 2f) / slotWidthPx)
                                        .toInt()
                                        .coerceIn(0, itemCount - 1)
                                    if (hoveredIndex != selectedIndex) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                },
                                onDragEnd = {
                                    isDragging = false
                                    val finalCenter = dropletOffsetPx.value + (basePillWidthPx / 2f)
                                    val targetIndex = (finalCenter / slotWidthPx)
                                        .toInt()
                                        .coerceIn(0, itemCount - 1)

                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onCategorySelected(navItems[targetIndex].serviceCategory)

                                    coroutineScope.launch {
                                        dropletOffsetPx.animateTo(
                                            targetValue = getTargetOffsetPx(targetIndex),
                                            animationSpec = spring(
                                                dampingRatio = 0.65f, // Jelly bounce
                                                stiffness = 320f
                                            )
                                        )
                                    }
                                    coroutineScope.launch {
                                        dropletSpread.snapTo(0.82f)
                                        dropletSpread.animateTo(
                                            targetValue = 1f,
                                            animationSpec = spring(
                                                dampingRatio = 0.52f,
                                                stiffness = 240f
                                            )
                                        )
                                    }
                                    dragVelocity = 0f
                                },
                                onDragCancel = {
                                    isDragging = false
                                    dragVelocity = 0f
                                    coroutineScope.launch {
                                        dropletOffsetPx.animateTo(
                                            targetValue = getTargetOffsetPx(selectedIndex),
                                            animationSpec = spring(
                                                dampingRatio = 0.72f,
                                                stiffness = 320f
                                            )
                                        )
                                    }
                                }
                            )
                        }
                )

                // Navigation Tab Icons Row
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    navItems.forEachIndexed { index, item ->
                        // Calculate proximity to droplet for optical magnification under the lens
                        val tabCenterPx = (slotWidthPx * index) + (slotWidthPx / 2f)
                        val dropletCenterPx = dropletOffsetPx.value + (basePillWidthPx / 2f)
                        val distanceToDroplet = abs(tabCenterPx - dropletCenterPx)
                        val proximity = (1f - (distanceToDroplet / slotWidthPx)).coerceIn(0f, 1f)

                        val isSelected = index == selectedIndex
                        val unselectedColor = if (isDark || isAmoled) Color(0xFF94A3B8) else Color(0xFF64748B)
                        val selectedColor = animatedAccentColor

                        val iconColor by animateColorAsState(
                            targetValue = if (proximity > 0.65f) selectedColor else unselectedColor,
                            animationSpec = tween(durationMillis = 180),
                            label = "navIconColor"
                        )

                        // Optical refraction & magnification under water droplet
                        val baseScale = 1.0f + (proximity * 0.16f)
                        val liftY = (-2.5f * proximity).dp
                        val rotX = (5f * proximity)

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
                                        scaleX = baseScale
                                        scaleY = baseScale
                                        translationY = with(density) { liftY.toPx() }
                                        rotationX = rotX
                                        cameraDistance = 16f * density.density
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}

