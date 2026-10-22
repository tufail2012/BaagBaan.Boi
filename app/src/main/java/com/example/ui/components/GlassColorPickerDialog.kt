package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.roundToInt

private enum class ColorPickerTab(val label: String) {
    GRID("Grid"),
    SPECTRUM("Spectrum"),
    SLIDERS("Sliders"),
}

private val GridBands = listOf(
    0.25f to 1.00f,
    0.55f to 0.98f,
    0.85f to 0.95f,
    0.95f to 0.75f,
    1.00f to 0.48f,
)

private val HueRainbowStops = listOf(
    Color(0xFFFF0000),
    Color(0xFFFFFF00),
    Color(0xFF00FF00),
    Color(0xFF00FFFF),
    Color(0xFF0000FF),
    Color(0xFFFF00FF),
    Color(0xFFFF0000),
)

private fun colorToHsvTriple(color: Color): Triple<Float, Float, Float> {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    return Triple(hsv[0].coerceIn(0f, 360f), hsv[1].coerceIn(0f, 1f), hsv[2].coerceIn(0f, 1f))
}

private fun colorToHex6(color: Color): String {
    val argb = color.toArgb()
    return String.format("#%06X", 0xFFFFFF and argb)
}

/**
 * 340dp colour picker dialog for Liquid Glass settings.
 * Supports Grid (12x5 swatches), Spectrum (2D saturation/brightness + hue bar),
 * and Sliders (Hue 0..360, Saturation 0..100, Brightness 0..100 + #RRGGBB hex field).
 */
@Composable
fun GlassColorPickerDialog(
    title: String,
    initialColor: Color,
    defaultColor: Color,
    isDark: Boolean,
    accentColor: Color,
    onDismiss: () -> Unit,
    onDone: (selectedColor: Color, isResetToDefault: Boolean) -> Unit,
) {
    val dialogBg = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
    val labelColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000)
    val secondaryLabelColor = if (isDark) {
        Color(0xFFEBEBF5).copy(alpha = 0.60f)
    } else {
        Color(0xFF3C3C43).copy(alpha = 0.60f)
    }
    val separatorColor = if (isDark) Color(0xFF38383A) else Color(0xFFC6C6C8)
    val segmentedTrackColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA)
    val segmentedSelectedColor = if (isDark) Color(0xFF48484A) else Color(0xFFFFFFFF)

    val initialHsv = remember(initialColor) { colorToHsvTriple(initialColor) }
    var hue by remember { mutableFloatStateOf(initialHsv.first) }
    var saturation by remember { mutableFloatStateOf(initialHsv.second) }
    var brightness by remember { mutableFloatStateOf(initialHsv.third) }
    var exactColorOverride by remember { mutableStateOf<Color?>(initialColor) }
    var isResetToDefault by remember { mutableStateOf(false) }
    var hexText by remember { mutableStateOf(colorToHex6(initialColor)) }
    var selectedTab by remember { mutableIntStateOf(0) }

    val currentColor = exactColorOverride ?: Color.hsv(
        hue = hue.coerceIn(0f, 360f),
        saturation = saturation.coerceIn(0f, 1f),
        value = brightness.coerceIn(0f, 1f),
    )

    fun updateFromHsv(newH: Float, newS: Float, newV: Float) {
        hue = newH.coerceIn(0f, 360f)
        saturation = newS.coerceIn(0f, 1f)
        brightness = newV.coerceIn(0f, 1f)
        exactColorOverride = null
        isResetToDefault = false
        val c = Color.hsv(hue, saturation, brightness)
        hexText = colorToHex6(c)
    }

    fun updateFromColor(color: Color, isReset: Boolean = false) {
        val (h, s, v) = colorToHsvTriple(color)
        hue = h
        saturation = s
        brightness = v
        exactColorOverride = color
        isResetToDefault = isReset
        hexText = colorToHex6(color)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .width(340.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(dialogBg)
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
                .testTag("glass_color_picker_dialog"),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top row: title on the left, "Cancel" in accent on the right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = labelColor,
                )
                Text(
                    text = "Cancel",
                    color = accentColor,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .testTag("color_picker_cancel"),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Preview circle, 44dp, showing the chosen colour
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(currentColor)
                    .border(1.dp, separatorColor, CircleShape)
                    .testTag("color_picker_preview"),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Segmented control with three tabs: Grid, Spectrum, Sliders
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(segmentedTrackColor)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ColorPickerTab.entries.forEachIndexed { index, tab ->
                    val isSelected = selectedTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) segmentedSelectedColor else Color.Transparent)
                            .clickable { selectedTab = index }
                            .padding(vertical = 7.dp)
                            .testTag("color_picker_tab_${tab.name.lowercase()}"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = tab.label,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = labelColor,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (ColorPickerTab.entries[selectedTab]) {
                ColorPickerTab.GRID -> {
                    // 12 columns, 5 rows of swatches. Hues evenly spread across 360 degrees,
                    // with five saturation and brightness bands.
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, separatorColor, RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        GridBands.forEachIndexed { rowIdx, (bandSat, bandVal) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                for (colIdx in 0 until 12) {
                                    val cellHue = colIdx * 30f
                                    val cellColor = Color.hsv(cellHue, bandSat, bandVal)
                                    val isCellSelected =
                                        colorToHex6(cellColor).equals(colorToHex6(currentColor), ignoreCase = true)

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(cellColor)
                                            .then(
                                                if (isCellSelected) {
                                                    Modifier.border(
                                                        width = 2.dp,
                                                        color = if (bandVal > 0.7f) Color.Black else Color.White,
                                                        shape = RoundedCornerShape(4.dp),
                                                    )
                                                } else {
                                                    Modifier
                                                }
                                            )
                                            .clickable {
                                                updateFromColor(cellColor, isReset = false)
                                            }
                                            .testTag("color_grid_${rowIdx}_$colIdx"),
                                    )
                                }
                            }
                        }
                    }
                }

                ColorPickerTab.SPECTRUM -> {
                    // 2D saturation and brightness area + hue bar
                    val pureHueColor = Color.hsv(hue.coerceIn(0f, 360f), 1f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, separatorColor, RoundedCornerShape(14.dp))
                            .pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    if (size.width <= 0 || size.height <= 0) return@detectTapGestures
                                    val newS = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                    val newV = (1f - (offset.y / size.height.toFloat())).coerceIn(0f, 1f)
                                    updateFromHsv(hue, newS, newV)
                                }
                            }
                            .pointerInput(Unit) {
                                detectDragGestures { change, _ ->
                                    change.consume()
                                    if (size.width <= 0 || size.height <= 0) return@detectDragGestures
                                    val newS = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                                    val newV = (1f - (change.position.y / size.height.toFloat())).coerceIn(0f, 1f)
                                    updateFromHsv(hue, newS, newV)
                                }
                            }
                            .testTag("color_spectrum_2d"),
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color.White, pureHueColor)
                                )
                            )
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black)
                                )
                            )
                            val cx = saturation.coerceIn(0f, 1f) * size.width
                            val cy = (1f - brightness.coerceIn(0f, 1f)) * size.height
                            drawCircle(
                                color = Color.White,
                                radius = 10.dp.toPx(),
                                center = Offset(cx, cy),
                                style = Stroke(width = 2.5.dp.toPx()),
                            )
                            drawCircle(
                                color = currentColor,
                                radius = 8.dp.toPx(),
                                center = Offset(cx, cy),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Hue bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, separatorColor, RoundedCornerShape(14.dp))
                            .pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    if (size.width <= 0) return@detectTapGestures
                                    val newH = ((offset.x / size.width.toFloat()) * 360f).coerceIn(0f, 360f)
                                    updateFromHsv(newH, saturation, brightness)
                                }
                            }
                            .pointerInput(Unit) {
                                detectDragGestures { change, _ ->
                                    change.consume()
                                    if (size.width <= 0) return@detectDragGestures
                                    val newH = ((change.position.x / size.width.toFloat()) * 360f).coerceIn(0f, 360f)
                                    updateFromHsv(newH, saturation, brightness)
                                }
                            }
                            .testTag("color_spectrum_hue_bar"),
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawRect(
                                brush = Brush.horizontalGradient(colors = HueRainbowStops)
                            )
                            val hx = (hue.coerceIn(0f, 360f) / 360f) * size.width
                            val hy = size.height / 2f
                            drawCircle(
                                color = Color.White,
                                radius = 11.dp.toPx(),
                                center = Offset(hx, hy),
                                style = Stroke(width = 2.5.dp.toPx()),
                            )
                        }
                    }
                }

                ColorPickerTab.SLIDERS -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        // Hue (0..360)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "Hue",
                                fontSize = 15.sp,
                                color = labelColor,
                            )
                            Text(
                                text = "${hue.roundToInt()}°",
                                fontSize = 15.sp,
                                color = secondaryLabelColor,
                            )
                        }
                        GlassSlider(
                            value = hue,
                            onValueChange = { newH -> updateFromHsv(newH, saturation, brightness) },
                            valueRange = 0f..360f,
                            activeColor = accentColor,
                            inactiveColor = separatorColor,
                            backdrop = null,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        // Saturation (0..100)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "Saturation",
                                fontSize = 15.sp,
                                color = labelColor,
                            )
                            Text(
                                text = "${(saturation * 100f).roundToInt()}%",
                                fontSize = 15.sp,
                                color = secondaryLabelColor,
                            )
                        }
                        GlassSlider(
                            value = saturation * 100f,
                            onValueChange = { newS -> updateFromHsv(hue, newS / 100f, brightness) },
                            valueRange = 0f..100f,
                            activeColor = accentColor,
                            inactiveColor = separatorColor,
                            backdrop = null,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        // Brightness (0..100)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "Brightness",
                                fontSize = 15.sp,
                                color = labelColor,
                            )
                            Text(
                                text = "${(brightness * 100f).roundToInt()}%",
                                fontSize = 15.sp,
                                color = secondaryLabelColor,
                            )
                        }
                        GlassSlider(
                            value = brightness * 100f,
                            onValueChange = { newV -> updateFromHsv(hue, saturation, newV / 100f) },
                            valueRange = 0f..100f,
                            activeColor = accentColor,
                            inactiveColor = separatorColor,
                            backdrop = null,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Hex text field (#RRGGBB)
                        OutlinedTextField(
                            value = hexText,
                            onValueChange = { raw ->
                                val cleaned = raw.uppercase().filter { it in "0123456789ABCDEF#" }
                                val normalized = if (cleaned.startsWith("#")) {
                                    "#" + cleaned.drop(1).take(6)
                                } else {
                                    "#" + cleaned.take(6)
                                }
                                hexText = normalized
                                if (normalized.length == 7) {
                                    runCatching {
                                        val parsed = Color(android.graphics.Color.parseColor(normalized))
                                        val (h, s, v) = colorToHsvTriple(parsed)
                                        hue = h
                                        saturation = s
                                        brightness = v
                                        exactColorOverride = parsed
                                        isResetToDefault = false
                                    }
                                }
                            },
                            label = { Text("Hex (#RRGGBB)") },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                color = labelColor,
                            ),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Characters,
                                imeAction = ImeAction.Done,
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = separatorColor,
                                focusedLabelColor = accentColor,
                                unfocusedLabelColor = secondaryLabelColor,
                                cursorColor = accentColor,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("color_picker_hex_input"),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Bottom row: "Reset" (sets the theme default colour value), then "Done" in accent (saves)
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
                            updateFromColor(defaultColor, isReset = true)
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("color_picker_reset"),
                )

                Text(
                    text = "Done",
                    color = accentColor,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            onDone(currentColor, isResetToDefault)
                            onDismiss()
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("color_picker_done"),
                )
            }
        }
    }
}
