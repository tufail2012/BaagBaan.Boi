package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
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
import kotlin.math.roundToInt

data class AgriNavItem(
    val title: String,
    val serviceCategory: String,
    val icon: ImageVector,
    val testTag: String
)

private val GlassSpring = androidx.compose.animation.core.spring<Float>(dampingRatio = 0.72f, stiffness = 320f)
private const val STRETCH = 0.16f
private const val SQUASH = 0.5f
private val PILL_INSET = 6.dp
private val TAB_VERTICAL_PADDING = 9.dp
private val TAB_ICON_LABEL_GAP = 2.dp

/**
 * Floating Pill Bottom Navigation Bar for Baagbaan BOI.
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

    var dragOffset by remember { mutableFloatStateOf(0f) }
    val currentSelectedIndex by rememberUpdatedState(selectedIndex)
    var rowSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    val gapPx = with(density) { 6.dp.toPx() }
    val n = navItems.size

    val tabWidthPx = if (rowSize.width > 0 && n > 0) (rowSize.width - gapPx * (n - 1)) / n else 0f
    val tabStepPx = if (rowSize.width > 0 && n > 0) (rowSize.width + gapPx) / n else 0f
    val pillTargetPx = if (tabStepPx > 0f) selectedIndex * tabStepPx + dragOffset else 0f

    val animatedPillOffset by animateFloatAsState(
        targetValue = pillTargetPx,
        animationSpec = GlassSpring,
        label = "bottomNavPillOffset"
    )

    val lag = if (tabStepPx > 0f) {
        (kotlin.math.abs(pillTargetPx - animatedPillOffset) / tabStepPx).coerceIn(0f, 1f)
    } else 0f

    var lastHapticTab by remember { mutableIntStateOf(selectedIndex) }
    LaunchedEffect(selectedIndex) { dragOffset = 0f }

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
                    Modifier.hazeEffect(state = hazeState, style = HazeMaterials.regular(container))
                } else {
                    Modifier
                }
            )
            .border(0.5.dp, Color.White.copy(alpha = 0.10f), pillShape)
            .padding(horizontal = PILL_INSET, vertical = PILL_INSET)
    ) {
        if (tabWidthPx > 0f) {
            Box(
                modifier = Modifier
                    .width(with(density) { tabWidthPx.toDp() })
                    .height(with(density) { rowSize.height.toDp() })
                    .graphicsLayer {
                        translationX = animatedPillOffset
                        scaleX = 1f + lag * STRETCH
                        scaleY = 1f - lag * STRETCH * SQUASH
                    }
                    .clip(pillShape)
                    .background(
                        brush = if (!isDark) {
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.28f),
                                    Color.White.copy(alpha = 0.18f),
                                    Color.White.copy(alpha = 0.16f)
                                ),
                                start = Offset.Zero,
                                end = Offset.Infinite
                            )
                        } else {
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.18f),
                                    Color.White.copy(alpha = 0.12f),
                                    Color.White.copy(alpha = 0.08f)
                                ),
                                start = Offset.Zero,
                                end = Offset.Infinite
                            )
                        },
                        shape = pillShape
                    )
                    .drawWithContent {
                        drawContent()
                        val w = size.width
                        val h = size.height
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
                        shape = pillShape
                    )
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { rowSize = it }
                .pointerInput(Unit) {
                    var totalDrag = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { totalDrag = 0f },
                        onDragCancel = { dragOffset = 0f },
                        onDragEnd = {
                            if (tabStepPx > 0f) {
                                val ratio = totalDrag / tabStepPx
                                val shift = when {
                                    ratio > 0.35f -> kotlin.math.max(1, ratio.roundToInt())
                                    ratio < -0.35f -> kotlin.math.min(-1, ratio.roundToInt())
                                    else -> 0
                                }
                                val newIndex = (currentSelectedIndex + shift).coerceIn(0, navItems.lastIndex)
                                if (newIndex != currentSelectedIndex) {
                                    onCategorySelected(navItems[newIndex].serviceCategory)
                                }
                            }
                            dragOffset = 0f
                        },
                        onHorizontalDrag = { _, delta ->
                            totalDrag += delta
                            val rawPx = when {
                                totalDrag > 0 && currentSelectedIndex == navItems.lastIndex -> totalDrag * 0.25f
                                totalDrag < 0 && currentSelectedIndex == 0 -> totalDrag * 0.25f
                                else -> totalDrag
                            }
                            dragOffset = rawPx
                            val approxTab = (currentSelectedIndex + dragOffset / tabStepPx)
                                .coerceIn(0f, navItems.lastIndex.toFloat())
                                .roundToInt()
                            if (approxTab != lastHapticTab) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                lastHapticTab = approxTab
                            }
                        }
                    )
                },
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            navItems.forEachIndexed { index, item ->
                val isSelected = index == selectedIndex
                val unselectedColor = if (isDark || isAmoled) Color(0xFF94A3B8) else Color(0xFF64748B)
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.08f else 1f,
                    animationSpec = GlassSpring,
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
                        .clip(RoundedCornerShape(percent = 50))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
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
