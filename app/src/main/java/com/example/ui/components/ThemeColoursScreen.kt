package com.example.ui.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.AppThemeMode

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
    onDismissRequest: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )
        ) {
            ThemeColoursContent(
                themeMode = themeMode,
                selectedColorHex = selectedColorHex,
                onSelectThemeMode = onSelectThemeMode,
                onSelectColorHex = onSelectColorHex,
                onClose = onDismissRequest
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
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(20.dp)
            .verticalScroll(scrollState)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val isDark = MaterialTheme.colorScheme.surface.let {
                    (0.299f * it.red + 0.587f * it.green + 0.114f * it.blue) < 0.5f
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = if (isDark) Color.White else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Theme & Colours",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Personalize app appearance",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // SECTION 1: Theme Mode
        Text(
            text = "Theme Mode",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Select your preferred light/dark interface style",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
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
                    modifier = Modifier.weight(1f)
                )
                LargeThemeCard(
                    option = themeOptions[1],
                    isSelected = themeMode == themeOptions[1].mode,
                    onClick = { onSelectThemeMode(themeOptions[1].mode) },
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
                    modifier = Modifier.weight(1f)
                )
                LargeThemeCard(
                    option = themeOptions[3],
                    isSelected = themeMode == themeOptions[3].mode,
                    onClick = { onSelectThemeMode(themeOptions[3].mode) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION 2: Colour Palette (16 circular color options in 4x4 grid)
        Text(
            text = "Colour Palette",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "16 distinct primary accent colors",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                            onClick = { onSelectColorHex(hex) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Bottom Action Button
        Surface(
            onClick = onClose,
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "Apply & Done",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
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
    modifier: Modifier = Modifier
) {
    val activeBorderColor = MaterialTheme.colorScheme.primary
    val activeContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    val inactiveContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val inactiveBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)

    Card(
        onClick = onClick,
        modifier = modifier
            .height(72.dp)
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
            val isDark = MaterialTheme.colorScheme.surface.let {
                (0.299f * it.red + 0.587f * it.green + 0.114f * it.blue) < 0.5f
            }

            Column(
                modifier = Modifier.align(Alignment.CenterStart),
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = option.icon,
                    contentDescription = option.title,
                    tint = if (isSelected) {
                        if (isDark) Color.White else MaterialTheme.colorScheme.primary
                    } else {
                        if (isDark) Color.White.copy(alpha = 0.65f) else MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = option.title,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) {
                        if (isDark) Color.White else MaterialTheme.colorScheme.primary
                    } else MaterialTheme.colorScheme.onSurface
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = if (isDark) Color.White else MaterialTheme.colorScheme.primary,
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (e: Exception) {
        Color.Red
    }

    val contrastingCheckColor = getContrastingIconColor(colorHex)
    val highlightRingColor = MaterialTheme.colorScheme.primary

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
