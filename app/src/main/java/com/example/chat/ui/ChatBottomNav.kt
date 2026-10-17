package com.example.chat.ui

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.chat.model.ChatTab
import com.example.ui.animation.IosMotion
import com.example.ui.components.FlatSelectionPill
import com.example.ui.components.GlassPillMotion
import com.example.ui.components.GlassSelectionPill
import com.example.ui.components.LocalNavGlassExport
import com.example.ui.components.NavGlassSpring
import com.example.ui.components.PILL_GROW_HEIGHT
import com.example.ui.components.backdrop.Backdrop as NavBackdrop
import com.example.ui.components.backdrop.backdrops.LayerBackdrop
import com.example.ui.components.backdrop.backdrops.layerBackdrop
import com.example.ui.components.backdrop.backdrops.rememberLayerBackdrop
import com.example.ui.components.glassContentColor
import com.example.ui.components.glassIndicatorColor
import com.example.ui.components.isGlassSupported
import com.example.ui.components.navLiquidGlass
import com.example.ui.components.pillPlacement
import com.example.ui.haptics.Haptic
import com.example.ui.haptics.rememberHaptics
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlin.math.roundToInt

data class ChatNavItem(
    val tab: ChatTab,
    val icon: ImageVector,
    val testTag: String
)

// Same numbers as AgriBottomNav.kt
private val PILL_INSET = 6.dp
private val TAB_VERTICAL_PADDING = 9.dp
private val TAB_ICON_LABEL_GAP = 2.dp
private val TAB_SPACING = 0.dp
private val BAR_GUTTER = 16.dp
private val GESTURE_BAR_FLOOR = 15.dp

/**
 * Theme colours only: black on light theme, white on dark theme.
 */
private const val USE_SECTION_ACCENT = false

private val navBarInsets: WindowInsets
    @Composable get() = WindowInsets.navigationBars.union(WindowInsets(bottom = GESTURE_BAR_FLOOR))

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun ChatBottomNav(
    selectedTab: ChatTab,
    onTabSelected: (ChatTab) -> Unit,
    pagerState: PagerState?,
    glassBackdrop: NavBackdrop?,
    hazeState: HazeState? = null,
    modifier: Modifier = Modifier
) {
    val navItems = remember {
        listOf(
            ChatNavItem(ChatTab.CHATS, Icons.AutoMirrored.Filled.Chat, "chat_nav_chats"),
            ChatNavItem(ChatTab.CALLS, Icons.Default.Call, "chat_nav_calls"),
            ChatNavItem(ChatTab.GROUPS, Icons.Default.Groups, "chat_nav_groups"),
            ChatNavItem(ChatTab.SETTINGS, Icons.Default.Settings, "chat_nav_settings")
        )
    }

    val haptics = rememberHaptics()
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val context = LocalContext.current
    val reduceMotion = remember(context) { IosMotion.isReducedMotion(context) }
    val glassSpec: AnimationSpec<Float> = if (reduceMotion) snap() else NavGlassSpring
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    val selectedIndex = remember(selectedTab) {
        navItems.indexOfFirst { it.tab == selectedTab }.coerceAtLeast(0)
    }
    val currentSelectedIndex by rememberUpdatedState(selectedIndex)

    val pillShape = remember { RoundedCornerShape(percent = 50) }
    val tabShape = remember { RoundedCornerShape(percent = 100) }
    val container = MaterialTheme.colorScheme.surface
    val n = navItems.size

    // Glass-adaptive colours, matching AgriBottomNav.kt with USE_SECTION_ACCENT = false
    val contentColor = glassContentColor()
    val selectedColor = if (USE_SECTION_ACCENT) {
        MaterialTheme.colorScheme.primary
    } else {
        contentColor
    }
    val unselectedColor = contentColor.copy(alpha = 0.65f)
    val indicatorColor = glassIndicatorColor().copy(alpha = 0.5f)

    val useGlass = glassBackdrop != null && isGlassSupported()

    // --- Pill geometry (same as AgriBottomNav) ---
    val motion = remember { GlassPillMotion(coroutineScope, selectedIndex) }
    motion.reduceMotion = reduceMotion
    val barGlass = rememberLayerBackdrop()
    val tabRow = rememberLayerBackdrop()

    var rowSize by remember { mutableStateOf(IntSize.Zero) }
    var lastHapticTab by remember { mutableIntStateOf(selectedIndex) }

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

    // External selection (tap on a tab, or programmatic tab switch)
    LaunchedEffect(selectedIndex) {
        if (!motion.isFlat && motion.index == selectedIndex) return@LaunchedEffect
        motion.animateTo(selectedIndex)
        lastHapticTab = selectedIndex
    }

    // Page swipe sync: starts the lift, follows continuous page fraction, releases on settled page
    if (pagerState != null) {
        LaunchedEffect(pagerState) {
            var swiping = false
            snapshotFlow {
                Triple(pagerState.isScrollInProgress, pagerState.currentPage, pagerState.currentPageOffsetFraction)
            }.collect { (scrolling, page, offsetFraction) ->
                val nowSwiping = scrolling && offsetFraction != 0f
                if (nowSwiping && !swiping) motion.startDrag()
                if (nowSwiping) {
                    motion.dragTo((page + offsetFraction).coerceIn(0f, (n - 1).toFloat()))
                } else if (swiping) {
                    motion.release(page.coerceIn(0, n - 1))
                }
                swiping = nowSwiping
            }
        }
    }

    Box(
        modifier = modifier
            .windowInsetsPadding(navBarInsets)
            .padding(horizontal = BAR_GUTTER)
            .padding(bottom = 2.dp)
            .fillMaxWidth()
    ) {
        CompositionLocalProvider(LocalNavGlassExport provides barGlass) {
            Box(
                modifier = Modifier
                    .clip(pillShape)
                    .then(
                        if (useGlass && glassBackdrop != null) {
                            Modifier.navLiquidGlass(shape = pillShape, backdrop = glassBackdrop)
                        } else if (hazeState != null) {
                            Modifier
                                .hazeEffect(state = hazeState, style = HazeMaterials.regular(container))
                                .border(0.5.dp, Color.White.copy(alpha = 0.10f), pillShape)
                        } else {
                            Modifier
                                .background(container.copy(alpha = 0.88f), pillShape)
                                .border(0.5.dp, contentColor.copy(alpha = 0.12f), pillShape)
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
                    glassSpec = glassSpec,
                    onRowSize = { rowSize = it },
                    dragModifier = Modifier.pointerInput(n, tabStepPx, currentSelectedIndex, isRtl) {
                        if (tabStepPx <= 0f) return@pointerInput

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
                                    onTabSelected(navItems[newIndex].tab)
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
                            haptics.play(Haptic.Select)
                        }
                        onTabSelected(item.tab)
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

@Composable
private fun NavTabRow(
    selectedIndex: Int,
    navItems: List<ChatNavItem>,
    useGlass: Boolean,
    tabRow: LayerBackdrop,
    tabShape: Shape,
    selectedColor: Color,
    unselectedColor: Color,
    glassSpec: AnimationSpec<Float>,
    onRowSize: (IntSize) -> Unit,
    dragModifier: Modifier,
    onTabClick: (Int, ChatNavItem) -> Unit,
) {
    Row(
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
                animationSpec = glassSpec,
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
                        contentDescription = item.tab.title,
                        tint = tint,
                        modifier = Modifier.size(25.dp)
                    )
                }
                Text(
                    text = item.tab.title,
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
