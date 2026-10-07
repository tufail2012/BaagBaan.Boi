package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.animation.IosMotion
import com.example.ui.haptics.Haptic
import com.example.ui.haptics.rememberHaptics
import kotlin.math.roundToInt

@Composable
fun AgriMotionTabs(
    titles: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    selectedTextColor: Color,
    unselectedTextColor: Color,
    pill: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
    testTags: List<String> = emptyList(),
    pillInset: Dp = 4.dp,
    tabContent: (@Composable (index: Int, isSelected: Boolean, contentColor: Color, scale: Float) -> Unit)? = null
) {
    val n = titles.size
    if (n == 0) return

    val haptics = rememberHaptics()
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val context = LocalContext.current
    val reduceMotion = remember(context) { IosMotion.isReducedMotion(context) }
    val textSpec: AnimationSpec<Float> = if (reduceMotion) snap() else NavGlassSpring
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    val motion = remember { GlassPillMotion(coroutineScope, selectedIndex) }
    motion.reduceMotion = reduceMotion

    var rowSize by remember { mutableStateOf(IntSize.Zero) }
    var lastHapticTab by remember { mutableIntStateOf(selectedIndex) }
    val currentSelectedIndex by rememberUpdatedState(selectedIndex)

    val rowWidthPx = rowSize.width.toFloat()
    val rowHeightPx = rowSize.height.toFloat()
    val tabWidthPx = if (n > 0) rowWidthPx / n else 0f
    val tabStepPx = tabWidthPx
    motion.stepDp = if (density.density > 0f) tabStepPx / density.density else 0f

    val padPx = with(density) { pillInset.toPx() }
    val restSize = Size(tabWidthPx, rowHeightPx)
    val liftedHeightPx = rowHeightPx + padPx * 2 + with(density) { PILL_GROW_HEIGHT.toPx() }
    val liftedSize = if (rowHeightPx > 0f) {
        Size(liftedHeightPx * tabWidthPx / rowHeightPx, liftedHeightPx)
    } else {
        restSize
    }

    val pillCenterInRow: (Float) -> Offset = { position ->
        val x = position * tabStepPx + tabWidthPx / 2f
        Offset(if (isRtl) rowWidthPx - x else x, rowHeightPx / 2f)
    }

    LaunchedEffect(selectedIndex) {
        if (!motion.isFlat && motion.index == selectedIndex) return@LaunchedEffect
        motion.animateTo(selectedIndex)
        lastHapticTab = selectedIndex
    }

    val dragModifier = Modifier.pointerInput(n, tabStepPx, currentSelectedIndex, isRtl) {
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
                    onSelect(newIndex)
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
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { rowSize = it }
    ) {
        // Pill placement: unclipped so lifted/growing pill stays visible while enlarged past track bounds
        if (tabWidthPx > 0f) {
            val pillModifier = Modifier
                .matchParentSize()
                .pillPlacement(
                    center = { pillCenterInRow(motion.position) },
                    size = { motion.liveSize(restSize, liftedSize) }
                )
            pill(pillModifier)
        }

        // Tab items row with gesture handling and tabs
        Row(
            modifier = Modifier
                .fillMaxSize()
                .then(dragModifier),
            verticalAlignment = Alignment.CenterVertically
        ) {
            titles.forEachIndexed { index, title ->
                val isSelected = index == selectedIndex
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) selectedTextColor else unselectedTextColor,
                    animationSpec = tween(durationMillis = 200),
                    label = "motionTabTextColor_$index"
                )
                val textScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.05f else 1.0f,
                    animationSpec = textSpec,
                    label = "motionTabTextScale_$index"
                )

                val tag = testTags.getOrNull(index)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .then(if (!tag.isNullOrEmpty()) Modifier.testTag(tag) else Modifier)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                if (index != currentSelectedIndex) {
                                    haptics.play(Haptic.Select)
                                    onSelect(index)
                                }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (tabContent != null) {
                        tabContent(index, isSelected, textColor, textScale)
                    } else {
                        Text(
                            text = title,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.graphicsLayer {
                                scaleX = textScale
                                scaleY = textScale
                            }
                        )
                    }
                }
            }
        }
    }
}
