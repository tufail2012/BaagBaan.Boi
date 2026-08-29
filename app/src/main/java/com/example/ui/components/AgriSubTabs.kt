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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.getSectionAccentColor
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/**
 * Pruning Sub-Tabs (Summer Pruning / Winter Pruning)
 * Styled with Liquid Glass material: backdrop blur, specular top edge highlight,
 * spring-animated selection indicator with squash & stretch, and section accent tinting (Red).
 */
@Composable
fun PruningSubTabs(
    selectedSubTab: String,
    onSelectSubTab: (String) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = getSectionAccentColor("Pruning"),
    hazeState: HazeState? = null
) {
    val subTabs = listOf("Summer Pruning", "Winter Pruning")
    val selectedIndex = if (selectedSubTab.contains("Winter", ignoreCase = true)) 1 else 0

    LiquidGlassTabSwitcher(
        tabCount = subTabs.size,
        selectedIndex = selectedIndex,
        accentColor = accentColor,
        hazeState = hazeState,
        modifier = modifier
    ) {
        subTabs.forEachIndexed { index, tabName ->
            val isSelected = index == selectedIndex
            val isSummer = tabName.contains("Summer", ignoreCase = true)
            val icon = if (isSummer) Icons.Default.WbSunny else Icons.Default.AcUnit
            val iconTint = if (isSummer) {
                if (isSelected) Color(0xFFFFB300) else Color(0xFFF57C00)
            } else {
                if (isSelected) Color(0xFF38BDF8) else Color(0xFF0288D1)
            }

            LiquidGlassSubTabButton(
                title = tabName,
                isSelected = isSelected,
                icon = icon,
                iconTint = iconTint,
                accentColor = accentColor,
                testTag = "subtab_${tabName.lowercase().replace(" ", "_")}",
                onClick = { onSelectSubTab(tabName) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Rootstock Sub-Tabs (M9-T337, MM111, Geneva dropdown)
 * Styled with Liquid Glass material: backdrop blur, specular top edge highlight,
 * spring-animated selection indicator with squash & stretch, and section accent tinting (Emerald).
 */
@Composable
fun RootstockSubTabs(
    selectedSubTab: String,
    selectedGenevaOption: String?,
    onSelectSubTab: (String, String?) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = getSectionAccentColor("Rootstocks"),
    hazeState: HazeState? = null
) {
    var genevaMenuExpanded by remember { mutableStateOf(false) }
    val genevaOptions = listOf("G41", "G214", "G11", "G35", "G969", "G890")
    val haptic = LocalHapticFeedback.current

    val isGenevaSelected = selectedSubTab.startsWith("Geneva") || genevaOptions.contains(selectedSubTab)
    val selectedIndex = when {
        selectedSubTab.equals("MM111", ignoreCase = true) -> 1
        isGenevaSelected -> 2
        else -> 0 // M9-T337
    }

    val activeGenevaLabel = if (selectedGenevaOption != null) {
        "Geneva ($selectedGenevaOption)"
    } else {
        "Geneva"
    }

    LiquidGlassTabSwitcher(
        tabCount = 3,
        selectedIndex = selectedIndex,
        accentColor = accentColor,
        hazeState = hazeState,
        modifier = modifier
    ) {
        // 1. M9-T337
        LiquidGlassSubTabButton(
            title = "M9-T337",
            isSelected = selectedIndex == 0,
            accentColor = accentColor,
            testTag = "subtab_m9_t337",
            onClick = { onSelectSubTab("M9-T337", null) },
            modifier = Modifier.weight(1f)
        )

        // 2. MM111
        LiquidGlassSubTabButton(
            title = "MM111",
            isSelected = selectedIndex == 1,
            accentColor = accentColor,
            testTag = "subtab_mm111",
            onClick = { onSelectSubTab("MM111", null) },
            modifier = Modifier.weight(1f)
        )

        // 3. Geneva Dropdown
        Box(
            modifier = Modifier.weight(1.2f)
        ) {
            LiquidGlassSubTabButton(
                title = activeGenevaLabel,
                isSelected = selectedIndex == 2,
                hasDropdown = true,
                accentColor = accentColor,
                testTag = "subtab_geneva",
                onClick = { genevaMenuExpanded = true },
                modifier = Modifier.fillMaxWidth()
            )

            DropdownMenu(
                expanded = genevaMenuExpanded,
                onDismissRequest = { genevaMenuExpanded = false }
            ) {
                Text(
                    text = "Geneva Rootstocks",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )

                genevaOptions.forEach { option ->
                    val isOptionSelected = selectedGenevaOption == option && isGenevaSelected
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = option,
                                    fontWeight = if (isOptionSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isOptionSelected) accentColor else MaterialTheme.colorScheme.onSurface
                                )
                                if (isOptionSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = accentColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            genevaMenuExpanded = false
                            onSelectSubTab("Geneva", option)
                        }
                    )
                }
            }
        }
    }
}

/**
 * Reusable Liquid Glass Container for Sub-Tabs with sliding stadium capsule indicator,
 * spring-based squash/stretch physics, real-time Haze backdrop blur, and AMOLED boost.
 */
@Composable
fun LiquidGlassTabSwitcher(
    tabCount: Int,
    selectedIndex: Int,
    accentColor: Color,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    content: @Composable RowScope.() -> Unit
) {
    val context = LocalContext.current
    val isDark = isAppInDarkMode()
    val screenBgColor = MaterialTheme.colorScheme.background
    val isAmoled = isDark && (screenBgColor.luminance() < 0.01f || screenBgColor == Color.Black)

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

    val fallbackBgColor = if (isAmoled) Color(0xFF0F0F0F) else if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)

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
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .height(46.dp)
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
                .padding(3.dp)
        ) {
            val totalWidth = maxWidth
            val count = tabCount.coerceAtLeast(1)
            val tabWidth = totalWidth / count
            val pillHeight = 40.dp
            val pillTargetWidth = tabWidth

            val animatedOffset by animateDpAsState(
                targetValue = tabWidth * selectedIndex,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "SubTabPillOffset"
            )

            // Sliding Spring Liquid Stadium Capsule Indicator
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
                        elevation = if (isAmoled) 6.dp else 4.dp,
                        shape = capsuleShape,
                        clip = false,
                        ambientColor = if (isDark) accentColor.copy(alpha = 0.35f) else Color(0xFF0F172A).copy(alpha = 0.08f),
                        spotColor = if (isDark) accentColor.copy(alpha = 0.50f) else accentColor.copy(alpha = 0.16f)
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
                        .height(10.dp)
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

            // Interactive Tab Row
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                content()
            }
        }
    }
}

@Composable
fun LiquidGlassSubTabButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    icon: ImageVector? = null,
    iconTint: Color? = null,
    hasDropdown: Boolean = false,
    testTag: String = ""
) {
    val haptic = LocalHapticFeedback.current
    val isDark = isAppInDarkMode()
    val screenBgColor = MaterialTheme.colorScheme.background
    val isAmoled = isDark && (screenBgColor.luminance() < 0.01f || screenBgColor == Color.Black)

    val textColor by animateColorAsState(
        targetValue = if (isSelected) {
            if (isDark || isAmoled) Color.White else accentColor
        } else {
            if (isDark || isAmoled) Color(0xFF94A3B8) else Color(0xFF475569)
        },
        label = "subtab_text"
    )

    val capsuleShape = RoundedCornerShape(percent = 50)

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(capsuleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = accentColor.copy(alpha = 0.20f)),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            )
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint ?: textColor,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = textColor,
                maxLines = 1
            )
            if (hasDropdown) {
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Expand Geneva Menu",
                    tint = textColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
