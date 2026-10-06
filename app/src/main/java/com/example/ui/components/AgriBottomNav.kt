package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.PagerState
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.ui.components.backdrop.Backdrop as NavBackdrop
import com.example.ui.components.backdrop.backdrops.layerBackdrop
import com.example.ui.components.backdrop.backdrops.rememberLayerBackdrop
import com.example.ui.haptics.Haptic
import com.example.ui.haptics.rememberHaptics
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlin.math.roundToInt

data class AgriNavItem(
    val title: String,
    val serviceCategory: String,
    val icon: ImageVector,
    val testTag: String
)

// Same numbers as the reference design (FloatingBottomBar / GlassNavBar / FloatingTabBar defaults).
private val PILL_INSET = 6.dp
private val TAB_VERTICAL_PADDING = 9.dp
private val TAB_ICON_LABEL_GAP = 2.dp
private val TAB_SPACING = 0.dp
private val BAR_GUTTER = 16.dp
private val GESTURE_BAR_FLOOR = 15.dp

/**
 * The reference design tints the selected tab with the plain glass content colour (black /
 * white). Set to true to tint it with the section accent colour instead.
 */
private const val USE_SECTION_ACCENT = false

private val navBarInsets: WindowInsets
    @Composable get() = WindowInsets.navigationBars.union(WindowInsets(bottom = GESTURE_BAR_FLOOR))

/**
 * Floating pill bottom navigation bar — a 1:1 port of the reference app's expanded
 * liquid-glass tab bar (GlassNavBar + FloatingTabBar's ExpandedTabs).
 *
 * Glass: [glassBackdrop] must be a vendored LayerBackdrop recorded from the
 * page (see AgriCropMainScreen). When it is null or glass is unsupported the
 * bar falls back to the Haze material with a flat selection pill.
 *
 * Pill: rests flat under the tabs; while lifted/travelling it is a clear lens
 * ([GlassSelectionPill]) refracting the page, the bar glass and the tab row.
 * Motion physics live in [GlassPillMotion]. A page swipe on [pagerState] feeds
 * the same physics as a direct drag on the bar.
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
    glassBackdrop: NavBackdrop? = null
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

    val haptics = rememberHaptics()
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    val selectedIndex = remember(selectedCategory) {
        val idx = navItems.indexOfFirst { item ->
            selectedCategory.equals(item.serviceCategory, ignoreCase = true) ||
                    (selectedCategory.equals("Local", ignoreCase = true) && item.serviceCategory.equals("Local Plants", ignoreCase = true)) ||
                    (selectedCategory.equals("Garden", ignoreCase = true) && item.serviceCategory.equals("Garden Planning", ignoreCase = true))
        }
        if (idx >= 0) idx else 0
    }
    val currentSelectedIndex by rememberUpdatedState(selectedIndex)

    val pillShape = remember { RoundedCornerShape(percent = 50) }
    val tabShape = remember { RoundedCornerShape(percent = 100) }
    val container = MaterialTheme.colorScheme.surface
    val n = navItems.size

    // Glass-adaptive colours, exactly as the reference app's GlassNavBar.
    val contentColor = glassContentColor()
    val selectedColor = if (USE_SECTION_ACCENT) {
        accentColor ?: MaterialTheme.colorScheme.primary
    } else {
        contentColor
    }
    val unselectedColor = contentColor.copy(alpha = 0.65f)
    val indicatorColor = glassIndicatorColor().copy(alpha = 0.5f)

    val useGlass = glassBackdrop != null && isGlassSupported()

    // --- Pill geometry (reference ExpandedTabs) -------------------------------------
    val motion = remember { GlassPillMotion(coroutineScope, selectedIndex) }
    val barGlass = rememberLayerBackdrop()
    val tabRow = rememberLayerBackdrop()

    var rowSize by remember { mutableStateOf(IntSize.Zero) }
    var lastHapticTab by remember { mutableIntStateOf(selectedIndex) }
    var isProgrammaticNav by remember { mutableStateOf(false) }

    val tabSpacingPx = with(density) { TAB_SPACING.toPx() }
    val tabWidthPx = if (rowSize.width > 0) (rowSize.width - tabSpacingPx * (n - 1)) / n else 0f
    val tabStepPx = if (rowSize.width > 0) (rowSize.width + tabSpacingPx) / n else 0f
    motion.stepDp = tabStepPx / density.density

    val padPx = with(density) { PILL_INSET.toPx() }
    val rowHeightPx = rowSize.height.toFloat()
    val restSize = Size(tabWidthPx, rowHeightPx)
    val liftedHeightPx = rowHeightPx + padPx * 2 + with(density) { PILL_GROW_HEIGHT.toPx() }
    val liftedSize = if (rowHeightPx > 0f) {
        Size(liftedHeightPx * tabWidthPx / rowHeightPx, liftedHeightPx)
    } else {
        restSize
    }
    val pillCenterInRow: (Float) -> Offset = { position ->
        val x = position * tabStepPx + tabWidthPx / 2f
        Offset(if (isRtl) rowSize.width - x else x, rowHeightPx / 2f)
    }
    val showPill = tabWidthPx > 0f

    // External selection (tap on a tab, or deep-link) that isn't already where
    // the pill's drag/travel has it — lift and travel there.
    LaunchedEffect(selectedIndex) {
        if (!motion.isFlat && motion.index == selectedIndex) return@LaunchedEffect
        motion.animateTo(selectedIndex)
        lastHapticTab = selectedIndex
    }

    // A page swipe on the content drives the exact same physics a direct
    // pill-drag would: start the lift when the swipe begins, follow the
    // continuous page fraction, release onto the page it settles on.
    if (pagerState != null) {
        LaunchedEffect(pagerState) {
            var wasScrolling = false
            snapshotFlow {
                Triple(pagerState.isScrollInProgress, pagerState.currentPage, pagerState.currentPageOffsetFraction)
            }.collect { (scrolling, page, offsetFraction) ->
                if (isProgrammaticNav) {
                    if (!scrolling && wasScrolling) {
                        isProgrammaticNav = false
                    }
                    wasScrolling = scrolling
                    return@collect
                }
                if (scrolling && !wasScrolling) {
                    motion.startDrag()
                }
                if (scrolling) {
                    motion.dragTo((page + offsetFraction).coerceIn(0f, (n - 1).toFloat()))
                } else if (wasScrolling) {
                    motion.release(page.coerceIn(0, n - 1))
                }
                wasScrolling = scrolling
            }
        }
    }

    // Unclipped outer box: the lifted pill stands past the bar's edges, so it is
    // drawn here, over the bar, rather than inside the bar's clip.
    Box(
        modifier = modifier
            .windowInsetsPadding(navBarInsets)
            .padding(horizontal = BAR_GUTTER)
            .padding(bottom = 2.dp)
            .fillMaxWidth()
    ) {
        // The bar's glass is exported for the lens to refract.
        CompositionLocalProvider(LocalNavGlassExport provides barGlass) {
            Box(
                modifier = Modifier
                    .clip(pillShape)
                    .then(
                        if (useGlass && glassBackdrop != null) {
                            Modifier.navLiquidGlass(shape = pillShape, backdrop = glassBackdrop)
                        } else {
                            Modifier
                                .hazeEffect(state = hazeState, style = HazeMaterials.regular(container))
                                .glassEdge(pillShape)
                        }
                    )
                    .padding(PILL_INSET)
            ) {
                if (showPill && (!useGlass || motion.isFlat)) {
                    FlatSelectionPill(
                        color = indicatorColor,
                        modifier = Modifier
                            .matchParentSize()
                            .pillPlacement(
                                center = { pillCenterInRow(motion.position) },
                                size = { motion.liveSize(restSize, liftedSize) },
                            ),
                    )
                }

                NavTabRow(
                    selectedIndex = selectedIndex,
                    navItems = navItems,
                    useGlass = useGlass,
                    tabRow = tabRow,
                    tabShape = tabShape,
                    selectedColor = selectedColor,
                    unselectedColor = unselectedColor,
                    onRowSize = { rowSize = it },
                    dragModifier = Modifier.pointerInput(n, tabStepPx, currentSelectedIndex, isRtl) {
                        if (tabStepPx <= 0f) return@pointerInput

                        // In tab order, whichever way the row runs.
                        val direction = if (isRtl) -1f else 1f
                        var totalDrag = 0f
                        detectHorizontalDragGestures(
                            onDragStart = {
                                totalDrag = 0f
                                motion.startDrag()
                            },
                            onDragCancel = { motion.release(currentSelectedIndex) },
                            onDragEnd = {
                                val ratio = totalDrag / tabStepPx
                                val shift = when {
                                    ratio > 0.35f -> maxOf(1, ratio.roundToInt())
                                    ratio < -0.35f -> minOf(-1, ratio.roundToInt())
                                    else -> 0
                                }
                                val newIndex = (currentSelectedIndex + shift).coerceIn(0, n - 1)
                                motion.release(newIndex)
                                if (newIndex != currentSelectedIndex) {
                                    isProgrammaticNav = true
                                    onCategorySelected(navItems[newIndex].serviceCategory)
                                }
                            },
                            onHorizontalDrag = { _, delta ->
                                totalDrag += delta * direction
                                val dragOffset = when {
                                    totalDrag > 0 && currentSelectedIndex == n - 1 -> totalDrag * 0.25f
                                    totalDrag < 0 && currentSelectedIndex == 0 -> totalDrag * 0.25f
                                    else -> totalDrag
                                }
                                motion.dragTo(currentSelectedIndex + dragOffset / tabStepPx)

                                val approximateTab = (currentSelectedIndex + dragOffset / tabStepPx)
                                    .coerceIn(0f, (n - 1).toFloat())
                                    .roundToInt()
                                if (approximateTab != lastHapticTab) {
                                    haptics.play(Haptic.Tick)
                                    lastHapticTab = approximateTab
                                }
                            }
                        )
                    },
                    onTabClick = { index, item ->
                        if (index != currentSelectedIndex) {
                            isProgrammaticNav = true
                            haptics.play(Haptic.Select)
                        }
                        onCategorySelected(item.serviceCategory)
                    }
                )
            }
        }

        if (useGlass && glassBackdrop != null && showPill && !motion.isFlat) {
            GlassSelectionPill(
                motion = motion,
                fillColor = indicatorColor,
                page = glassBackdrop,
                barGlass = barGlass,
                tabRow = tabRow,
                modifier = Modifier
                    .matchParentSize()
                    .pillPlacement(
                        center = { pillCenterInRow(motion.position) + Offset(padPx, padPx) },
                        size = { motion.liveSize(restSize, liftedSize) },
                    ),
            )
        }
    }
}

/** The tab row: equal-weight tabs, recorded into [tabRow] so the lens can refract it. */
@Composable
private fun NavTabRow(
    selectedIndex: Int,
    navItems: List<AgriNavItem>,
    useGlass: Boolean,
    tabRow: com.example.ui.components.backdrop.backdrops.LayerBackdrop,
    tabShape: androidx.compose.ui.graphics.Shape,
    selectedColor: Color,
    unselectedColor: Color,
    onRowSize: (IntSize) -> Unit,
    dragModifier: Modifier,
    onTabClick: (Int, AgriNavItem) -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        horizontalArrangement = Arrangement.spacedBy(TAB_SPACING),
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged(onRowSize)
            .then(if (useGlass) Modifier.layerBackdrop(tabRow) else Modifier)
            .then(dragModifier)
    ) {
        navItems.forEachIndexed { index, item ->
            val isSelected = index == selectedIndex
            val tint = if (isSelected) selectedColor else unselectedColor
            val iconScale by animateFloatAsState(
                targetValue = if (isSelected) 1.08f else 1f,
                animationSpec = NavGlassSpring,
                label = "glassTabScale"
            )
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .testTag(item.testTag)
                    .clip(tabShape)
                    .clickable(
                        onClick = { onTabClick(index, item) },
                        indication = LocalIndication.current,
                        interactionSource = remember { MutableInteractionSource() }
                    )
                    .padding(vertical = TAB_VERTICAL_PADDING)
            ) {
                Box(
                    modifier = Modifier.graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    }
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = tint,
                        modifier = Modifier.size(25.dp)
                    )
                }
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.labelSmall,
                    color = tint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = TAB_ICON_LABEL_GAP)
                )
            }
        }
    }
}
