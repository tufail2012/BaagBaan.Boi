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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
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
 * - Real-time backdrop blur via Haze matching the main navigation bar glassmorphism.
 * - Dynamic theme coverage: Light, Dark, AMOLED (pitch black with amplified specular/tint), and Follow System.
 * - Dynamic section accent color tinting via centralized getSectionAccentColor().
 * - Specular top rim highlight and subtle translucent glass depth.
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

    val containerShape = CircleShape
    val capsuleShape = CircleShape

    // Explicit HazeStyle matching the main navigation bar glass language
    val hazeStyle = remember(isDark, isAmoled, accentColor, screenBgColor) {
        when {
            isAmoled -> HazeStyle(
                backgroundColor = screenBgColor,
                tint = HazeTint(accentColor.copy(alpha = 0.16f)),
                blurRadius = 24.dp
            )
            isDark -> HazeStyle(
                backgroundColor = screenBgColor,
                tint = HazeTint(Color(0xFF0F172A).copy(alpha = 0.35f)),
                blurRadius = 24.dp
            )
            else -> HazeStyle(
                backgroundColor = screenBgColor,
                tint = HazeTint(accentColor.copy(alpha = 0.08f)),
                blurRadius = 24.dp
            )
        }
    }

    val outlineColor = MaterialTheme.colorScheme.outline

    // Specular Edge: Thin 1.dp vertical gradient border matching the header glass pill
    val specularBorderBrush = remember(isDark, isAmoled, accentColor, outlineColor) {
        when {
            isAmoled -> Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.60f),
                    accentColor.copy(alpha = 0.35f),
                    Color.White.copy(alpha = 0.15f)
                )
            )
            isDark -> Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.35f),
                    outlineColor.copy(alpha = 0.25f),
                    Color.White.copy(alpha = 0.08f)
                )
            )
            else -> Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.95f),
                    accentColor.copy(alpha = 0.22f),
                    Color.White.copy(alpha = 0.35f)
                )
            )
        }
    }

    // Outer Container Ambient & Spot shadow for subtle glass elevation
    val containerShadowAmbient = remember(isDark, isAmoled, accentColor) {
        when {
            isAmoled -> accentColor.copy(alpha = 0.15f)
            isDark -> Color.Black.copy(alpha = 0.25f)
            else -> Color(0xFF0F172A).copy(alpha = 0.06f)
        }
    }
    val containerShadowSpot = remember(isDark, isAmoled, accentColor) {
        when {
            isAmoled -> accentColor.copy(alpha = 0.22f)
            isDark -> Color.Black.copy(alpha = 0.35f)
            else -> accentColor.copy(alpha = 0.10f)
        }
    }

    // Base glass overlay tint brush (translucent glass matching top nav bar)
    val baseGlassOverlayBrush = remember(isDark, isAmoled, accentColor) {
        when {
            isAmoled -> Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF141414).copy(alpha = 0.82f),
                    accentColor.copy(alpha = 0.14f),
                    Color(0xFF070707).copy(alpha = 0.88f)
                )
            )
            isDark -> Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF1E293B).copy(alpha = 0.55f),
                    accentColor.copy(alpha = 0.10f),
                    Color(0xFF0F172A).copy(alpha = 0.65f)
                )
            )
            else -> Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.72f),
                    accentColor.copy(alpha = 0.08f),
                    Color.White.copy(alpha = 0.58f)
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
        targetValue = if (isMoving) 1.06f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "BlobScaleX"
    )

    val blobSquashScaleY by animateFloatAsState(
        targetValue = if (isMoving) 0.92f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "BlobScaleY"
    )

    // Active pill indicator gradient: translucent accent-tinted glass
    val activePillGradient = remember(isDark, isAmoled, accentColor) {
        when {
            isAmoled -> Brush.verticalGradient(
                colors = listOf(
                    accentColor.copy(alpha = 0.78f),
                    accentColor.copy(alpha = 0.55f)
                )
            )
            isDark -> Brush.verticalGradient(
                colors = listOf(
                    accentColor.copy(alpha = 0.65f),
                    accentColor.copy(alpha = 0.42f)
                )
            )
            else -> Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.88f),
                    lerp(Color.White, accentColor, 0.15f).copy(alpha = 0.82f),
                    Color.White.copy(alpha = 0.78f)
                )
            )
        }
    }

    val activePillBorderBrush = remember(isDark, isAmoled, accentColor) {
        when {
            isAmoled -> Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.70f),
                    accentColor.copy(alpha = 0.60f),
                    Color.White.copy(alpha = 0.20f)
                )
            )
            isDark -> Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.50f),
                    accentColor.copy(alpha = 0.45f),
                    Color.White.copy(alpha = 0.15f)
                )
            )
            else -> Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.95f),
                    accentColor.copy(alpha = 0.35f),
                    Color.White.copy(alpha = 0.45f)
                )
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .height(48.dp)
            .shadow(
                elevation = if (isAmoled) 4.dp else 2.dp,
                shape = containerShape,
                clip = false,
                ambientColor = containerShadowAmbient,
                spotColor = containerShadowSpot
            )
            .clip(containerShape)
            .then(
                if (hazeState != null && !isReduceTransparencyOrBatterySaver) {
                    Modifier.hazeEffect(state = hazeState, style = hazeStyle)
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
                width = 1.dp,
                brush = specularBorderBrush,
                shape = containerShape
            )
    ) {
        // Subtle top specular highlight reflection along the inner rim of the glass pill
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(0.90f)
                .height(1.dp)
                .padding(top = 1.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = if (!isDark) 0.80f else 0.35f),
                            Color.Transparent
                        )
                    )
                )
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(3.5.dp)
        ) {
            val totalWidth = maxWidth
            val tabCount = items.size.coerceAtLeast(1)
            val tabWidth = totalWidth / tabCount
            val pillHeight = 41.dp
            val pillTargetWidth = tabWidth

            val animatedOffset by animateDpAsState(
                targetValue = tabWidth * selectedIndex,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "PillOffset"
            )

            // 1. Sliding Spring Liquid Stadium Capsule Indicator (Glass Pill with Soft Glow)
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
                        elevation = if (isAmoled) 5.dp else if (isDark) 4.dp else 3.dp,
                        shape = capsuleShape,
                        clip = false,
                        ambientColor = accentColor.copy(alpha = if (isAmoled) 0.40f else if (isDark) 0.30f else 0.18f),
                        spotColor = accentColor.copy(alpha = if (isAmoled) 0.60f else if (isDark) 0.45f else 0.25f)
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
                                    Color.White.copy(alpha = if (isDark) 0.25f else 0.45f),
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
                        .height(10.dp)
                        .padding(top = 1.5.dp)
                        .clip(capsuleShape)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (isAmoled) 0.70f else if (isDark) 0.45f else 0.75f),
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

