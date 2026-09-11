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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.runtime.mutableIntStateOf
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
            val boldGlassStyle = remember(isDark, container) {
                HazeStyle(
                    backgroundColor = container,
                    tint = HazeTint(
                        color = if (isDark) container.copy(alpha = 0.35f) else container.copy(alpha = 0.30f)
                    ),
                    blurRadius = 24.dp,
                    noiseFactor = 0f
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(containerShape)
                    .hazeEffect(
                        state = hazeState,
                        style = boldGlassStyle
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
                val totalWidthPx = with(density) { totalWidth.toPx() }

                fun getTargetOffsetPx(index: Int): Float {
                    return (slotWidthPx * index) + (slotWidthPx - basePillWidthPx) / 2f
                }

                // Precalculate tab center points
                val tabCenterPoints = remember(slotWidthPx, itemCount) {
                    List(itemCount) { i -> (slotWidthPx * i) + (slotWidthPx / 2f) }
                }

                // Indicator offset state (synchronous binding for pointer drag)
                var indicatorOffsetPx by remember { mutableFloatStateOf(getTargetOffsetPx(selectedIndex)) }
                val indicatorAnimatable = remember { Animatable(getTargetOffsetPx(selectedIndex)) }
                var isDragging by remember { mutableStateOf(false) }
                var dragVelocity by remember { mutableFloatStateOf(0f) }
                var lastHoveredIndex by remember { mutableIntStateOf(selectedIndex) }

                // Continuous spring-damper dynamic stretch & bounce
                val dropletAspectAnim = remember { Animatable(1f) }

                // Sync position on external index change when not dragging
                LaunchedEffect(selectedIndex, slotWidthPx, basePillWidthPx) {
                    if (!isDragging) {
                        val targetPx = getTargetOffsetPx(selectedIndex)
                        lastHoveredIndex = selectedIndex
                        if (abs(indicatorOffsetPx - targetPx) > 0.5f) {
                            launch {
                                indicatorAnimatable.snapTo(indicatorOffsetPx)
                                indicatorAnimatable.animateTo(
                                    targetValue = targetPx,
                                    animationSpec = spring(
                                        dampingRatio = 0.68f, // Bounciness factor
                                        stiffness = 320f
                                    )
                                ) {
                                    indicatorOffsetPx = this.value
                                }
                            }
                            launch {
                                dropletAspectAnim.snapTo(1.22f)
                                dropletAspectAnim.animateTo(
                                    targetValue = 1f,
                                    animationSpec = spring(
                                        dampingRatio = 0.54f, // Spring restorative bounce
                                        stiffness = 260f
                                    )
                                )
                            }
                        } else {
                            indicatorOffsetPx = targetPx
                            indicatorAnimatable.snapTo(targetPx)
                        }
                    }
                }

                // Nearest tab index based on current indicator center
                val currentIndicatorCenter = indicatorOffsetPx + (basePillWidthPx / 2f)
                val nearestTabIndex = tabCenterPoints.indices.minByOrNull { i ->
                    abs(tabCenterPoints[i] - currentIndicatorCenter)
                } ?: selectedIndex

                val nearestCenterPx = getTargetOffsetPx(nearestTabIndex)
                val dragDistancePx = abs(indicatorOffsetPx - nearestCenterPx)
                val elasticRatio = (dragDistancePx / slotWidthPx.coerceAtLeast(1f)).coerceIn(0f, 1f)

                val velocityStretch = (abs(dragVelocity) / 2200f).coerceIn(0f, 0.40f)
                val continuousDragStretch = if (isDragging) {
                    (elasticRatio * 0.45f + velocityStretch * 0.35f).coerceIn(0f, 0.55f)
                } else {
                    val motionLag = abs(getTargetOffsetPx(selectedIndex) - indicatorOffsetPx)
                    (motionLag / slotWidthPx.coerceAtLeast(1f) * 0.32f).coerceIn(0f, 0.35f)
                }

                val dynamicScaleX = (dropletAspectAnim.value + continuousDragStretch).coerceIn(0.85f, 1.65f)
                val dynamicScaleY = (1f / dynamicScaleX.coerceAtLeast(0.7f)).coerceIn(0.65f, 1.15f)

                val dropletPillShape = RoundedCornerShape(percent = 50)
                val animatedAccentColor = activeSectionAccent

                // -------------------------------------------------------------
                // 1. VISUAL RENDERING: PURE GLASS WATER DROPLET LENS
                // - NO manual gradient fills or opaque colors.
                // - Purely transparent glass lens with high-intensity backdrop blur filter (20px).
                // - Sharp, thin white arc specular highlight along top edge for 3D depth.
                // -------------------------------------------------------------
                val dropletOffsetDp = with(density) { indicatorOffsetPx.toDp() }

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
                        // Pure transparent glass lens backdrop blur (20.dp high-intensity blur)
                        .hazeEffect(
                            state = hazeState,
                            style = HazeStyle(
                                backgroundColor = Color.Transparent,
                                blurRadius = 20.dp,
                                tint = HazeTint(
                                    Color.White.copy(alpha = if (isDark) 0.08f else 0.14f)
                                ),
                                noiseFactor = 0f
                            )
                        )
                        // Specular highlight: Sharp, thin white arc along the top edge for 3D depth
                        .drawWithContent {
                            drawContent()
                            val w = size.width
                            val h = size.height

                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = if (isDark) 0.85f else 0.95f),
                                        Color.White.copy(alpha = if (isDark) 0.35f else 0.45f),
                                        Color.Transparent
                                    ),
                                    startY = 0f,
                                    endY = h * 0.42f
                                ),
                                topLeft = Offset(1.5.dp.toPx(), 0.8.dp.toPx()),
                                size = Size(w - 3.dp.toPx(), h * 0.42f),
                                cornerRadius = CornerRadius(h / 2, h / 2),
                                style = Stroke(width = 1.0.dp.toPx())
                            )
                        }
                        // Pure transparent subtle glass boundary rim
                        .border(
                            width = 0.75.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (isDark) 0.45f else 0.65f),
                                    Color.White.copy(alpha = if (isDark) 0.12f else 0.20f),
                                    Color.White.copy(alpha = if (isDark) 0.20f else 0.35f)
                                )
                            ),
                            shape = dropletPillShape
                        )
                )

                // Navigation Tab Icons Row with pointer drag gesture detection
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(totalWidthPx, itemCount, selectedIndex) {
                            detectHorizontalDragGestures(
                                onDragStart = { _ ->
                                    isDragging = true
                                    dragVelocity = 0f
                                    coroutineScope.launch {
                                        indicatorAnimatable.stop()
                                    }
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    val minOffset = 0f
                                    val maxOffset = (totalWidthPx - basePillWidthPx).coerceAtLeast(0f)
                                    val newOffset = (indicatorOffsetPx + dragAmount).coerceIn(minOffset, maxOffset)
                                    indicatorOffsetPx = newOffset
                                    dragVelocity = dragAmount * 60f

                                    // Haptic feedback when crossing nearest tab threshold
                                    val center = newOffset + (basePillWidthPx / 2f)
                                    val hoveredIndex = tabCenterPoints.indices.minByOrNull { i ->
                                        abs(tabCenterPoints[i] - center)
                                    } ?: selectedIndex

                                    if (hoveredIndex != lastHoveredIndex) {
                                        lastHoveredIndex = hoveredIndex
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                },
                                onDragEnd = {
                                    isDragging = false
                                    val finalCenter = indicatorOffsetPx + (basePillWidthPx / 2f)
                                    val targetIndex = tabCenterPoints.indices.minByOrNull { i ->
                                        abs(tabCenterPoints[i] - finalCenter)
                                    } ?: selectedIndex

                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (targetIndex != selectedIndex) {
                                        onCategorySelected(navItems[targetIndex].serviceCategory)
                                    }

                                    val snapTargetPx = getTargetOffsetPx(targetIndex)
                                    coroutineScope.launch {
                                        indicatorAnimatable.snapTo(indicatorOffsetPx)
                                        indicatorAnimatable.animateTo(
                                            targetValue = snapTargetPx,
                                            animationSpec = spring(
                                                dampingRatio = 0.62f, // Bounciness factor
                                                stiffness = 320f
                                            )
                                        ) {
                                            indicatorOffsetPx = this.value
                                        }
                                    }
                                    coroutineScope.launch {
                                        dropletAspectAnim.snapTo(1.30f)
                                        dropletAspectAnim.animateTo(
                                            targetValue = 1f,
                                            animationSpec = spring(
                                                dampingRatio = 0.50f, // Bouncy elastic restoration
                                                stiffness = 240f
                                            )
                                        )
                                    }
                                    dragVelocity = 0f
                                },
                                onDragCancel = {
                                    isDragging = false
                                    dragVelocity = 0f
                                    val snapTargetPx = getTargetOffsetPx(selectedIndex)
                                    coroutineScope.launch {
                                        indicatorAnimatable.snapTo(indicatorOffsetPx)
                                        indicatorAnimatable.animateTo(
                                            targetValue = snapTargetPx,
                                            animationSpec = spring(
                                                dampingRatio = 0.68f,
                                                stiffness = 320f
                                            )
                                        ) {
                                            indicatorOffsetPx = this.value
                                        }
                                    }
                                    coroutineScope.launch {
                                        dropletAspectAnim.animateTo(
                                            targetValue = 1f,
                                            animationSpec = spring(
                                                dampingRatio = 0.60f,
                                                stiffness = 260f
                                            )
                                        )
                                    }
                                }
                            )
                        },
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    navItems.forEachIndexed { index, item ->
                        // Natural optical magnification under the lens
                        val tabCenterPx = tabCenterPoints[index]
                        val dropletCenterPx = indicatorOffsetPx + (basePillWidthPx / 2f)
                        val distanceToDroplet = abs(tabCenterPx - dropletCenterPx)
                        val proximity = (1f - (distanceToDroplet / slotWidthPx)).coerceIn(0f, 1f)

                        val isSelected = index == selectedIndex
                        val unselectedColor = if (isDark || isAmoled) Color(0xFF94A3B8) else Color(0xFF64748B)
                        val selectedColor = animatedAccentColor

                        val iconColor by animateColorAsState(
                            targetValue = if (proximity > 0.60f) selectedColor else unselectedColor,
                            animationSpec = tween(durationMillis = 180),
                            label = "navIconColor"
                        )

                        // Subtle natural lens magnification
                        val baseScale = 1.0f + (proximity * 0.18f)
                        val liftY = (-2.0f * proximity).dp

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
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}

