package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.PlaylistAddCheck
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    modifier: Modifier = Modifier
) {
    val navItems = listOf(
        AgriNavItem("Local", "Local Plants", Icons.Outlined.LocalFlorist, "nav_local"),
        AgriNavItem("Imported", "Imported", Icons.Default.LocalShipping, "nav_imported"),
        AgriNavItem("Rootstocks", "Rootstocks", Icons.Default.Spa, "nav_rootstocks"),
        AgriNavItem("Site Visit", "Site Visit", Icons.Outlined.Assignment, "nav_site_visit"),
        AgriNavItem("Pruning", "Pruning", Icons.Default.ContentCut, "nav_pruning"),
        AgriNavItem("Garden", "Garden Planning", Icons.Default.Park, "nav_garden_planning")
    )

    val isDark = isAppInDarkMode()
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val containerGlassGradient = if (!isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.70f),
                Color(0xFFE2E8F0).copy(alpha = 0.50f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1E293B).copy(alpha = 0.65f),
                Color(0xFF0F172A).copy(alpha = 0.78f)
            )
        )
    }

    val containerGlassBorder = if (!isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.95f),
                Color.White.copy(alpha = 0.40f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.28f),
                Color.White.copy(alpha = 0.08f)
            )
        )
    }

    val containerShadowColor = if (!isDark) {
        Color(0xFF0F172A).copy(alpha = 0.16f)
    } else {
        Color.Black.copy(alpha = 0.60f)
    }

    // Glassmorphism floating pill container
    Box(
        modifier = modifier
            .navigationBarsPadding()
            .fillMaxWidth()
            .padding(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 18.dp)
            .shadow(
                elevation = 16.dp,
                shape = CircleShape,
                clip = false,
                ambientColor = containerShadowColor,
                spotColor = containerShadowColor
            )
            .clip(CircleShape)
            .background(containerGlassGradient)
            .border(
                width = 1.2.dp,
                brush = containerGlassBorder,
                shape = CircleShape
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            navItems.forEach { item ->
                val scale = remember { Animatable(1f) }

                val isSelected = selectedCategory.equals(item.serviceCategory, ignoreCase = true) ||
                        (selectedCategory.equals("Local", ignoreCase = true) && item.serviceCategory.equals("Local Plants", ignoreCase = true)) ||
                        (selectedCategory.equals("Garden", ignoreCase = true) && item.serviceCategory.equals("Garden Planning", ignoreCase = true))

                val inactiveColor = if (!isDark) {
                    Color(0xFF455A64) // Crisp dark slate for light theme
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }

                // Translucent liquid droplet / glassmorphism pill gradient
                val glassGradient = if (!isDark) {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.82f),
                            Color(0xFFDCE5EF).copy(alpha = 0.65f)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF37474F).copy(alpha = 0.90f),
                            Color(0xFF263238).copy(alpha = 0.95f)
                        )
                    )
                }

                val glassBorderColor = if (!isDark) {
                    Color.White.copy(alpha = 0.85f)
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.50f)
                }

                // Smooth animations for state changes
                val animatedIconColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else inactiveColor,
                    animationSpec = tween(durationMillis = 280),
                    label = "IconColor"
                )

                val animatedPillWidth by animateDpAsState(
                    targetValue = if (isSelected) 48.dp else 42.dp,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "PillWidth"
                )

                val animatedPillHeight by animateDpAsState(
                    targetValue = if (isSelected) 48.dp else 42.dp,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "PillHeight"
                )

                val animatedIconScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.15f else 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "IconScale"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .testTag(item.testTag)
                        .clip(RoundedCornerShape(32.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)),
                            onClick = {
                                scope.launch {
                                    scale.animateTo(0.82f, animationSpec = tween(80, easing = FastOutSlowInEasing))
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
                    // Unified Liquid Glassmorphism Water Droplet Pill Container (Encompasses Icon)
                    Box(
                        modifier = Modifier
                            .width(animatedPillWidth)
                            .height(animatedPillHeight)
                            .then(
                                if (isSelected) {
                                    Modifier
                                        .shadow(
                                            elevation = 4.dp,
                                            shape = CircleShape,
                                            clip = false,
                                            ambientColor = Color.Black.copy(alpha = 0.15f),
                                            spotColor = Color.Black.copy(alpha = 0.20f)
                                        )
                                        .clip(CircleShape)
                                        .background(glassGradient)
                                        .border(
                                            width = 1.dp,
                                            color = glassBorderColor,
                                            shape = CircleShape
                                        )
                                } else {
                                    Modifier.clip(CircleShape)
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


