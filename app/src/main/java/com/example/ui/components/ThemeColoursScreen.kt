package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppThemeMode
import com.example.ui.components.glassCardBackground
import com.example.ui.theme.getAppDimBackgroundBrush
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

// 16 Distinct theme color options in HEX
val ThemeColorPalette16 = listOf(
    "#D32F2F", // 1. Crimson Red (Default Brand)
    "#16A34A", // 2. Forest Green
    "#2563EB", // 3. Royal Blue
    "#7C3AED", // 4. Deep Violet
    "#EA580C", // 5. Vibrant Orange
    "#059669", // 6. Emerald Teal
    "#0284C7", // 7. Sky Blue
    "#D97706", // 8. Amber Gold
    "#E11D48", // 9. Rose Red
    "#4F46E5", // 10. Indigo
    "#9333EA", // 11. Purple Violet
    "#0891B2", // 12. Cyan Teal
    "#65A30D", // 13. Lime Green
    "#B45309", // 14. Terracotta
    "#475569", // 15. Slate Dark Gray
    "#DB2777"  // 16. Fuchsia Pink
)

/**
 * Calculates a contrasting icon color (Black or White) for a given background color
 */
fun getContrastingIconColor(colorHex: String): Color {
    val colorInt = try {
        android.graphics.Color.parseColor(colorHex)
    } catch (e: Exception) {
        android.graphics.Color.RED
    }
    val r = android.graphics.Color.red(colorInt) / 255.0
    val g = android.graphics.Color.green(colorInt) / 255.0
    val b = android.graphics.Color.blue(colorInt) / 255.0
    val luminance = 0.299 * r + 0.587 * g + 0.114 * b
    return if (luminance > 0.6) Color.Black else Color.White
}

@Composable
fun ThemeColoursDialog(
    themeMode: AppThemeMode,
    selectedColorHex: String,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    onSelectColorHex: (String) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onDismissRequest)

    val isDark = when (themeMode) {
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK, AppThemeMode.AMOLED -> true
        AppThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
    }
    val isAmoled = themeMode == AppThemeMode.AMOLED
    val themeHazeState = remember { HazeState() }

    val currentAccentColor = remember(selectedColorHex) {
        try {
            Color(android.graphics.Color.parseColor(selectedColorHex))
        } catch (e: Exception) {
            Color(0xFFD32F2F)
        }
    }

    val themeBgBrush = remember(isDark, isAmoled, currentAccentColor) {
        getAppDimBackgroundBrush(currentAccentColor, isDark = isDark, isAmoled = isAmoled)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(themeBgBrush)
            .testTag("theme_colours_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Wide Pill-Shaped Glass Header (Matching Dashboard Header Style)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .frostedGlassChrome(
                        hazeState = themeHazeState,
                        isDark = isDark,
                        accentColor = currentAccentColor,
                        shape = RoundedCornerShape(percent = 50)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(
                            onClick = onDismissRequest,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("theme_colours_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = currentAccentColor
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(currentAccentColor.copy(alpha = if (isDark) 0.25f else 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "Theme Icon",
                                tint = currentAccentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Theme & Colours",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    letterSpacing = (-0.3).sp
                                ),
                                color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Personalize app appearance",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = if (isDark) Color(0xFF94A3B8) else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            ThemeColoursContent(
                themeMode = themeMode,
                selectedColorHex = selectedColorHex,
                onSelectThemeMode = onSelectThemeMode,
                onSelectColorHex = onSelectColorHex,
                onClose = onDismissRequest,
                isDark = isDark,
                themeModeState = themeMode,
                currentAccentColor = currentAccentColor,
                hazeState = themeHazeState,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            )
        }
    }
}

@Composable
fun ThemeColoursContent(
    themeMode: AppThemeMode,
    selectedColorHex: String,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    onSelectColorHex: (String) -> Unit,
    onClose: () -> Unit,
    isDark: Boolean = false,
    themeModeState: AppThemeMode = AppThemeMode.SYSTEM,
    currentAccentColor: Color = MaterialTheme.colorScheme.primary,
    hazeState: HazeState? = null,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (hazeState != null) Modifier.hazeSource(state = hazeState) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(scrollState)
    ) {
        // SECTION 1: Theme Mode Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            border = null,
            modifier = Modifier
                .fillMaxWidth()
                .glassCardBackground(
                    cornerRadius = 20.dp,
                    accentColor = currentAccentColor,
                    isDark = isDark,
                    themeMode = themeModeState
                )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Theme Mode",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Select your preferred light/dark interface style",
                    fontSize = 12.sp,
                    color = if (isDark) Color(0xFF94A3B8) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 14.dp)
                )

                // Four large rectangular options (2x2 grid)
                val themeOptions = listOf(
                    ThemeModeOption(
                        mode = AppThemeMode.SYSTEM,
                        title = "Follow system",
                        icon = Icons.Default.PhoneAndroid
                    ),
                    ThemeModeOption(
                        mode = AppThemeMode.LIGHT,
                        title = "Light",
                        icon = Icons.Default.LightMode
                    ),
                    ThemeModeOption(
                        mode = AppThemeMode.DARK,
                        title = "Dark",
                        icon = Icons.Default.NightsStay
                    ),
                    ThemeModeOption(
                        mode = AppThemeMode.AMOLED,
                        title = "AMOLED",
                        icon = Icons.Default.Contrast
                    )
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LargeThemeCard(
                            option = themeOptions[0],
                            isSelected = themeMode == themeOptions[0].mode,
                            onClick = { onSelectThemeMode(themeOptions[0].mode) },
                            currentAccentColor = currentAccentColor,
                            isDark = isDark,
                            themeMode = themeModeState,
                            modifier = Modifier.weight(1f)
                        )
                        LargeThemeCard(
                            option = themeOptions[1],
                            isSelected = themeMode == themeOptions[1].mode,
                            onClick = { onSelectThemeMode(themeOptions[1].mode) },
                            currentAccentColor = currentAccentColor,
                            isDark = isDark,
                            themeMode = themeModeState,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LargeThemeCard(
                            option = themeOptions[2],
                            isSelected = themeMode == themeOptions[2].mode,
                            onClick = { onSelectThemeMode(themeOptions[2].mode) },
                            currentAccentColor = currentAccentColor,
                            isDark = isDark,
                            themeMode = themeModeState,
                            modifier = Modifier.weight(1f)
                        )
                        LargeThemeCard(
                            option = themeOptions[3],
                            isSelected = themeMode == themeOptions[3].mode,
                            onClick = { onSelectThemeMode(themeOptions[3].mode) },
                            currentAccentColor = currentAccentColor,
                            isDark = isDark,
                            themeMode = themeModeState,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION 2: Colour Palette Card (16 circular color options in 4x4 grid)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            border = null,
            modifier = Modifier
                .fillMaxWidth()
                .glassCardBackground(
                    cornerRadius = 20.dp,
                    accentColor = currentAccentColor,
                    isDark = isDark,
                    themeMode = themeModeState
                )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Colour Palette",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "16 distinct primary accent colors",
                    fontSize = 12.sp,
                    color = if (isDark) Color(0xFF94A3B8) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // 4 by 4 Grid of 16 circular color options with NO text
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ThemeColorPalette16.chunked(4).forEach { rowColors ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            rowColors.forEach { hex ->
                                val isSelected = hex.equals(selectedColorHex, ignoreCase = true)
                                CircularColorOption(
                                    colorHex = hex,
                                    isSelected = isSelected,
                                    currentAccentColor = currentAccentColor,
                                    onClick = { onSelectColorHex(hex) }
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Bottom Action Button
        Card(
            onClick = onClose,
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = currentAccentColor),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("apply_and_done_theme_button")
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Apply & Done",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

private data class ThemeModeOption(
    val mode: AppThemeMode,
    val title: String,
    val icon: ImageVector
)

@Composable
private fun LargeThemeCard(
    option: ThemeModeOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    currentAccentColor: Color,
    isDark: Boolean,
    themeMode: AppThemeMode,
    modifier: Modifier = Modifier
) {
    val activeBorderColor = currentAccentColor
    val activeContainerColor = currentAccentColor.copy(alpha = if (isDark) 0.22f else 0.12f)
    val inactiveContainerColor = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f)
    val inactiveBorderColor = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)

    Card(
        onClick = onClick,
        modifier = modifier
            .height(74.dp)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) activeBorderColor else inactiveBorderColor,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) activeContainerColor else inactiveContainerColor
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.align(Alignment.CenterStart),
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = option.icon,
                    contentDescription = option.title,
                    tint = if (isSelected) {
                        currentAccentColor
                    } else {
                        if (isDark) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = option.title,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) {
                        if (isDark) Color.White else currentAccentColor
                    } else if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = currentAccentColor,
                    modifier = Modifier
                        .size(18.dp)
                        .align(Alignment.TopEnd)
                )
            }
        }
    }
}

/**
 * 16 circular color option widget with no text.
 * When selected: displays a checkmark on it and highlights it with a circle (outer ring stroke).
 */
@Composable
private fun CircularColorOption(
    colorHex: String,
    isSelected: Boolean,
    currentAccentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (e: Exception) {
        Color.Red
    }

    val contrastingCheckColor = getContrastingIconColor(colorHex)
    val highlightRingColor = currentAccentColor

    Box(
        modifier = modifier
            .size(54.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            // Highlighted outer circle ring
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .border(
                        width = 3.dp,
                        color = highlightRingColor,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Inner color circle
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(color),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = contrastingCheckColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        } else {
            // Unselected circular color option
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = 1.dp,
                        color = Color.Black.copy(alpha = 0.15f),
                        shape = CircleShape
                    )
            )
        }
    }
}

