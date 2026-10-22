package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BlurCircular
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.FilterTiltShift
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.GlassStyle
import com.example.ui.theme.LiquidGlassSettings
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlin.math.roundToInt

private enum class ValueDialogType {
    VIBRANCY,
    BLUR_RADIUS,
    MENU_BACKDROP_QUALITY,
    LENS_HEIGHT,
    LENS_AMOUNT,
    SURFACE_OPACITY,
    SELECTION_PILL_OPACITY,
    HIGHLIGHT_OPACITY,
}

private enum class ColorDialogType {
    SURFACE_TINT,
    SELECTION_PILL,
    HIGHLIGHT,
    TEXT_AND_ICON,
}

private fun GlassStyle.displayLabel(): String = when (this) {
    GlassStyle.LIQUID -> "Liquid Glass"
    GlassStyle.BLUR -> "Blur only"
    GlassStyle.TRANSPARENT -> "Transparent"
}

/**
 * Full-screen Liquid Glass settings screen with Main page and Advanced page,
 * matching the Convx layout, sections, order, rows, dialogs, colours, and sizes.
 */
@Composable
fun LiquidGlassSettingsScreen(
    isDark: Boolean,
    accentColor: Color,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showAdvancedPage by rememberSaveable { mutableStateOf(false) }
    var activeValueDialog by remember { mutableStateOf<ValueDialogType?>(null) }
    var activeColorDialog by remember { mutableStateOf<ColorDialogType?>(null) }
    var showStyleDialog by remember { mutableStateOf(false) }

    BackHandler {
        if (showAdvancedPage) {
            showAdvancedPage = false
        } else {
            onBack()
        }
    }

    val context = LocalContext.current
    val screenBg = if (isDark) Color(0xFF000000) else Color(0xFFF2F2F7)
    val cardFill = if (isDark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)
    val labelColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000)
    val secondaryLabelColor = if (isDark) {
        Color(0xFFEBEBF5).copy(alpha = 0.60f)
    } else {
        Color(0xFF3C3C43).copy(alpha = 0.60f)
    }
    val separatorColor = if (isDark) Color(0xFF38383A) else Color(0xFFC6C6C8)

    val liveConfig = LiquidGlassSettings.resolveConfig(isDark)

    // Backdrop for the inline Blur strength GlassSlider on the page
    val pagePaintBackdrop: androidx.compose.ui.graphics.drawscope.ContentDrawScope.() -> Unit =
        remember(screenBg) {
            {
                drawRect(screenBg)
                drawContent()
            }
        }
    val pageBackdrop = rememberLayerBackdrop(onDraw = pagePaintBackdrop)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(screenBg)
            .layerBackdrop(pageBackdrop)
            .testTag(if (showAdvancedPage) "liquid_glass_advanced_screen" else "liquid_glass_main_screen"),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .windowInsetsPadding(WindowInsets.navigationBars),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top bar: Back arrow on the left in accent colour, title "Liquid Glass" centred, semibold
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
                    .height(52.dp)
                    .padding(horizontal = 8.dp),
            ) {
                IconButton(
                    onClick = {
                        if (showAdvancedPage) {
                            showAdvancedPage = false
                        } else {
                            onBack()
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .testTag("liquid_glass_back_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = accentColor,
                    )
                }

                Text(
                    text = "Liquid Glass",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = labelColor,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            // Page body: Column, 16dp side padding, max width 720dp, centred
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // 1. Preview card (LiquidGlassPreview), 190dp tall
                LiquidGlassPreview(
                    isDark = isDark,
                    config = liveConfig,
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Blur strength row: "Blur strength" on the left (titleMedium),
                // value "${blurRadius.roundToInt()}%" on the right (secondary label).
                // Below it, full-width GlassSlider, range 0..100, bound to blurRadius.
                // 8dp above the slider, 16dp below it. Inline, no dialog.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Blur strength",
                        style = MaterialTheme.typography.titleMedium,
                        color = labelColor,
                    )
                    Text(
                        text = "${LiquidGlassSettings.blurRadius.roundToInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        color = secondaryLabelColor,
                        modifier = Modifier.testTag("liquid_glass_blur_strength_value"),
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                GlassSlider(
                    value = LiquidGlassSettings.blurRadius,
                    onValueChange = { newVal ->
                        LiquidGlassSettings.setBlurRadius(context, newVal)
                    },
                    valueRange = 0f..100f,
                    activeColor = accentColor,
                    inactiveColor = separatorColor,
                    backdrop = pageBackdrop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("liquid_glass_blur_strength_slider"),
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (!showAdvancedPage) {
                    // 3. On the main page: one group "More controls" with one row
                    SettingsGroupTitle(
                        title = "More controls",
                        secondaryLabelColor = secondaryLabelColor,
                    )
                    SettingsGroupCard(cardFill = cardFill) {
                        SettingsRow(
                            icon = Icons.Default.Tune,
                            title = "Advanced glass settings",
                            subtitle = "Style, color, highlights and per-component effects",
                            accentColor = accentColor,
                            labelColor = labelColor,
                            secondaryLabelColor = secondaryLabelColor,
                            isDark = isDark,
                            onClick = { showAdvancedPage = true },
                            testTag = "row_advanced_glass_settings",
                            trailing = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = secondaryLabelColor,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            },
                        )
                    }
                } else {
                    // 3. On the advanced page: text "Advanced glass controls" (titleMedium,
                    // secondary label, 4dp start, 12dp bottom), then the three groups below.
                    Text(
                        text = "Advanced glass controls",
                        style = MaterialTheme.typography.titleMedium,
                        color = secondaryLabelColor,
                        modifier = Modifier.padding(start = 4.dp, bottom = 12.dp),
                    )

                    // Group 1: "Effects"
                    SettingsGroupTitle(
                        title = "Effects",
                        secondaryLabelColor = secondaryLabelColor,
                    )
                    SettingsGroupCard(cardFill = cardFill) {
                        // a. Vibrancy
                        SettingsRow(
                            icon = Icons.Default.WbSunny,
                            title = "Vibrancy",
                            subtitle = "Color saturation of content behind the glass",
                            accentColor = accentColor,
                            labelColor = labelColor,
                            secondaryLabelColor = secondaryLabelColor,
                            isDark = isDark,
                            onClick = { activeValueDialog = ValueDialogType.VIBRANCY },
                            testTag = "row_vibrancy",
                            trailing = {
                                TrailingValueText(
                                    text = "${(LiquidGlassSettings.vibrancy * 100f).roundToInt()}%",
                                    secondaryLabelColor = secondaryLabelColor,
                                )
                            },
                        )
                        SettingsRowDivider(separatorColor = separatorColor)

                        // b. Blur radius
                        SettingsRow(
                            icon = Icons.Default.BlurOn,
                            title = "Blur radius",
                            subtitle = "Gaussian blur strength in density-independent pixels",
                            accentColor = accentColor,
                            labelColor = labelColor,
                            secondaryLabelColor = secondaryLabelColor,
                            isDark = isDark,
                            onClick = { activeValueDialog = ValueDialogType.BLUR_RADIUS },
                            testTag = "row_blur_radius",
                            trailing = {
                                TrailingValueText(
                                    text = "${LiquidGlassSettings.blurRadius.roundToInt()} dp",
                                    secondaryLabelColor = secondaryLabelColor,
                                )
                            },
                        )
                        SettingsRowDivider(separatorColor = separatorColor)

                        // c. Menu backdrop quality
                        SettingsRow(
                            icon = Icons.Default.HighQuality,
                            title = "Menu backdrop quality",
                            subtitle = "Capture resolution for popup menu glass",
                            accentColor = accentColor,
                            labelColor = labelColor,
                            secondaryLabelColor = secondaryLabelColor,
                            isDark = isDark,
                            onClick = { activeValueDialog = ValueDialogType.MENU_BACKDROP_QUALITY },
                            testTag = "row_menu_backdrop_quality",
                            trailing = {
                                TrailingValueText(
                                    text = "${(LiquidGlassSettings.menuBackdropResolution * 100f).roundToInt()}%",
                                    secondaryLabelColor = secondaryLabelColor,
                                )
                            },
                        )
                        SettingsRowDivider(separatorColor = separatorColor)

                        // d. Lens height
                        SettingsRow(
                            icon = Icons.Default.FilterTiltShift,
                            title = "Lens height",
                            subtitle = "Refraction depth of the glass curvature",
                            accentColor = accentColor,
                            labelColor = labelColor,
                            secondaryLabelColor = secondaryLabelColor,
                            isDark = isDark,
                            onClick = { activeValueDialog = ValueDialogType.LENS_HEIGHT },
                            testTag = "row_lens_height",
                            trailing = {
                                TrailingValueText(
                                    text = "${(LiquidGlassSettings.lensHeight * 100f).roundToInt()}%",
                                    secondaryLabelColor = secondaryLabelColor,
                                )
                            },
                        )
                        SettingsRowDivider(separatorColor = separatorColor)

                        // e. Lens amount
                        SettingsRow(
                            icon = Icons.Default.CenterFocusStrong,
                            title = "Lens amount",
                            subtitle = "Strength of light bending at glass edges",
                            accentColor = accentColor,
                            labelColor = labelColor,
                            secondaryLabelColor = secondaryLabelColor,
                            isDark = isDark,
                            onClick = { activeValueDialog = ValueDialogType.LENS_AMOUNT },
                            testTag = "row_lens_amount",
                            trailing = {
                                TrailingValueText(
                                    text = "${(LiquidGlassSettings.lensAmount * 100f).roundToInt()}%",
                                    secondaryLabelColor = secondaryLabelColor,
                                )
                            },
                        )
                        SettingsRowDivider(separatorColor = separatorColor)

                        // f. Chromatic aberration
                        SettingsRow(
                            icon = Icons.Default.BlurCircular,
                            title = "Chromatic aberration",
                            subtitle = "RGB color fringing on refracted edges",
                            accentColor = accentColor,
                            labelColor = labelColor,
                            secondaryLabelColor = secondaryLabelColor,
                            isDark = isDark,
                            onClick = {
                                LiquidGlassSettings.setChromaticAberration(
                                    context,
                                    !LiquidGlassSettings.chromaticAberration
                                )
                            },
                            testTag = "row_chromatic_aberration",
                            trailing = {
                                IosSwitch(
                                    checked = LiquidGlassSettings.chromaticAberration,
                                    isDark = isDark,
                                )
                            },
                        )
                        SettingsRowDivider(separatorColor = separatorColor)

                        // g. Depth effect
                        SettingsRow(
                            icon = Icons.Default.ViewInAr,
                            title = "Depth effect",
                            subtitle = "3D parallax depth inside the lens",
                            accentColor = accentColor,
                            labelColor = labelColor,
                            secondaryLabelColor = secondaryLabelColor,
                            isDark = isDark,
                            onClick = {
                                LiquidGlassSettings.setDepthEffect(
                                    context,
                                    !LiquidGlassSettings.depthEffect
                                )
                            },
                            testTag = "row_depth_effect",
                            trailing = {
                                IosSwitch(
                                    checked = LiquidGlassSettings.depthEffect,
                                    isDark = isDark,
                                )
                            },
                        )
                    }

                    // Group 2: "Appearance"
                    SettingsGroupTitle(
                        title = "Appearance",
                        secondaryLabelColor = secondaryLabelColor,
                    )
                    SettingsGroupCard(cardFill = cardFill) {
                        // h. Surface tint
                        SettingsRow(
                            icon = Icons.Default.FormatColorFill,
                            title = "Surface tint",
                            subtitle = "Base color of the glass material",
                            accentColor = accentColor,
                            labelColor = labelColor,
                            secondaryLabelColor = secondaryLabelColor,
                            isDark = isDark,
                            onClick = { activeColorDialog = ColorDialogType.SURFACE_TINT },
                            testTag = "row_surface_tint",
                            trailing = {
                                ColorSwatchCircle(
                                    color = liveConfig.surfaceTintColor,
                                    separatorColor = separatorColor,
                                )
                            },
                        )
                        SettingsRowDivider(separatorColor = separatorColor)

                        // i. Surface opacity
                        SettingsRow(
                            icon = Icons.Default.Opacity,
                            title = "Surface opacity",
                            subtitle = "How opaque the glass tint appears",
                            accentColor = accentColor,
                            labelColor = labelColor,
                            secondaryLabelColor = secondaryLabelColor,
                            isDark = isDark,
                            onClick = { activeValueDialog = ValueDialogType.SURFACE_OPACITY },
                            testTag = "row_surface_opacity",
                            trailing = {
                                TrailingValueText(
                                    text = "${(liveConfig.surfaceOpacity * 100f).roundToInt()}%",
                                    secondaryLabelColor = secondaryLabelColor,
                                )
                            },
                        )
                        SettingsRowDivider(separatorColor = separatorColor)

                        // j. Glass style
                        SettingsRow(
                            icon = Icons.Default.Style,
                            title = "Glass style",
                            subtitle = "Liquid Glass, plain blur, or transparent",
                            accentColor = accentColor,
                            labelColor = labelColor,
                            secondaryLabelColor = secondaryLabelColor,
                            isDark = isDark,
                            onClick = { showStyleDialog = true },
                            testTag = "row_glass_style",
                            trailing = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TrailingValueText(
                                        text = LiquidGlassSettings.glassStyle.displayLabel(),
                                        secondaryLabelColor = secondaryLabelColor,
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = secondaryLabelColor,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            },
                        )
                        SettingsRowDivider(separatorColor = separatorColor)

                        // k. Selection pill color
                        SettingsRow(
                            icon = Icons.Default.Palette,
                            title = "Selection pill color",
                            subtitle = "Active tab indicator color",
                            accentColor = accentColor,
                            labelColor = labelColor,
                            secondaryLabelColor = secondaryLabelColor,
                            isDark = isDark,
                            onClick = { activeColorDialog = ColorDialogType.SELECTION_PILL },
                            testTag = "row_selection_pill_color",
                            trailing = {
                                ColorSwatchCircle(
                                    color = liveConfig.selectionPillColor,
                                    separatorColor = separatorColor,
                                )
                            },
                        )
                        SettingsRowDivider(separatorColor = separatorColor)

                        // l. Selection pill opacity
                        SettingsRow(
                            icon = Icons.Default.Layers,
                            title = "Selection pill opacity",
                            subtitle = "How opaque the active tab pill appears",
                            accentColor = accentColor,
                            labelColor = labelColor,
                            secondaryLabelColor = secondaryLabelColor,
                            isDark = isDark,
                            onClick = { activeValueDialog = ValueDialogType.SELECTION_PILL_OPACITY },
                            testTag = "row_selection_pill_opacity",
                            trailing = {
                                TrailingValueText(
                                    text = "${(liveConfig.selectionPillOpacity * 100f).roundToInt()}%",
                                    secondaryLabelColor = secondaryLabelColor,
                                )
                            },
                        )
                        SettingsRowDivider(separatorColor = separatorColor)

                        // m. Highlight color
                        SettingsRow(
                            icon = Icons.Default.AutoAwesome,
                            title = "Highlight color",
                            subtitle = "Edge specular highlight color",
                            accentColor = accentColor,
                            labelColor = labelColor,
                            secondaryLabelColor = secondaryLabelColor,
                            isDark = isDark,
                            onClick = { activeColorDialog = ColorDialogType.HIGHLIGHT },
                            testTag = "row_highlight_color",
                            trailing = {
                                ColorSwatchCircle(
                                    color = liveConfig.highlightColor,
                                    separatorColor = separatorColor,
                                )
                            },
                        )
                        SettingsRowDivider(separatorColor = separatorColor)

                        // n. Highlight opacity
                        SettingsRow(
                            icon = Icons.Default.ColorLens,
                            title = "Highlight opacity",
                            subtitle = "Brightness of the edge highlight",
                            accentColor = accentColor,
                            labelColor = labelColor,
                            secondaryLabelColor = secondaryLabelColor,
                            isDark = isDark,
                            onClick = { activeValueDialog = ValueDialogType.HIGHLIGHT_OPACITY },
                            testTag = "row_highlight_opacity",
                            trailing = {
                                TrailingValueText(
                                    text = "${(liveConfig.highlightOpacity * 100f).roundToInt()}%",
                                    secondaryLabelColor = secondaryLabelColor,
                                )
                            },
                        )
                        SettingsRowDivider(separatorColor = separatorColor)

                        // o. Adaptive contrast
                        SettingsRow(
                            icon = Icons.Default.Contrast,
                            title = "Adaptive contrast",
                            subtitle = "Auto-switch text between black and white based on background",
                            accentColor = accentColor,
                            labelColor = labelColor,
                            secondaryLabelColor = secondaryLabelColor,
                            isDark = isDark,
                            onClick = {
                                LiquidGlassSettings.setAdaptiveContrast(
                                    context,
                                    !LiquidGlassSettings.adaptiveContrast
                                )
                            },
                            testTag = "row_adaptive_contrast",
                            trailing = {
                                IosSwitch(
                                    checked = LiquidGlassSettings.adaptiveContrast,
                                    isDark = isDark,
                                )
                            },
                        )

                        // p. Text & icon color — Only shown when adaptiveContrast is false
                        if (!LiquidGlassSettings.adaptiveContrast) {
                            SettingsRowDivider(separatorColor = separatorColor)
                            SettingsRow(
                                icon = Icons.Default.FormatColorText,
                                title = "Text & icon color",
                                subtitle = "Color of labels and icons on glass (when adaptive is off)",
                                accentColor = accentColor,
                                labelColor = labelColor,
                                secondaryLabelColor = secondaryLabelColor,
                                isDark = isDark,
                                onClick = { activeColorDialog = ColorDialogType.TEXT_AND_ICON },
                                testTag = "row_text_and_icon_color",
                                trailing = {
                                    ColorSwatchCircle(
                                        color = liveConfig.glassTextColor,
                                        separatorColor = separatorColor,
                                    )
                                },
                            )
                        }
                    }

                    // Group 3: "Per Component"
                    SettingsGroupTitle(
                        title = "Per Component",
                        secondaryLabelColor = secondaryLabelColor,
                    )
                    SettingsGroupCard(cardFill = cardFill) {
                        // q. Bottom navigation bar
                        SettingsRow(
                            icon = Icons.Default.Navigation,
                            title = "Bottom navigation bar",
                            subtitle = "Liquid glass on the main navigation bar",
                            accentColor = accentColor,
                            labelColor = labelColor,
                            secondaryLabelColor = secondaryLabelColor,
                            isDark = isDark,
                            onClick = {
                                LiquidGlassSettings.setNavBarGlassEnabled(
                                    context,
                                    !LiquidGlassSettings.navBarGlassEnabled
                                )
                            },
                            testTag = "row_nav_bar_glass",
                            trailing = {
                                IosSwitch(
                                    checked = LiquidGlassSettings.navBarGlassEnabled,
                                    isDark = isDark,
                                )
                            },
                        )
                        SettingsRowDivider(separatorColor = separatorColor)

                        // r. Chat input & header
                        SettingsRow(
                            icon = Icons.Default.ChatBubbleOutline,
                            title = "Chat input & header",
                            subtitle = "Liquid glass on message bar and top bar",
                            accentColor = accentColor,
                            labelColor = labelColor,
                            secondaryLabelColor = secondaryLabelColor,
                            isDark = isDark,
                            onClick = {
                                LiquidGlassSettings.setChatGlassEnabled(
                                    context,
                                    !LiquidGlassSettings.chatGlassEnabled
                                )
                            },
                            testTag = "row_chat_glass",
                            trailing = {
                                IosSwitch(
                                    checked = LiquidGlassSettings.chatGlassEnabled,
                                    isDark = isDark,
                                )
                            },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // 1. Value dialog (for rows a, b, c, d, e, i, l, n)
    val valueDialog = activeValueDialog
    if (valueDialog != null) {
        when (valueDialog) {
            ValueDialogType.VIBRANCY -> {
                GlassValueDialog(
                    title = "Vibrancy",
                    subtitle = "Color saturation of content behind the glass",
                    initialDisplayValue = (LiquidGlassSettings.vibrancy * 100f).roundToInt().toFloat(),
                    defaultDisplayValue = (LiquidGlassSettings.DEFAULT_VIBRANCY * 100f).roundToInt().toFloat(),
                    valueRange = 0f..200f,
                    step = 1f,
                    unitSuffix = "%",
                    cardFill = cardFill,
                    labelColor = labelColor,
                    secondaryLabelColor = secondaryLabelColor,
                    separatorColor = separatorColor,
                    accentColor = accentColor,
                    onDismiss = { activeValueDialog = null },
                    onSave = { displayVal ->
                        LiquidGlassSettings.setVibrancy(context, displayVal / 100f)
                    },
                )
            }

            ValueDialogType.BLUR_RADIUS -> {
                GlassValueDialog(
                    title = "Blur radius",
                    subtitle = "Gaussian blur strength in density-independent pixels",
                    initialDisplayValue = LiquidGlassSettings.blurRadius.roundToInt().toFloat(),
                    defaultDisplayValue = LiquidGlassSettings.DEFAULT_BLUR_RADIUS.roundToInt().toFloat(),
                    valueRange = 0f..100f,
                    step = 1f,
                    unitSuffix = " dp",
                    cardFill = cardFill,
                    labelColor = labelColor,
                    secondaryLabelColor = secondaryLabelColor,
                    separatorColor = separatorColor,
                    accentColor = accentColor,
                    onDismiss = { activeValueDialog = null },
                    onSave = { displayVal ->
                        LiquidGlassSettings.setBlurRadius(context, displayVal)
                    },
                )
            }

            ValueDialogType.MENU_BACKDROP_QUALITY -> {
                GlassValueDialog(
                    title = "Menu backdrop quality",
                    subtitle = "Capture resolution for popup menu glass",
                    initialDisplayValue = (LiquidGlassSettings.menuBackdropResolution * 100f).roundToInt().toFloat(),
                    defaultDisplayValue = (LiquidGlassSettings.DEFAULT_MENU_BACKDROP_RESOLUTION * 100f).roundToInt().toFloat(),
                    valueRange = 30f..100f,
                    step = 5f,
                    unitSuffix = "%",
                    cardFill = cardFill,
                    labelColor = labelColor,
                    secondaryLabelColor = secondaryLabelColor,
                    separatorColor = separatorColor,
                    accentColor = accentColor,
                    onDismiss = { activeValueDialog = null },
                    onSave = { displayVal ->
                        LiquidGlassSettings.setMenuBackdropResolution(context, displayVal / 100f)
                    },
                )
            }

            ValueDialogType.LENS_HEIGHT -> {
                GlassValueDialog(
                    title = "Lens height",
                    subtitle = "Refraction depth of the glass curvature",
                    initialDisplayValue = (LiquidGlassSettings.lensHeight * 100f).roundToInt().toFloat(),
                    defaultDisplayValue = (LiquidGlassSettings.DEFAULT_LENS_HEIGHT * 100f).roundToInt().toFloat(),
                    valueRange = 0f..200f,
                    step = 1f,
                    unitSuffix = "%",
                    cardFill = cardFill,
                    labelColor = labelColor,
                    secondaryLabelColor = secondaryLabelColor,
                    separatorColor = separatorColor,
                    accentColor = accentColor,
                    onDismiss = { activeValueDialog = null },
                    onSave = { displayVal ->
                        LiquidGlassSettings.setLensHeight(context, displayVal / 100f)
                    },
                )
            }

            ValueDialogType.LENS_AMOUNT -> {
                GlassValueDialog(
                    title = "Lens amount",
                    subtitle = "Strength of light bending at glass edges",
                    initialDisplayValue = (LiquidGlassSettings.lensAmount * 100f).roundToInt().toFloat(),
                    defaultDisplayValue = (LiquidGlassSettings.DEFAULT_LENS_AMOUNT * 100f).roundToInt().toFloat(),
                    valueRange = 0f..200f,
                    step = 1f,
                    unitSuffix = "%",
                    cardFill = cardFill,
                    labelColor = labelColor,
                    secondaryLabelColor = secondaryLabelColor,
                    separatorColor = separatorColor,
                    accentColor = accentColor,
                    onDismiss = { activeValueDialog = null },
                    onSave = { displayVal ->
                        LiquidGlassSettings.setLensAmount(context, displayVal / 100f)
                    },
                )
            }

            ValueDialogType.SURFACE_OPACITY -> {
                GlassValueDialog(
                    title = "Surface opacity",
                    subtitle = "How opaque the glass tint appears",
                    initialDisplayValue = (liveConfig.surfaceOpacity * 100f).roundToInt().toFloat(),
                    defaultDisplayValue = (LiquidGlassSettings.defaultSurfaceOpacity(isDark) * 100f).roundToInt().toFloat(),
                    valueRange = 0f..100f,
                    step = 1f,
                    unitSuffix = "%",
                    cardFill = cardFill,
                    labelColor = labelColor,
                    secondaryLabelColor = secondaryLabelColor,
                    separatorColor = separatorColor,
                    accentColor = accentColor,
                    onDismiss = { activeValueDialog = null },
                    onSave = { displayVal ->
                        LiquidGlassSettings.setSurfaceOpacity(context, displayVal / 100f)
                    },
                )
            }

            ValueDialogType.SELECTION_PILL_OPACITY -> {
                GlassValueDialog(
                    title = "Selection pill opacity",
                    subtitle = "How opaque the active tab pill appears",
                    initialDisplayValue = (liveConfig.selectionPillOpacity * 100f).roundToInt().toFloat(),
                    defaultDisplayValue = (LiquidGlassSettings.defaultSelectionPillOpacity(isDark) * 100f).roundToInt().toFloat(),
                    valueRange = 0f..100f,
                    step = 1f,
                    unitSuffix = "%",
                    cardFill = cardFill,
                    labelColor = labelColor,
                    secondaryLabelColor = secondaryLabelColor,
                    separatorColor = separatorColor,
                    accentColor = accentColor,
                    onDismiss = { activeValueDialog = null },
                    onSave = { displayVal ->
                        LiquidGlassSettings.setSelectionPillOpacity(context, displayVal / 100f)
                    },
                )
            }

            ValueDialogType.HIGHLIGHT_OPACITY -> {
                GlassValueDialog(
                    title = "Highlight opacity",
                    subtitle = "Brightness of the edge highlight",
                    initialDisplayValue = (liveConfig.highlightOpacity * 100f).roundToInt().toFloat(),
                    defaultDisplayValue = (LiquidGlassSettings.defaultHighlightOpacity(isDark) * 100f).roundToInt().toFloat(),
                    valueRange = 0f..100f,
                    step = 1f,
                    unitSuffix = "%",
                    cardFill = cardFill,
                    labelColor = labelColor,
                    secondaryLabelColor = secondaryLabelColor,
                    separatorColor = separatorColor,
                    accentColor = accentColor,
                    onDismiss = { activeValueDialog = null },
                    onSave = { displayVal ->
                        LiquidGlassSettings.setHighlightOpacity(context, displayVal / 100f)
                    },
                )
            }
        }
    }

    // 2. Colour dialog (GlassColorPickerDialog)
    val colorDialog = activeColorDialog
    if (colorDialog != null) {
        when (colorDialog) {
            ColorDialogType.SURFACE_TINT -> {
                GlassColorPickerDialog(
                    title = "Surface tint",
                    initialColor = liveConfig.surfaceTintColor,
                    defaultColor = LiquidGlassSettings.defaultSurfaceTintColor(isDark),
                    isDark = isDark,
                    accentColor = accentColor,
                    onDismiss = { activeColorDialog = null },
                    onDone = { selectedColor, isReset ->
                        if (isReset) {
                            LiquidGlassSettings.setSurfaceTintColor(context, 0)
                        } else {
                            LiquidGlassSettings.setSurfaceTintColor(context, selectedColor.toArgb())
                        }
                    },
                )
            }

            ColorDialogType.SELECTION_PILL -> {
                GlassColorPickerDialog(
                    title = "Selection pill color",
                    initialColor = liveConfig.selectionPillColor,
                    defaultColor = LiquidGlassSettings.defaultSelectionPillColor(isDark),
                    isDark = isDark,
                    accentColor = accentColor,
                    onDismiss = { activeColorDialog = null },
                    onDone = { selectedColor, isReset ->
                        if (isReset) {
                            LiquidGlassSettings.setSelectionPillColor(context, 0)
                        } else {
                            LiquidGlassSettings.setSelectionPillColor(context, selectedColor.toArgb())
                        }
                    },
                )
            }

            ColorDialogType.HIGHLIGHT -> {
                GlassColorPickerDialog(
                    title = "Highlight color",
                    initialColor = liveConfig.highlightColor,
                    defaultColor = LiquidGlassSettings.defaultHighlightColor(isDark),
                    isDark = isDark,
                    accentColor = accentColor,
                    onDismiss = { activeColorDialog = null },
                    onDone = { selectedColor, isReset ->
                        if (isReset) {
                            LiquidGlassSettings.setHighlightColor(context, 0)
                        } else {
                            LiquidGlassSettings.setHighlightColor(context, selectedColor.toArgb())
                        }
                    },
                )
            }

            ColorDialogType.TEXT_AND_ICON -> {
                GlassColorPickerDialog(
                    title = "Text & icon color",
                    initialColor = liveConfig.glassTextColor,
                    defaultColor = LiquidGlassSettings.defaultGlassTextColor(isDark),
                    isDark = isDark,
                    accentColor = accentColor,
                    onDismiss = { activeColorDialog = null },
                    onDone = { selectedColor, isReset ->
                        if (isReset) {
                            LiquidGlassSettings.setGlassTextColor(context, 0)
                        } else {
                            LiquidGlassSettings.setGlassTextColor(context, selectedColor.toArgb())
                        }
                    },
                )
            }
        }
    }

    // 3. Glass style dialog (row j)
    if (showStyleDialog) {
        GlassStyleDialog(
            currentStyle = LiquidGlassSettings.glassStyle,
            cardFill = cardFill,
            labelColor = labelColor,
            accentColor = accentColor,
            isDark = isDark,
            onDismiss = { showStyleDialog = false },
            onSelectStyle = { style ->
                LiquidGlassSettings.setGlassStyle(context, style)
                showStyleDialog = false
            },
        )
    }
}

@Composable
private fun SettingsGroupTitle(
    title: String,
    secondaryLabelColor: Color,
) {
    Text(
        text = title.uppercase(),
        fontSize = 13.sp,
        letterSpacing = 0.6.sp,
        color = secondaryLabelColor,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 6.dp),
    )
}

@Composable
private fun SettingsGroupCard(
    cardFill: Color,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cardFill),
    ) {
        content()
    }
}

@Composable
private fun SettingsRowDivider(
    separatorColor: Color,
) {
    HorizontalDivider(
        modifier = Modifier.padding(start = 60.dp),
        thickness = 0.5.dp,
        color = separatorColor,
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    labelColor: Color,
    secondaryLabelColor: Color,
    isDark: Boolean,
    onClick: () -> Unit,
    testTag: String,
    trailing: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressedBg = if (isPressed) {
        if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
    } else {
        Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .background(pressedBg)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 11.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Icon tile: 30dp square, 7dp corners, accent background, 18dp white icon centred
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(accentColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        }

        // 14dp gap before the text column
        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
        ) {
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Normal,
                color = labelColor,
            )
            Text(
                text = subtitle,
                fontSize = 13.sp,
                lineHeight = 16.sp,
                color = secondaryLabelColor,
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        trailing()
    }
}

@Composable
private fun TrailingValueText(
    text: String,
    secondaryLabelColor: Color,
) {
    Text(
        text = text,
        fontSize = 17.sp,
        fontWeight = FontWeight.Normal,
        color = secondaryLabelColor,
    )
}

@Composable
private fun ColorSwatchCircle(
    color: Color,
    separatorColor: Color,
) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(color)
            .border(1.dp, separatorColor, CircleShape),
    )
}

/**
 * iOS switch (51dp x 31dp).
 * Switch ON: #34C759
 * Switch OFF: #E9E9EA (light) / #39393D (dark)
 */
@Composable
private fun IosSwitch(
    checked: Boolean,
    isDark: Boolean,
) {
    val progress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "iosSwitchProgress",
    )
    val clampedProgress = progress.coerceIn(0f, 1f)
    val offColor = if (isDark) Color(0xFF39393D) else Color(0xFFE9E9EA)
    val onColor = Color(0xFF34C759)
    val trackColor = lerp(offColor, onColor, clampedProgress)
    val density = LocalDensity.current
    val travelPx = with(density) { 20.dp.toPx() }

    Box(
        modifier = Modifier
            .size(width = 51.dp, height = 31.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(trackColor)
            .padding(2.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset((clampedProgress * travelPx).roundToInt(), 0) }
                .size(27.dp)
                .shadow(elevation = 2.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

/**
 * Value dialog (for rows a, b, c, d, e, i, l, n):
 * Rounded 28dp, card fill colour, 24dp padding.
 * Title (titleLarge), 8dp spacer, subtitle (bodyMedium secondary), 20dp spacer,
 * current value (headlineMedium semibold, centred), 12dp spacer,
 * GlassSlider (backdrop = null), 16dp spacer.
 * Bottom row: "Reset" on the left, "Cancel" and "Done" on the right in accent.
 */
@Composable
private fun GlassValueDialog(
    title: String,
    subtitle: String,
    initialDisplayValue: Float,
    defaultDisplayValue: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    step: Float,
    unitSuffix: String,
    cardFill: Color,
    labelColor: Color,
    secondaryLabelColor: Color,
    separatorColor: Color,
    accentColor: Color,
    onDismiss: () -> Unit,
    onSave: (Float) -> Unit,
) {
    fun snapToStep(raw: Float): Float {
        val clamped = raw.coerceIn(valueRange.start, valueRange.endInclusive)
        if (step <= 0f) return clamped
        val steps = ((clamped - valueRange.start) / step).roundToInt()
        return (valueRange.start + steps * step).coerceIn(valueRange.start, valueRange.endInclusive)
    }

    var currentVal by remember { mutableFloatStateOf(snapToStep(initialDisplayValue)) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .width(320.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(cardFill)
                .padding(24.dp)
                .testTag("glass_value_dialog"),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = labelColor,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = secondaryLabelColor,
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "${currentVal.roundToInt()}$unitSuffix",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = labelColor,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("glass_value_dialog_readout"),
            )

            Spacer(modifier = Modifier.height(12.dp))

            GlassSlider(
                value = currentVal,
                onValueChange = { raw ->
                    currentVal = snapToStep(raw)
                },
                valueRange = valueRange,
                activeColor = accentColor,
                inactiveColor = separatorColor,
                backdrop = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("glass_value_dialog_slider"),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Reset",
                    color = accentColor,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            currentVal = snapToStep(defaultDisplayValue)
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("glass_value_dialog_reset"),
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Cancel",
                        color = accentColor,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onDismiss)
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("glass_value_dialog_cancel"),
                    )

                    Text(
                        text = "Done",
                        color = accentColor,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onSave(currentVal)
                                onDismiss()
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("glass_value_dialog_done"),
                    )
                }
            }
        }
    }
}

/**
 * Glass style dialog (row j):
 * Rounded 28dp, card fill colour, 24dp padding.
 * Title "Glass style", 16dp spacer.
 * Three rows (48dp min height): "Liquid Glass", "Blur only", "Transparent".
 * Selected row is semibold with a check icon in accent on the right.
 * Tapping a row selects it and closes the dialog.
 * "Cancel" at the bottom right in accent.
 */
@Composable
private fun GlassStyleDialog(
    currentStyle: GlassStyle,
    cardFill: Color,
    labelColor: Color,
    accentColor: Color,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onSelectStyle: (GlassStyle) -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .width(320.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(cardFill)
                .padding(24.dp)
                .testTag("glass_style_dialog"),
        ) {
            Text(
                text = "Glass style",
                style = MaterialTheme.typography.titleLarge,
                color = labelColor,
            )

            Spacer(modifier = Modifier.height(16.dp))

            GlassStyle.entries.forEach { style ->
                val isSelected = style == currentStyle
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val pressedBg = if (isPressed) {
                    if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
                } else {
                    Color.Transparent
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(pressedBg)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { onSelectStyle(style) },
                        )
                        .padding(horizontal = 8.dp)
                        .testTag("glass_style_option_${style.name.lowercase()}"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = style.displayLabel(),
                        fontSize = 17.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = labelColor,
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = accentColor,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    text = "Cancel",
                    color = accentColor,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("glass_style_dialog_cancel"),
                )
            }
        }
    }
}
