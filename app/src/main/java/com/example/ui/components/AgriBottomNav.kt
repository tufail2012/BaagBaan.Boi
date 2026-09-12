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
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.backdrops.emptyBackdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop as kyantDrawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.kyant.backdrop.shadow.InnerShadow
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.abs
import kotlin.math.roundToInt

// BitChord-v1.5.2 Liquid Glass Pipeline Constants
private const val VIBRANCY = 1f
private const val BLUR_RADIUS_DP = 8f
private const val LENS_HEIGHT = 0.5f
private const val LENS_AMOUNT = 0.5f
private const val LENS_MAX_DP = 48f
private const val SURFACE_OPACITY = 0.4f
private const val GLASS_RESOLUTION_SCALE = 0.33f

/**
 * Extension overload for drawBackdrop supporting downscaled backdrop resolution via backdropScale.
 */
fun Modifier.drawBackdrop(
    backdrop: Backdrop,
    shape: () -> Shape,
    effects: BackdropEffectScope.() -> Unit,
    highlight: () -> Highlight = { Highlight.Default },
    shadow: () -> Shadow = { Shadow.Default },
    innerShadow: (() -> InnerShadow)? = null,
    layerBlock: (GraphicsLayerScope.() -> Unit)? = null,
    exportedBackdrop: LayerBackdrop? = null,
    onDrawBehind: (DrawScope.() -> Unit)? = null,
    onDrawSurface: (DrawScope.() -> Unit)? = null,
    onDrawFront: (DrawScope.() -> Unit)? = null,
    backdropScale: Float = 1f,
): Modifier {
    val scaleLayerBlock: (GraphicsLayerScope.() -> Unit)? = if (backdropScale != 1f) {
        {
            scaleX = backdropScale
            scaleY = backdropScale
            layerBlock?.invoke(this)
            Unit
        }
    } else {
        layerBlock
    }
    return this.kyantDrawBackdrop(
        backdrop = backdrop,
        shape = shape,
        effects = effects,
        highlight = highlight,
        shadow = shadow,
        innerShadow = innerShadow,
        layerBlock = scaleLayerBlock,
        exportedBackdrop = exportedBackdrop,
        onDrawBehind = onDrawBehind,
        onDrawSurface = onDrawSurface,
        onDrawFront = onDrawFront
    )
}

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
    accentColor: Color? = null,
    pagerState: PagerState? = null,
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

    val existingAppBackdrop = backdrop ?: remember { emptyBackdrop() }

    val blurPx = with(density) { BLUR_RADIUS_DP.dp.toPx() }
    val maxLensPx = with(density) { LENS_MAX_DP.dp.toPx() }
    val lensHeightPx = maxLensPx * LENS_HEIGHT
    val lensAmountPx = maxLensPx * LENS_AMOUNT

    val adaptiveNeutralSurfaceColor = if (isDark || isAmoled) {
        Color(0xFF121212)
    } else {
        MaterialTheme.colorScheme.surface
    }

    // Directional specular border catching overhead ambient light
    val specularBorderBrush = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = if (isDark) 0.35f else 0.65f),
            Color.White.copy(alpha = 0.08f),
            Color.Transparent
        )
    )

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
                    elevation = if (isDark) 12.dp else 6.dp,
                    shape = containerShape,
                    spotColor = Color.Black.copy(alpha = if (isDark) 0.60f else 0.12f),
                    ambientColor = Color.Black.copy(alpha = 0.05f)
                )
                .drawBackdrop(
                    backdrop = existingAppBackdrop,
                    shape = { containerShape },
                    effects = {
                        colorControls(saturation = 1f + 0.5f * 1f)
                        blur(blurPx)

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            lens(
                                refractionHeight = lensHeightPx,
                                refractionAmount = lensAmountPx,
                                depthEffect = true,
                                chromaticAberration = true,
                            )
                        }
                    },
                    highlight = { Highlight.Default },
                    shadow = { Shadow.Default },
                    onDrawSurface = {
                        drawRect(
                            color = adaptiveNeutralSurfaceColor.copy(alpha = 0.4f),
                            size = size
                        )
                    },
                    backdropScale = 0.33f,
                )
                // Specular Chamfer Stroke
                .border(
                    BorderStroke(width = 1.dp, brush = specularBorderBrush),
                    containerShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Specular apex highlight line
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(1.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = if (isDark) 0.40f else 0.70f),
                                Color.Transparent
                            )
                        )
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
                // Using mutableFloatStateOf read inside layout/draw lambda (or Animatable) prevents recompositions
                var indicatorOffsetPx by remember { mutableFloatStateOf(getTargetOffsetPx(selectedIndex)) }
                val indicatorAnimatable = remember { Animatable(getTargetOffsetPx(selectedIndex)) }
                var isDragging by remember { mutableStateOf(false) }
                var isSnappingAfterDrag by remember { mutableStateOf(false) }
                var dragVelocity by remember { mutableFloatStateOf(0f) }
                var lastHoveredIndex by remember { mutableIntStateOf(selectedIndex) }

                // Continuous spring-damper dynamic stretch & bounce
                val dropletAspectAnim = remember { Animatable(1f) }

                // Two-Way Sync (Pager Swiping Updates Pill):
                // When the user swipes pages directly on the screen (or when pager transitions),
                // interpolate the bottom pill position smoothly using:
                // pagerState.currentPage + pagerState.currentPageOffsetFraction
                if (pagerState != null) {
                    LaunchedEffect(pagerState, isDragging, isSnappingAfterDrag, slotWidthPx, basePillWidthPx) {
                        if (!isDragging && !isSnappingAfterDrag) {
                            snapshotFlow { pagerState.currentPage + pagerState.currentPageOffsetFraction }
                                .collect { progress ->
                                    if (!isDragging && !isSnappingAfterDrag) {
                                        val minOffset = (slotWidthPx - basePillWidthPx) / 2f
                                        val targetPx = (slotWidthPx * progress) + minOffset
                                        indicatorOffsetPx = targetPx
                                        val currentTab = progress.roundToInt().coerceIn(0, itemCount - 1)
                                        lastHoveredIndex = currentTab
                                    }
                                }
                        }
                    }
                } else {
                    // Fallback sync position on external index change when no pagerState is attached
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
                }

                val dropletPillShape = RoundedCornerShape(percent = 50)

                // -------------------------------------------------------------
                // 1. VISUAL RENDERING: PURE GLASS WATER DROPLET LENS
                // - Decoupled Drag via GraphicsLayer / Offset lambda:
                // - Read indicatorOffsetPx and calculate scale inside graphicsLayer / offset { }
                //   so drag updates only trigger drawing/layout passes, NOT full recompositions.
                // - NO manual gradient fills or opaque colors.
                // - Purely transparent glass lens with high-intensity backdrop blur filter (20px).
                // - Sharp, thin white arc specular highlight along top edge for 3D depth.
                // -------------------------------------------------------------
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(basePillWidth)
                        .height(pillHeight)
                        .graphicsLayer {
                            translationX = indicatorOffsetPx
                            // Calculate dynamic stretch inside graphicsLayer so micro-drags do not cause recomposition
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

                            scaleX = dynamicScaleX
                            scaleY = dynamicScaleY
                        }
                        .clip(dropletPillShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (isDark) 0.15f else 0.25f),
                                    Color.Transparent
                                ),
                                radius = 90f
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (isDark) 0.60f else 0.85f),
                                    Color.White.copy(alpha = 0.10f),
                                    Color.White.copy(alpha = if (isDark) 0.30f else 0.40f)
                                )
                            ),
                            shape = dropletPillShape
                        )
                )

                // Navigation Tab Icons Row with pointer drag gesture detection
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(totalWidthPx, itemCount, selectedIndex, pagerState) {
                            detectHorizontalDragGestures(
                                onDragStart = { _ ->
                                    isDragging = true
                                    isSnappingAfterDrag = false
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
                                    val effectivePillDelta = newOffset - indicatorOffsetPx
                                    indicatorOffsetPx = newOffset
                                    dragVelocity = dragAmount * 60f

                                    // Fractional drag progress from the navigation bar pill:
                                    val tabWidth = slotWidthPx
                                    val initialTabCenterOffset = (tabWidth - basePillWidthPx) / 2f
                                    val dragOffsetX = newOffset - initialTabCenterOffset
                                    val pageCount = pagerState?.pageCount ?: itemCount
                                    val targetPageOffset = (dragOffsetX / tabWidth).coerceIn(0f, (pageCount - 1).toFloat())

                                    // Haptic feedback when crossing nearest tab threshold
                                    val hoveredIndex = targetPageOffset.roundToInt().coerceIn(0, itemCount - 1)
                                    if (hoveredIndex != lastHoveredIndex) {
                                        lastHoveredIndex = hoveredIndex
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                },
                                onDragEnd = {
                                    isDragging = false

                                    val tabWidth = slotWidthPx
                                    val initialTabCenterOffset = (tabWidth - basePillWidthPx) / 2f
                                    val dragOffsetX = indicatorOffsetPx - initialTabCenterOffset
                                    val pageCount = pagerState?.pageCount ?: itemCount
                                    val targetPageOffset = (dragOffsetX / tabWidth).coerceIn(0f, (pageCount - 1).toFloat())
                                    val nearestIndex = targetPageOffset.roundToInt().coerceIn(0, pageCount - 1)

                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (nearestIndex != selectedIndex && nearestIndex in navItems.indices) {
                                        onCategorySelected(navItems[nearestIndex].serviceCategory)
                                    }

                                    isSnappingAfterDrag = true
                                    val snapTargetPx = getTargetOffsetPx(nearestIndex)

                                    // Page Release & Snap:
                                    // Only launch animateScrollToPage once inside onDragEnd, never inside onDrag.
                                    if (pagerState != null) {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(
                                                page = nearestIndex,
                                                animationSpec = spring(
                                                    dampingRatio = 0.70f,
                                                    stiffness = 340f
                                                )
                                            )
                                        }
                                    }

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
                                        isSnappingAfterDrag = false
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
                                    val targetIndex = selectedIndex
                                    isSnappingAfterDrag = true
                                    val snapTargetPx = getTargetOffsetPx(targetIndex)

                                    if (pagerState != null) {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(targetIndex)
                                        }
                                    }

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
                                        isSnappingAfterDrag = false
                                    }
                                    coroutineScope.launch {
                                        dropletAspectAnim.animateTo(
                                            targetValue = 1f,
                                            animationSpec = spring(
                                                dampingRatio = 0.54f,
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
                    val unselectedColor = if (isDark || isAmoled) Color(0xFF94A3B8) else Color(0xFF64748B)
                    val selectedColor = activeSectionAccent

                    navItems.forEachIndexed { index, item ->
                        val isSelected = index == selectedIndex
                        val iconColor by animateColorAsState(
                            targetValue = if (isSelected) selectedColor else unselectedColor,
                            animationSpec = tween(durationMillis = 180),
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
                                        if (pagerState != null) {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(
                                                    page = index,
                                                    animationSpec = spring(
                                                        dampingRatio = 0.72f,
                                                        stiffness = 340f
                                                    )
                                                )
                                            }
                                        }
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
                                        // Read indicator offset inside graphicsLayer to avoid recomposition passes
                                        val tabCenterPx = tabCenterPoints[index]
                                        val dropletCenterPx = indicatorOffsetPx + (basePillWidthPx / 2f)
                                        val distanceToDroplet = abs(tabCenterPx - dropletCenterPx)
                                        val proximity = (1f - (distanceToDroplet / slotWidthPx)).coerceIn(0f, 1f)
                                        val baseScale = 1.0f + (proximity * 0.18f)
                                        val liftYPx = -2.0f * proximity * density.density

                                        scaleX = baseScale
                                        scaleY = baseScale
                                        translationY = liftYPx
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}

