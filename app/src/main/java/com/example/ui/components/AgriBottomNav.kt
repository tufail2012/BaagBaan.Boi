package com.example.ui.components

import android.content.Context
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.getSectionAccentColor
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlinx.coroutines.launch

data class AgriNavItem(
    val title: String,
    val serviceCategory: String,
    val icon: ImageVector,
    val testTag: String
)

@Composable
fun AgriBottomNav(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color? = null,
    hazeState: HazeState? = null
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

    val context = LocalContext.current
    val isDark = isAppInDarkMode()
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // Check accessibility Reduce Transparency or Battery Saver mode for performance fallback
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

    val selectedIndex = remember(selectedCategory) {
        val idx = navItems.indexOfFirst { item ->
            selectedCategory.equals(item.serviceCategory, ignoreCase = true) ||
                    (selectedCategory.equals("Local", ignoreCase = true) && item.serviceCategory.equals("Local Plants", ignoreCase = true)) ||
                    (selectedCategory.equals("Garden", ignoreCase = true) && item.serviceCategory.equals("Garden Planning", ignoreCase = true))
        }
        if (idx >= 0) idx else 0
    }

    // Dynamic Section Accent Color matching the active section
    val activeSectionAccent = remember(selectedCategory, accentColor) {
        accentColor ?: getSectionAccentColor(selectedCategory)
    }

    val animatedAccentColor by animateColorAsState(
        targetValue = activeSectionAccent,
        animationSpec = tween(durationMillis = 280),
        label = "BottomNavAccentColor"
    )

    val screenBgColor = MaterialTheme.colorScheme.background

    // Liquid Glass Tint & Material styling with explicit real background color
    val hazeStyle = remember(isDark, screenBgColor) {
        if (!isDark) {
            HazeStyle(
                backgroundColor = screenBgColor,
                tint = HazeTint(Color.White.copy(alpha = 0.18f)),
                blurRadius = 26.dp
            )
        } else {
            HazeStyle(
                backgroundColor = screenBgColor,
                tint = HazeTint(Color(0xFF0F172A).copy(alpha = 0.40f)),
                blurRadius = 26.dp
            )
        }
    }

    // Specular Edge: Thin 1.2dp gradient stroke (brighter on top edge, fading to bottom)
    val specularBorderBrush = if (!isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.88f),
                Color.White.copy(alpha = 0.22f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.48f),
                Color.White.copy(alpha = 0.10f)
            )
        )
    }

    // Glass bar floating shadow
    val barShadowAmbient = if (!isDark) Color(0xFF1E1B4B).copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.45f)
    val barShadowSpot = if (!isDark) Color(0xFF1E1B4B).copy(alpha = 0.16f) else Color.Black.copy(alpha = 0.60f)

    // Base glass tint layer on top of blur (18-20% white + faint hint of section accent)
    val baseGlassOverlayBrush = if (!isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.20f),
                animatedAccentColor.copy(alpha = 0.06f),
                Color.White.copy(alpha = 0.15f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1E293B).copy(alpha = 0.40f),
                animatedAccentColor.copy(alpha = 0.10f),
                Color(0xFF0F172A).copy(alpha = 0.50f)
            )
        )
    }

    // Fallback solid fill when transparency is reduced or battery saver is active
    val fallbackBgColor = if (!isDark) {
        Color(0xFFF6F3FB)
    } else {
        Color(0xFF1B1828)
    }

    // Track movement for spring-based squash & stretch
    var previousIndex by remember { mutableIntStateOf(selectedIndex) }
    val isMoving = previousIndex != selectedIndex

    LaunchedEffect(selectedIndex) {
        previousIndex = selectedIndex
    }

    val blobStretchScaleX by animateFloatAsState(
        targetValue = if (isMoving) 1.05f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "BlobScaleX"
    )

    val blobSquashScaleY by animateFloatAsState(
        targetValue = if (isMoving) 0.95f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "BlobScaleY"
    )

    // Capsule / stadium shape with 50% radius on container (fully rounded left & right ends)
    val capsuleShape = RoundedCornerShape(percent = 50)

    // Floating Placement: lifted off the bottom edge
    Box(
        modifier = modifier
            .navigationBarsPadding()
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 16.dp)
            .shadow(
                elevation = 18.dp,
                shape = CircleShape,
                clip = false,
                ambientColor = barShadowAmbient,
                spotColor = barShadowSpot
            )
            .clip(CircleShape)
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
                width = 1.2.dp,
                brush = specularBorderBrush,
                shape = CircleShape
            )
    ) {
        // Subtle top specular highlight reflection along the inner rim of the glass pill
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(0.92f)
                .height(1.dp)
                .padding(top = 1.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = if (!isDark) 0.85f else 0.40f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Tab Content and Animated Liquid Blob Indicator
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .padding(horizontal = 6.dp)
        ) {
            val totalWidth = maxWidth
            val tabCount = navItems.size
            val tabWidth = totalWidth / tabCount

            // Active pill dimensions: Spans the tab slot width with subtle, clean margins
            val horizontalMargin = 4.dp
            val blobBaseWidth = (tabWidth - (horizontalMargin * 2)).coerceAtLeast(36.dp)
            val blobTargetWidth = if (isMoving) (blobBaseWidth + 3.dp) else blobBaseWidth
            val blobHeight = 54.dp

            val animatedBlobWidth by animateDpAsState(
                targetValue = blobTargetWidth,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "BlobWidth"
            )

            // Calculate target horizontal offset for sliding liquid blob capsule
            val targetOffset = (tabWidth * selectedIndex) + ((tabWidth - animatedBlobWidth) / 2)
            val animatedBlobOffset by animateDpAsState(
                targetValue = targetOffset,
                animationSpec = spring(
                    dampingRatio = 0.72f,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "SlidingBlobOffset"
            )

            // Liquid Glass Blob Material & Refraction tinted with active section accent
            val blobGradient = if (!isDark) {
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.85f),
                        animatedAccentColor.copy(alpha = 0.22f),
                        Color.White.copy(alpha = 0.65f)
                    )
                )
            } else {
                Brush.verticalGradient(
                    colors = listOf(
                        animatedAccentColor.copy(alpha = 0.38f),
                        animatedAccentColor.copy(alpha = 0.25f),
                        Color(0xFF0F172A).copy(alpha = 0.70f)
                    )
                )
            }

            val blobBorderColor = if (!isDark) {
                Color.White.copy(alpha = 0.85f)
            } else {
                Color.White.copy(alpha = 0.35f)
            }

            // 1. SIGNATURE SLIDING & SQUASHING/STRETCHING LIQUID CAPSULE / PILL
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = animatedBlobOffset)
                    .width(animatedBlobWidth)
                    .height(blobHeight)
                    .graphicsLayer {
                        scaleX = blobStretchScaleX
                        scaleY = blobSquashScaleY
                    }
                    .shadow(
                        elevation = 6.dp,
                        shape = capsuleShape,
                        clip = false,
                        ambientColor = animatedAccentColor.copy(alpha = if (!isDark) 0.22f else 0.35f),
                        spotColor = animatedAccentColor.copy(alpha = if (!isDark) 0.32f else 0.50f)
                    )
                    .clip(capsuleShape)
                    .background(blobGradient)
                    .border(
                        width = 1.dp,
                        color = blobBorderColor,
                        shape = capsuleShape
                    )
            ) {
                // Internal soft reflection layer
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(capsuleShape)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = if (!isDark) 0.35f else 0.15f)
                                )
                            )
                        )
                )

                // Pristine top curvature specular highlight on the liquid capsule
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth(0.76f)
                        .height(10.dp)
                        .padding(top = 2.dp)
                        .clip(capsuleShape)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (!isDark) 0.80f else 0.50f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            // 2. TAB ICONS ROW
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                navItems.forEachIndexed { index, item ->
                    val isSelected = index == selectedIndex
                    val scale = remember { Animatable(1f) }
                    val itemAccentColor = getSectionAccentColor(item.serviceCategory)

                    // Inactive tabs: muted grey-violet; Selected tab: dynamic section accent
                    val inactiveColor = if (!isDark) {
                        Color(0xFF8E84A3) // Muted grey-violet for light theme
                    } else {
                        Color(0xFF9E95B3) // Muted grey-violet for dark theme
                    }

                    val animatedIconColor by animateColorAsState(
                        targetValue = if (isSelected) animatedAccentColor else inactiveColor,
                        animationSpec = tween(durationMillis = 260),
                        label = "NavIconColor_$index"
                    )

                    val animatedIconScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.15f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "NavIconScale_$index"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(68.dp)
                            .testTag(item.testTag)
                            .clip(RoundedCornerShape(32.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(
                                    bounded = true,
                                    color = itemAccentColor.copy(alpha = 0.22f)
                                ),
                                onClick = {
                                    scope.launch {
                                        scale.animateTo(
                                            0.84f,
                                            animationSpec = tween(70, easing = FastOutSlowInEasing)
                                        )
                                        scale.animateTo(
                                            1f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessLow
                                            )
                                        )
                                    }
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onCategorySelected(item.serviceCategory)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            tint = animatedIconColor,
                            modifier = Modifier
                                .size(24.dp)
                                .scale(scale.value)
                                .graphicsLayer {
                                    scaleX = animatedIconScale
                                    scaleY = animatedIconScale
                                }
                        )
                    }
                }
            }
        }
    }
}
