package com.example.ui.components

import android.content.Context
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/**
 * Liquid Glass Segmented Control (New Entry / Records and general tab switcher).
 * Features:
 * - Real-time backdrop blur via Haze with explicit backgroundColor to prevent RenderEffect crashes.
 * - Dynamic theme coverage: Light, Dark, AMOLED (pitch black with amplified specular/tint), and Follow System.
 * - Dynamic section accent color tinting (e.g. Red for Pruning, Purple for Imported, Emerald for Rootstocks).
 * - Specular top rim highlight (1.2dp vertical gradient border).
 * - Liquid stadium capsule sliding indicator with spring physics and squash & stretch.
 */
@Composable
fun AgriSegmentedControl(
    selectedMode: Int, // 0 = New Entry, 1 = Records, 2 = Analytics etc.
    onModeSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    newEntryLabel: String = "New Entry",
    recordsLabel: String = "Records",
    accentColor: Color = MaterialTheme.colorScheme.primary,
    hazeState: HazeState? = null
) {
    val items = listOf(
        SegmentedTabEntry(title = newEntryLabel, testTag = "tab_new_entry"),
        SegmentedTabEntry(title = recordsLabel, testTag = "tab_records")
    )

    LiquidGlassSegmentedSwitcher(
        items = items,
        selectedIndex = selectedMode.coerceIn(0, items.size - 1),
        onItemSelected = onModeSelected,
        accentColor = accentColor,
        hazeState = hazeState,
        modifier = modifier
    )
}

data class SegmentedTabEntry(
    val title: String,
    val testTag: String
)

@Composable
fun LiquidGlassSegmentedSwitcher(
    items: List<SegmentedTabEntry>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    hazeState: HazeState? = null
) {
    val context = LocalContext.current
    val isDark = isAppInDarkMode()
    val screenBgColor = MaterialTheme.colorScheme.background
    val isAmoled = isDark && (screenBgColor.luminance() < 0.01f || screenBgColor == Color.Black)
    val haptic = LocalHapticFeedback.current

    // Performance & accessibility fallback check
    val isReduceTransparencyOrBatterySaver = remember(context) {
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val isPowerSave = powerManager?.isPowerSaveMode == true
            val reduceTransparency = Settings.Secure.getInt(context.contentResolver, "reduce_transparency", 0) == 1
            isPowerSave || reduceTransparency
        } catch (e: Exception) {
            false
        }
    }

    val containerShape = RoundedCornerShape(percent = 50)
    val capsuleShape = RoundedCornerShape(percent = 50)

    // Explicit HazeStyle with backgroundColor set on EVERY branch
    val hazeStyle = remember(isDark, isAmoled, accentColor, screenBgColor) {
        when {
            isAmoled -> HazeStyle(
                backgroundColor = screenBgColor,
                tint = HazeTint(accentColor.copy(alpha = 0.22f)),
                blurRadius = 26.dp
            )
            isDark -> HazeStyle(
                backgroundColor = screenBgColor,
                tint = HazeTint(Color(0xFF0F172A).copy(alpha = 0.40f)),
                blurRadius = 26.dp
            )
            else -> HazeStyle(
                backgroundColor = screenBgColor,
                tint = HazeTint(Color.White.copy(alpha = 0.18f)),
                blurRadius = 26.dp
            )
        }
    }

    // Specular Edge: 1.2dp vertical gradient stroke (brighter on top edge, fading to bottom)
    val specularBorderBrush = remember(isDark, isAmoled, accentColor) {
        when {
            isAmoled -> Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.70f),
                    accentColor.copy(alpha = 0.45f),
                    Color.White.copy(alpha = 0.15f)
                )
            )
            isDark -> Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.45f),
                    accentColor.copy(alpha = 0.20f),
                    Color.White.copy(alpha = 0.10f)
                )
            )
            else -> Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.88f),
                    accentColor.copy(alpha = 0.25f),
                    Color.White.copy(alpha = 0.20f)
                )
            )
        }
    }

    // Container Ambient & Spot shadow
    val containerShadowAmbient = remember(isDark, isAmoled, accentColor) {
        when {
            isAmoled -> accentColor.copy(alpha = 0.20f)
            isDark -> Color.Black.copy(alpha = 0.45f)
            else -> Color(0xFF0F172A).copy(alpha = 0.10f)
        }
    }
    val containerShadowSpot = remember(isDark, isAmoled, accentColor) {
        when {
            isAmoled -> accentColor.copy(alpha = 0.28f)
            isDark -> Color.Black.copy(alpha = 0.60f)
            else -> Color(0xFF0F172A).copy(alpha = 0.14f)
        }
    }

    // Base glass overlay tint brush
    val baseGlassOverlayBrush = remember(isDark, isAmoled, accentColor) {
        when {
            isAmoled -> Brush.verticalGradient(
                colors = listOf(
                    accentColor.copy(alpha = 0.24f),
                    Color(0xFF0A0A0A).copy(alpha = 0.88f),
                    accentColor.copy(alpha = 0.16f)
                )
            )
            isDark -> Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF1E293B).copy(alpha = 0.45f),
                    accentColor.copy(alpha = 0.12f),
                    Color(0xFF0F172A).copy(alpha = 0.52f)
                )
            )
            else -> Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.60f),
                    accentColor.copy(alpha = 0.08f),
                    Color(0xFFE2E8F0).copy(alpha = 0.50f)
                )
            )
        }
    }

    // Solid fallback for power save / reduce transparency
    val fallbackBgColor = if (isAmoled) Color(0xFF0F0F0F) else if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)

    // Motion physics tracking for squash & stretch
    var previousIndex by remember { mutableIntStateOf(selectedIndex) }
    val isMoving = previousIndex != selectedIndex
    LaunchedEffect(selectedIndex) {
        previousIndex = selectedIndex
    }

    val blobStretchScaleX by animateFloatAsState(
        targetValue = if (isMoving) 1.08f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "BlobScaleX"
    )

    val blobSquashScaleY by animateFloatAsState(
        targetValue = if (isMoving) 0.90f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "BlobScaleY"
    )

    // Active pill indicator gradient
    val activePillGradient = remember(isDark, isAmoled, accentColor) {
        when {
            isAmoled -> Brush.verticalGradient(
                colors = listOf(
                    accentColor.copy(alpha = 0.96f),
                    accentColor.copy(alpha = 0.82f)
                )
            )
            isDark -> Brush.verticalGradient(
                colors = listOf(
                    accentColor.copy(alpha = 0.95f),
                    accentColor.copy(alpha = 0.80f)
                )
            )
            else -> Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.98f),
                    Color(0xFFF8FAFC).copy(alpha = 0.92f)
                )
            )
        }
    }

    val activePillBorderBrush = remember(isDark, isAmoled, accentColor) {
        when {
            isAmoled -> Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.80f),
                    accentColor.copy(alpha = 0.50f)
                )
            )
            isDark -> Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.40f),
                    Color.White.copy(alpha = 0.15f)
                )
            )
            else -> Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.95f),
                    Color.White.copy(alpha = 0.40f)
                )
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .height(50.dp)
            .shadow(
                elevation = if (isAmoled) 14.dp else 10.dp,
                shape = containerShape,
                clip = false,
                ambientColor = containerShadowAmbient,
                spotColor = containerShadowSpot
            )
            .clip(containerShape)
            .then(
                if (hazeState != null && !isReduceTransparencyOrBatterySaver) {
                    Modifier.hazeEffect(state = hazeState, style = hazeStyle)
                } else if (!isReduceTransparencyOrBatterySaver) {
                    Modifier.blur(26.dp)
                } else {
                    Modifier
                }
            )
            .background(
                if (isReduceTransparencyOrBatterySaver) {
                    Brush.verticalGradient(listOf(fallbackBgColor, fallbackBgColor))
                } else {
                    baseGlassOverlayBrush
                }
            )
            .border(
                width = 1.2.dp,
                brush = specularBorderBrush,
                shape = containerShape
            )
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
        ) {
            val totalWidth = maxWidth
            val tabCount = items.size.coerceAtLeast(1)
            val tabWidth = totalWidth / tabCount
            val pillHeight = 42.dp
            val pillTargetWidth = tabWidth

            val animatedOffset by animateDpAsState(
                targetValue = tabWidth * selectedIndex,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "PillOffset"
            )

            // 1. Sliding Spring Liquid Stadium Capsule Indicator
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = animatedOffset)
                    .width(pillTargetWidth)
                    .height(pillHeight)
                    .graphicsLayer {
                        scaleX = blobStretchScaleX
                        scaleY = blobSquashScaleY
                    }
                    .shadow(
                        elevation = if (isAmoled) 8.dp else 5.dp,
                        shape = capsuleShape,
                        clip = false,
                        ambientColor = accentColor.copy(alpha = if (isAmoled) 0.35f else 0.20f),
                        spotColor = accentColor.copy(alpha = if (isAmoled) 0.50f else 0.30f)
                    )
                    .clip(capsuleShape)
                    .background(activePillGradient)
                    .border(
                        width = 1.dp,
                        brush = activePillBorderBrush,
                        shape = capsuleShape
                    )
            ) {
                // Internal soft reflection sheen
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(capsuleShape)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (isDark) 0.25f else 0.50f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Top specular highlight curve
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth(0.78f)
                        .height(11.dp)
                        .padding(top = 2.dp)
                        .clip(capsuleShape)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (isAmoled) 0.70f else if (isDark) 0.50f else 0.80f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            // 2. Interactive Tab Labels
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, item ->
                    val isSelected = index == selectedIndex
                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            if (isDark || isAmoled) Color.White else accentColor
                        } else {
                            if (isDark || isAmoled) Color(0xFF94A3B8) else Color(0xFF475569)
                        },
                        label = "tab_text_$index"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(capsuleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, color = accentColor.copy(alpha = 0.20f)),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onItemSelected(index)
                                }
                            )
                            .testTag(item.testTag),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.title,
                            fontSize = 14.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = textColor
                        )
                    }
                }
            }
        }
    }
}
