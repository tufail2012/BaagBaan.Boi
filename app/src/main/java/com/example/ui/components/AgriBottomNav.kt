package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class AgriNavItem(
    val title: String,
    val serviceCategory: String,
    val icon: ImageVector,
    val testTag: String
)

private val PILL_INSET = 6.dp
private val TAB_VERTICAL_PADDING = 9.dp
private val TAB_ICON_LABEL_GAP = 2.dp

/**
 * Floating Pill Bottom Navigation Bar for Baagbaan BOI.
 *
 * The selection indicator's motion (lift/overshoot, travel spring, launch
 * stretch and brake squash, glass-to-flat hand-off) is driven by
 * [GlassPillMotion]. Both a direct drag on the pill and a page swipe on
 * [pagerState] feed the same physics through startDrag/dragTo/release, so
 * either gesture gets the identical liquid feel.
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
    val pillShape = RoundedCornerShape(percent = 50)
    val container = MaterialTheme.colorScheme.surface
    val n = navItems.size

    val currentSelectedIndex by rememberUpdatedState(selectedIndex)
    var rowSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    val gapPx = with(density) { 6.dp.toPx() }
    val tabStepPx = if (rowSize.width > 0 && n > 0) (rowSize.width + gapPx) / n else 0f
    val tabWidthPx = if (rowSize.width > 0 && n > 0) (rowSize.width - gapPx * (n - 1)) / n else 0f

    val motion = remember { GlassPillMotion(coroutineScope, selectedIndex) }
    LaunchedEffect(tabStepPx) {
        motion.stepDp = with(density) { tabStepPx.toDp().value }
    }

    // External selection (tap on a tab, or deep-link) that isn't already
    // where the pill's drag/travel has it — lift and travel there.
    LaunchedEffect(selectedIndex) {
        if (!motion.isFlat && motion.index == selectedIndex) return@LaunchedEffect
        motion.animateTo(selectedIndex)
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

    var lastHapticTab by remember { mutableStateOf(selectedIndex) }

    Box(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 10.dp)
            .padding(bottom = 2.dp)
            .fillMaxWidth()
            .clip(pillShape)
            .liquidGlassNav(shape = pillShape, backdrop = backdrop)
            .then(
                if (backdrop == null || !isGlassSupported()) {
                    Modifier
                        .hazeEffect(state = hazeState, style = HazeMaterials.regular(container))
                        .glassEdge(pillShape)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = PILL_INSET, vertical = PILL_INSET)
    ) {
        if (tabWidthPx > 0f) {
            val restSize = Size(tabWidthPx, with(density) { rowSize.height.toFloat() })
            val liftedSize = Size(tabWidthPx * 1.06f, restSize.height + with(density) { 9.dp.toPx() })
            val liveSize = motion.liveSize(restSize, liftedSize)

            Box(
                modifier = Modifier
                    .width(with(density) { liveSize.width.toDp() })
                    .height(with(density) { liveSize.height.toDp() })
                    .graphicsLayer {
                        val centerPx = motion.position * tabStepPx + tabWidthPx / 2f
                        translationX = centerPx - liveSize.width / 2f
                        translationY = -(liveSize.height - restSize.height) / 2f
                    }
                    .clip(pillShape)
                    .then(
                        if (backdrop != null && isGlassSupported()) {
                            Modifier.liquidGlassNav(shape = pillShape, backdrop = backdrop)
                        } else {
                            Modifier
                        }
                    )
                    .background(
                        glassIndicatorColor().copy(alpha = 0.5f * (1f - motion.glassPresence * 0.6f)),
                        pillShape
                    )
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { rowSize = it }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            motion.startDrag()
                        },
                        onDragCancel = {
                            motion.release(currentSelectedIndex)
                        },
                        onDragEnd = {
                            val newIndex = motion.position.roundToInt().coerceIn(0, navItems.lastIndex)
                            motion.release(newIndex)
                            if (newIndex != currentSelectedIndex) {
                                onCategorySelected(navItems[newIndex].serviceCategory)
                            }
                        },
                        onHorizontalDrag = { _, delta ->
                            if (tabStepPx > 0f) {
                                val next = (motion.position + delta / tabStepPx)
                                    .coerceIn(0f, navItems.lastIndex.toFloat())
                                motion.dragTo(next)
                                val approxTab = next.roundToInt()
                                if (approxTab != lastHapticTab) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    lastHapticTab = approxTab
                                }
                            }
                        }
                    )
                },
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val activeTabIndex = motion.position.roundToInt().coerceIn(0, navItems.lastIndex)
            navItems.forEachIndexed { index, item ->
                val isSelected = index == activeTabIndex
                val unselectedColor = if (isDark || isAmoled) Color(0xFF94A3B8) else Color(0xFF64748B)

                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()

                val pressedScale by animateFloatAsState(
                    targetValue = if (isPressed) 0.97f else 1.0f,
                    animationSpec = spring(stiffness = 700f, dampingRatio = 1.0f),
                    label = "tabItemPressedScale"
                )

                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.22f else 1f,
                    animationSpec = spring(stiffness = 380f, dampingRatio = 0.55f),
                    label = "tabScale"
                )
                val iconColor by animateColorAsState(
                    targetValue = if (isSelected) activeSectionAccent else unselectedColor,
                    animationSpec = tween(200),
                    label = "tabIconColor"
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .testTag(item.testTag)
                        .graphicsLayer {
                            scaleX = pressedScale
                            scaleY = pressedScale
                        }
                        .clip(RoundedCornerShape(percent = 50))
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onCategorySelected(item.serviceCategory)
                            }
                        )
                        .padding(vertical = TAB_VERTICAL_PADDING)
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = iconColor,
                        modifier = Modifier
                            .size(25.dp)
                            .graphicsLayer { scaleX = scale; scaleY = scale }
                    )
                    Spacer(Modifier.height(TAB_ICON_LABEL_GAP))
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.labelSmall,
                        color = unselectedColor,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
