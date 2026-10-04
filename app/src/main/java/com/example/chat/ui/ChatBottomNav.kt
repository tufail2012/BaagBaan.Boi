package com.example.chat.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.chat.model.ChatTab
import com.example.ui.components.glassEdge
import com.example.ui.components.glassIndicatorColor
import com.example.ui.components.isAppInAmoledMode
import com.example.ui.components.isAppInDarkMode
import com.example.ui.components.isGlassSupported
import com.example.ui.components.liquidGlassNav
import com.kyant.backdrop.Backdrop
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

data class ChatNavItem(
    val tab: ChatTab,
    val icon: ImageVector,
    val testTag: String
)

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun ChatBottomNav(
    selectedTab: ChatTab,
    onTabSelected: (ChatTab) -> Unit,
    hazeState: HazeState? = null,
    accentColor: Color? = null,
    backdrop: Backdrop? = null,
    modifier: Modifier = Modifier
) {
    val items = remember {
        listOf(
            ChatNavItem(ChatTab.CHATS, Icons.AutoMirrored.Filled.Chat, "chat_nav_chats"),
            ChatNavItem(ChatTab.CALLS, Icons.Default.Call, "chat_nav_calls"),
            ChatNavItem(ChatTab.GROUPS, Icons.Default.Groups, "chat_nav_groups"),
            ChatNavItem(ChatTab.SETTINGS, Icons.Default.Settings, "chat_nav_settings")
        )
    }

    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val isDark = isAppInDarkMode()
    val isAmoled = isAppInAmoledMode()

    val selectedIndex = remember(selectedTab) {
        items.indexOfFirst { it.tab == selectedTab }.coerceAtLeast(0)
    }

    val activeAccent = accentColor ?: MaterialTheme.colorScheme.primary
    val pillShape = RoundedCornerShape(percent = 50)
    val container = MaterialTheme.colorScheme.surface

    var rowSize by remember { mutableStateOf(IntSize.Zero) }
    val gapPx = with(density) { 6.dp.toPx() }
    val n = items.size

    val tabWidthPx = if (rowSize.width > 0 && n > 0) (rowSize.width - gapPx * (n - 1)) / n else 0f
    val tabStepPx = if (rowSize.width > 0 && n > 0) (rowSize.width + gapPx) / n else 0f

    val pillOffsetAnimatable = remember { Animatable(0f) }
    var isInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(selectedIndex, tabStepPx) {
        if (tabStepPx > 0f) {
            val targetPx = selectedIndex * tabStepPx
            if (!isInitialized) {
                pillOffsetAnimatable.snapTo(targetPx)
                isInitialized = true
            } else {
                pillOffsetAnimatable.animateTo(
                    targetValue = targetPx,
                    animationSpec = spring(stiffness = 500f, dampingRatio = 0.85f)
                )
            }
        }
    }

    Box(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(bottom = 6.dp)
            .fillMaxWidth()
            .clip(pillShape)
            .liquidGlassNav(shape = pillShape, backdrop = backdrop)
            .then(
                if (backdrop == null || !isGlassSupported()) {
                    if (hazeState != null) {
                        Modifier
                            .hazeEffect(state = hazeState, style = HazeMaterials.regular(container))
                            .glassEdge(pillShape)
                    } else {
                        Modifier
                            .background(
                                color = if (isDark || isAmoled) Color(0xDD1E293B) else Color(0xEEFFFFFF),
                                shape = pillShape
                            )
                            .glassEdge(pillShape)
                    }
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 6.dp, vertical = 6.dp)
    ) {
        // Sliding glass pill indicator
        if (tabWidthPx > 0f) {
            Box(
                modifier = Modifier
                    .width(with(density) { tabWidthPx.toDp() })
                    .height(with(density) { rowSize.height.toDp() })
                    .graphicsLayer {
                        translationX = pillOffsetAnimatable.value
                    }
                    .clip(pillShape)
                    .background(glassIndicatorColor().copy(alpha = 0.55f), pillShape)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { rowSize = it },
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = index == selectedIndex
                val unselectedColor = if (isDark || isAmoled) Color(0xFF94A3B8) else Color(0xFF64748B)

                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()

                val pressedScale by animateFloatAsState(
                    targetValue = if (isPressed) 0.92f else 1.0f,
                    animationSpec = spring(stiffness = 700f, dampingRatio = 1.0f),
                    label = "chatNavPressed"
                )

                val iconScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.15f else 1.0f,
                    animationSpec = spring(stiffness = 500f, dampingRatio = 0.8f),
                    label = "chatNavIconScale"
                )

                val iconColor by animateColorAsState(
                    targetValue = if (isSelected) activeAccent else unselectedColor,
                    animationSpec = tween(220),
                    label = "chatNavIconColor"
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag(item.testTag)
                        .graphicsLayer {
                            scaleX = pressedScale
                            scaleY = pressedScale
                        }
                        .clip(pillShape)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onTabSelected(item.tab)
                            }
                        )
                ) {
                    // Soft glow behind selected tab icon
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            activeAccent.copy(alpha = 0.35f),
                                            Color.Transparent
                                        )
                                    ),
                                    shape = CircleShape
                                )
                        )
                    }

                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.tab.title,
                        tint = iconColor,
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer {
                                scaleX = iconScale
                                scaleY = iconScale
                            }
                    )
                }
            }
        }
    }
}
