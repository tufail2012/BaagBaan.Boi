package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppThemeMode
import com.example.ui.components.glassCardBackground
import com.example.ui.theme.AppPalette
import com.example.ui.theme.PredefinedThemePalettes
import com.example.ui.theme.getAppDimBackgroundBrush

// 16 Distinct theme color options in HEX (Preserved exactly)
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
    selectedPaletteId: String? = null,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    onSelectColorHex: (String) -> Unit,
    onSelectPaletteId: (String) -> Unit = {},
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
            // Wide Pill-Shaped Glass Header (Matching Reference & App Design System)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .frostedGlassChrome(
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
                                text = "Theme Colors",
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
                                text = "Choose a solid color or a pre-designed color palette for your app.",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = if (isDark) Color(0xFF94A3B8) else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = onDismissRequest,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("theme_colours_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = if (isDark) Color.White.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            ThemeColoursContent(
                themeMode = themeMode,
                selectedColorHex = selectedColorHex,
                selectedPaletteId = selectedPaletteId,
                onSelectThemeMode = onSelectThemeMode,
                onSelectColorHex = onSelectColorHex,
                onSelectPaletteId = onSelectPaletteId,
                onClose = onDismissRequest,
                isDark = isDark,
                themeModeState = themeMode,
                currentAccentColor = currentAccentColor,
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
    selectedPaletteId: String? = null,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    onSelectColorHex: (String) -> Unit,
    onSelectPaletteId: (String) -> Unit = {},
    onClose: () -> Unit,
    isDark: Boolean = false,
    themeModeState: AppThemeMode = AppThemeMode.SYSTEM,
    currentAccentColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // SECTION 1: Theme Mode Selection Card
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
                    text = "Choose how the application appears",
                    fontSize = 12.sp,
                    color = if (isDark) Color(0xFF94A3B8) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 14.dp)
                )

                val modes = listOf(
                    ThemeModeOption(AppThemeMode.SYSTEM, "System", Icons.Default.PhoneAndroid),
                    ThemeModeOption(AppThemeMode.LIGHT, "Light", Icons.Default.LightMode),
                    ThemeModeOption(AppThemeMode.DARK, "Dark", Icons.Default.NightsStay),
                    ThemeModeOption(AppThemeMode.AMOLED, "AMOLED", Icons.Default.Contrast)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    modes.forEach { option ->
                        LargeThemeCard(
                            option = option,
                            isSelected = themeMode == option.mode,
                            onClick = { onSelectThemeMode(option.mode) },
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

        // SECTION 2: Theme Colors Card (Divided into Solid Colors & Color Palettes)
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
                // Main Header inside the Theme Colors Card
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(currentAccentColor.copy(alpha = if (isDark) 0.25f else 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = currentAccentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Theme Colors",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Choose a solid color or a pre-designed color palette for your app.",
                            fontSize = 11.5.sp,
                            color = if (isDark) Color(0xFF94A3B8) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // SUBSECTION A: Solid Colors (16 choices)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Solid Colors",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(16)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = currentAccentColor
                        )
                    }

                    Text(
                        text = "Single accent colors",
                        fontSize = 11.sp,
                        color = if (isDark) Color(0xFF94A3B8) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 16 Solid Colors Swatches: 2 rows of 8 swatches with labels 01 to 16
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ThemeColorPalette16.chunked(8).forEachIndexed { rowIndex, rowColors ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            rowColors.forEachIndexed { colIndex, hex ->
                                val itemIndex = rowIndex * 8 + colIndex + 1
                                val indexLabel = String.format("%02d", itemIndex)
                                // Only selected if no palette is active and hex matches
                                val isSelected = selectedPaletteId == null && hex.equals(selectedColorHex, ignoreCase = true)

                                SolidColorSwatch(
                                    hex = hex,
                                    label = indexLabel,
                                    isSelected = isSelected,
                                    currentAccentColor = currentAccentColor,
                                    isDark = isDark,
                                    onClick = { onSelectColorHex(hex) }
                                )
                            }
                        }
                    }
                }

                // Visual Separation Divider between Solid Colors and Color Palettes
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(
                    color = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f),
                    thickness = 1.dp
                )
                Spacer(modifier = Modifier.height(20.dp))

                // SUBSECTION B: Color Palettes (8 choices)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Color Palettes",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(8)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = currentAccentColor
                        )
                    }

                    Text(
                        text = "Curated multi-color palettes",
                        fontSize = 11.sp,
                        color = if (isDark) Color(0xFF94A3B8) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 8 Predefined Palettes arranged in 2 rows of 4 columns
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PredefinedThemePalettes.chunked(4).forEach { rowPalettes ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowPalettes.forEach { palette ->
                                val isSelected = selectedPaletteId.equals(palette.id, ignoreCase = true)
                                PaletteCard(
                                    palette = palette,
                                    isSelected = isSelected,
                                    currentAccentColor = currentAccentColor,
                                    isDark = isDark,
                                    onClick = { onSelectPaletteId(palette.id) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowPalettes.size < 4) {
                                repeat(4 - rowPalettes.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
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

/**
 * Circular Solid Color Swatch with 2-digit number label below it
 */
@Composable
private fun SolidColorSwatch(
    hex: String,
    label: String,
    isSelected: Boolean,
    currentAccentColor: Color,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val color = try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color.Red
    }
    val contrastingCheckColor = getContrastingIconColor(hex)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier.size(34.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                // Highlighted outer circle ring
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .border(
                            width = 2.5.dp,
                            color = currentAccentColor,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(color),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = contrastingCheckColor,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = 1.dp,
                            color = if (isDark) Color.White.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.15f),
                            shape = CircleShape
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) {
                if (isDark) Color.White else currentAccentColor
            } else {
                if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
            },
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Predefined Palette Card:
 * Compact design displaying only:
 * - Palette name at top
 * - Circular palette preview centered
 * - When selected: accent border, subtle container tint, and checkmark badge at top-right
 */
@Composable
private fun PaletteCard(
    palette: AppPalette,
    isSelected: Boolean,
    currentAccentColor: Color,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeBorderColor = palette.primary
    val inactiveBorderColor = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)
    val activeBgColor = if (isDark) palette.primary.copy(alpha = 0.16f) else palette.primary.copy(alpha = 0.08f)
    val inactiveBgColor = if (isDark) Color(0xFF141824).copy(alpha = 0.65f) else Color(0xFFF8FAFC)

    Card(
        onClick = onClick,
        modifier = modifier
            .height(98.dp)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) activeBorderColor else inactiveBorderColor,
                shape = RoundedCornerShape(14.dp)
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) activeBgColor else inactiveBgColor
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Palette Title
                Text(
                    text = palette.name,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) {
                        if (isDark) Color.White else palette.primary
                    } else if (isDark) Color.White else Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp)
                )

                // Circular Palette Preview (divided into 4 quarters or 2 vertical halves)
                CircularPalettePreview(
                    palette = palette,
                    isSelected = isSelected,
                    isDark = isDark,
                    modifier = Modifier.size(38.dp)
                )

                Spacer(modifier = Modifier.height(2.dp))
            }

            // Top-right corner checkmark badge when selected
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(palette.primary)
                        .align(Alignment.TopEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = if (palette.primary.luminance() > 0.5f) Color.Black else Color.White,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }
        }
    }
}

/**
 * Circular Palette Preview:
 * - 4-color palette: perfect circle divided into 4 quarters
 *   - Top-left quarter = Primary (angles 180° to 270°)
 *   - Top-right quarter = Secondary (angles 270° to 360°)
 *   - Bottom-left quarter = Tertiary (angles 90° to 180°)
 *   - Bottom-right quarter = Neutral (angles 0° to 90°)
 * - 2-color palette: perfect circle divided vertically into 2 equal halves
 *   - Left half = Primary (angles 90° to 270°)
 *   - Right half = Secondary (angles 270° to 90°)
 * Compact preview size (~38.dp), similar in size to Solid Color swatches.
 */
@Composable
private fun CircularPalettePreview(
    palette: AppPalette,
    isSelected: Boolean,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .border(
                    width = 1.dp,
                    color = if (isDark) Color.White.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.15f),
                    shape = CircleShape
                )
        ) {
            if (palette.isTwoColor) {
                // Left half = Primary (Text, Icons)
                drawArc(
                    color = palette.primary,
                    startAngle = 90f,
                    sweepAngle = 180f,
                    useCenter = true,
                    size = size
                )
                // Right half = Secondary (Backgrounds)
                drawArc(
                    color = palette.secondary,
                    startAngle = 270f,
                    sweepAngle = 180f,
                    useCenter = true,
                    size = size
                )
            } else {
                // Top-left quarter = Primary (angles 180° to 270°)
                drawArc(
                    color = palette.primary,
                    startAngle = 180f,
                    sweepAngle = 90f,
                    useCenter = true,
                    size = size
                )
                // Top-right quarter = Secondary (angles 270° to 360°)
                drawArc(
                    color = palette.secondary,
                    startAngle = 270f,
                    sweepAngle = 90f,
                    useCenter = true,
                    size = size
                )
                // Bottom-right quarter = Tertiary (angles 0° to 90°)
                drawArc(
                    color = palette.tertiary,
                    startAngle = 0f,
                    sweepAngle = 90f,
                    useCenter = true,
                    size = size
                )
                // Bottom-left quarter = Neutral (angles 90° to 180°)
                drawArc(
                    color = palette.neutral,
                    startAngle = 90f,
                    sweepAngle = 90f,
                    useCenter = true,
                    size = size
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
            .height(72.dp)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) activeBorderColor else inactiveBorderColor,
                shape = RoundedCornerShape(14.dp)
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) activeContainerColor else inactiveContainerColor
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
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
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = option.title,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) {
                        if (isDark) Color.White else currentAccentColor
                    } else if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = currentAccentColor,
                    modifier = Modifier
                        .size(15.dp)
                        .align(Alignment.TopEnd)
                )
            }
        }
    }
}
