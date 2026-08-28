package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import com.example.ui.AppThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Forwarding function to single source of truth in Color.kt
 */
fun getSectionAccentColor(
    section: String,
    customPaletteColor: Color? = null,
    defaultColor: Color = Color(0xFF10B981)
): Color = com.example.ui.theme.getSectionAccentColor(section, customPaletteColor, defaultColor)

/**
 * Standard keyboard options for text fields that expect natural word capitalization.
 * Sends TYPE_TEXT_FLAG_CAP_WORDS to Android IMEs (Gboard, Samsung Keyboard, etc.)
 */
val AppDefaultWordKeyboardOptions = KeyboardOptions(
    capitalization = KeyboardCapitalization.Words,
    autoCorrectEnabled = true,
    keyboardType = KeyboardType.Text
)

/**
 * Pure, non-destructive natural word capitalization.
 * Capitalizes the first character of each word (following whitespace, start of string)
 * without altering length, erasing characters, or shifting cursor offsets.
 */
fun capitalizeWordsNaturally(input: String): String {
    if (input.isEmpty()) return input
    val chars = input.toCharArray()
    var capitalizeNext = true
    for (i in chars.indices) {
        val c = chars[i]
        if (c.isWhitespace()) {
            capitalizeNext = true
        } else if (capitalizeNext && c.isLetter()) {
            chars[i] = c.uppercaseChar()
            capitalizeNext = false
        } else {
            capitalizeNext = false
        }
    }
    return String(chars)
}

fun capitalizeWordsNaturally(value: TextFieldValue): TextFieldValue {
    val capitalizedText = capitalizeWordsNaturally(value.text)
    return if (capitalizedText == value.text) {
        value
    } else {
        value.copy(text = capitalizedText)
    }
}

/**
 * Reusable application-wide OutlinedTextField that defaults to natural word capitalization
 * and elevated styling across all themes.
 */
@Composable
fun AppOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = AppDefaultWordKeyboardOptions,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = RoundedCornerShape(16.dp),
    colors: TextFieldColors = elevatedInputFieldColors(),
    autoCapitalizeWords: Boolean = true
) {
    val effectiveOnValueChange: (String) -> Unit = remember(onValueChange, keyboardOptions, autoCapitalizeWords) {
        { raw ->
            val shouldAutoCapitalize = autoCapitalizeWords &&
                keyboardOptions.capitalization == KeyboardCapitalization.Words &&
                (keyboardOptions.keyboardType == KeyboardType.Text || keyboardOptions.keyboardType == KeyboardType.Unspecified)
            if (shouldAutoCapitalize) {
                onValueChange(capitalizeWordsNaturally(raw))
            } else {
                onValueChange(raw)
            }
        }
    }

    OutlinedTextField(
        value = value,
        onValueChange = effectiveOnValueChange,
        modifier = modifier.boundedFormFieldRipple(
            shape = shape,
            accentColor = MaterialTheme.colorScheme.primary,
            interactionSource = interactionSource,
            enabled = enabled
        ),
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        prefix = prefix,
        suffix = suffix,
        supportingText = supportingText,
        isError = isError,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        interactionSource = interactionSource,
        shape = shape,
        colors = colors
    )
}

/**
 * TextFieldValue overload of AppOutlinedTextField
 */
@Composable
fun AppOutlinedTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = AppDefaultWordKeyboardOptions,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = RoundedCornerShape(16.dp),
    colors: TextFieldColors = elevatedInputFieldColors(),
    autoCapitalizeWords: Boolean = true
) {
    val effectiveOnValueChange: (TextFieldValue) -> Unit = remember(onValueChange, keyboardOptions, autoCapitalizeWords) {
        { raw ->
            val shouldAutoCapitalize = autoCapitalizeWords &&
                keyboardOptions.capitalization == KeyboardCapitalization.Words &&
                (keyboardOptions.keyboardType == KeyboardType.Text || keyboardOptions.keyboardType == KeyboardType.Unspecified)
            if (shouldAutoCapitalize) {
                onValueChange(capitalizeWordsNaturally(raw))
            } else {
                onValueChange(raw)
            }
        }
    }

    OutlinedTextField(
        value = value,
        onValueChange = effectiveOnValueChange,
        modifier = modifier.boundedFormFieldRipple(
            shape = shape,
            accentColor = MaterialTheme.colorScheme.primary,
            interactionSource = interactionSource,
            enabled = enabled
        ),
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        prefix = prefix,
        suffix = suffix,
        supportingText = supportingText,
        isError = isError,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        interactionSource = interactionSource,
        shape = shape,
        colors = colors
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.bringIntoViewOnFocus(): Modifier {
    val scope = rememberCoroutineScope()
    val requester = remember { BringIntoViewRequester() }
    return this
        .bringIntoViewRequester(requester)
        .onFocusEvent { focusState ->
            if (focusState.isFocused) {
                scope.launch {
                    requester.bringIntoView()
                }
            }
        }
}

@Composable
fun isAppInDarkMode(): Boolean {
    return MaterialTheme.colorScheme.surface.luminance() < 0.5f
}

/**
 * Interactive water-ripple wave effect on glass.
 * When tapped/clicked, triggers a fluid water-droplet impact expanding from the exact
 * horizontal center toward both the left and right edges simultaneously.
 * The wave is soft, smooth, subtle, and strictly clipped within [shape].
 */
@Composable
fun Modifier.centerWaterRipple(
    shape: Shape = RoundedCornerShape(16.dp),
    accentColor: Color = MaterialTheme.colorScheme.primary,
    interactionSource: MutableInteractionSource? = null,
    enabled: Boolean = true
): Modifier {
    val coroutineScope = rememberCoroutineScope()
    val isDark = isAppInDarkMode()
    val animProgress = remember { Animatable(0f) }

    val triggerWave: () -> Unit = remember(coroutineScope, enabled) {
        {
            if (enabled) {
                coroutineScope.launch {
                    animProgress.snapTo(0f)
                    animProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = 480,
                            easing = CubicBezierEasing(0.18f, 0.70f, 0.20f, 1.0f)
                        )
                    )
                    animProgress.snapTo(0f)
                }
            }
        }
    }

    LaunchedEffect(interactionSource, enabled) {
        if (interactionSource != null && enabled) {
            interactionSource.interactions.collect { interaction ->
                if (interaction is PressInteraction.Press) {
                    triggerWave()
                }
            }
        }
    }

    return this
        .pointerInput(enabled) {
            if (enabled) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.changes.any { it.changedToDown() }) {
                            triggerWave()
                        }
                    }
                }
            }
        }
        .drawWithContent {
            drawContent()

            val progress = animProgress.value
            if (progress > 0f && progress < 1f) {
                val outline = shape.createOutline(size, layoutDirection, this)
                clipPath(
                    path = Path().apply {
                        addOutline(outline)
                    }
                ) {
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f

                    // Max horizontal spread reaches slightly past edges for full wash
                    val maxSpread = (size.width / 2f) * 1.12f
                    val currentSpread = maxSpread * progress

                    val fadeAlpha = ((1f - progress) * (if (isDark) 0.50f else 0.60f)).coerceIn(0f, 1f)

                    // 1. Center droplet impact ring / glow (fades quickly at start)
                    val dropletDecay = (1f - (progress * 2.6f)).coerceIn(0f, 1f)
                    if (dropletDecay > 0f) {
                        val dropRadius = 22.dp.toPx() * (0.8f + progress * 1.6f)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = dropletDecay * (if (isDark) 0.40f else 0.50f)),
                                    accentColor.copy(alpha = dropletDecay * 0.30f),
                                    Color.Transparent
                                ),
                                center = Offset(centerX, centerY),
                                radius = dropRadius
                            ),
                            radius = dropRadius,
                            center = Offset(centerX, centerY)
                        )
                    }

                    // 2. Horizontal water wave spreading left and right simultaneously
                    if (currentSpread > 1f) {
                        val leftEdge = (centerX - currentSpread).coerceAtLeast(0f)
                        val rightEdge = (centerX + currentSpread).coerceAtMost(size.width)
                        val waveWidth = rightEdge - leftEdge

                        if (waveWidth > 0f) {
                            val waveBrush = Brush.horizontalGradient(
                                colorStops = arrayOf(
                                    0.00f to Color.Transparent,
                                    0.07f to Color.White.copy(alpha = fadeAlpha * 0.90f),
                                    0.18f to accentColor.copy(alpha = fadeAlpha * 0.55f),
                                    0.35f to Color.White.copy(alpha = fadeAlpha * 0.20f),
                                    0.50f to accentColor.copy(alpha = fadeAlpha * 0.12f),
                                    0.65f to Color.White.copy(alpha = fadeAlpha * 0.20f),
                                    0.82f to accentColor.copy(alpha = fadeAlpha * 0.55f),
                                    0.93f to Color.White.copy(alpha = fadeAlpha * 0.90f),
                                    1.00f to Color.Transparent
                                ),
                                startX = leftEdge,
                                endX = rightEdge
                            )

                            drawRect(
                                brush = waveBrush,
                                topLeft = Offset(leftEdge, 0f),
                                size = Size(waveWidth, size.height)
                            )

                            // Upper specular crest highlight
                            val crestHeight = 3.dp.toPx()
                            val topCrestBrush = Brush.horizontalGradient(
                                colorStops = arrayOf(
                                    0.00f to Color.Transparent,
                                    0.08f to Color.White.copy(alpha = fadeAlpha * 0.85f),
                                    0.50f to Color.White.copy(alpha = fadeAlpha * 0.25f),
                                    0.92f to Color.White.copy(alpha = fadeAlpha * 0.85f),
                                    1.00f to Color.Transparent
                                ),
                                startX = leftEdge,
                                endX = rightEdge
                            )
                            drawRect(
                                brush = topCrestBrush,
                                topLeft = Offset(leftEdge, 0f),
                                size = Size(waveWidth, crestHeight)
                            )
                        }
                    }
                }
            }
        }
}

/**
 * Modifier extension to attach a smooth, center-origin horizontal water ripple effect to form fields.
 * The water wave originates from the horizontal center of the field, expands towards both left and right edges,
 * is contained strictly within [shape], and provides responsive, fluid visual feedback without overflowing.
 * For clickable cards/selectors (onClick != null), applies the centralized glassCardBackground modifier.
 * For text input fields (onClick == null), applies center water ripple and focus handling without outer 3D drop-shadows
 * or outer borders that clash with OutlinedTextField's floating label geometry.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.boundedFormFieldRipple(
    shape: Shape = RoundedCornerShape(16.dp),
    accentColor: Color = MaterialTheme.colorScheme.primary,
    rippleColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null
): Modifier {
    val isDark = isAppInDarkMode()
    val baseMod = this
        .bringIntoViewOnFocus()
        .glassCardBackground(
            isDark = isDark,
            accentColor = accentColor,
            shape = shape
        )
        .centerWaterRipple(
            shape = shape,
            accentColor = accentColor,
            interactionSource = interactionSource,
            enabled = enabled
        )
    return if (onClick != null) {
        baseMod.clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
    } else {
        baseMod
    }
}

/**
 * Underlying canvas shadow drawer for glass containers.
 * Employs dual-layer ambient and directional shadow with corner-radius compensation.
 */
fun Modifier.drawElevatedShadow(
    shape: Shape = RoundedCornerShape(16.dp),
    isDark: Boolean,
    offsetY: Dp = 4.dp,
    blurRadius: Dp = 10.dp,
    elevationAlphaScale: Float = 1.0f
): Modifier = this.drawBehind {
    val ambientAlpha = (if (isDark) 0.22f else 0.08f) * elevationAlphaScale.coerceIn(0.5f, 2.0f)
    val spotAlpha = (if (isDark) 0.36f else 0.12f) * elevationAlphaScale.coerceIn(0.5f, 2.0f)

    val ambientColor = if (isDark) Color.Black.copy(alpha = ambientAlpha) else Color(0xFF0F172A).copy(alpha = ambientAlpha)
    val spotColor = if (isDark) Color.Black.copy(alpha = spotAlpha) else Color(0xFF0F172A).copy(alpha = spotAlpha)

    drawIntoCanvas { canvas ->
        // Ambient soft blur
        val ambientPaint = Paint()
        val ambientFrameworkPaint = ambientPaint.asFrameworkPaint()
        ambientFrameworkPaint.color = ambientColor.toArgb()
        val ambientBlurPx = (blurRadius * 1.3f).toPx()
        if (ambientBlurPx > 0f) {
            ambientFrameworkPaint.maskFilter = android.graphics.BlurMaskFilter(
                ambientBlurPx,
                android.graphics.BlurMaskFilter.Blur.NORMAL
            )
        }
        val outline = shape.createOutline(size, layoutDirection, this)
        canvas.save()
        canvas.translate(0f, 1.5.dp.toPx())
        canvas.drawOutline(outline, ambientPaint)
        canvas.restore()

        // Directional key shadow
        val spotPaint = Paint()
        val spotFrameworkPaint = spotPaint.asFrameworkPaint()
        spotFrameworkPaint.color = spotColor.toArgb()
        val spotBlurPx = blurRadius.toPx()
        if (spotBlurPx > 0f) {
            spotFrameworkPaint.maskFilter = android.graphics.BlurMaskFilter(
                spotBlurPx,
                android.graphics.BlurMaskFilter.Blur.NORMAL
            )
        }
        canvas.save()
        canvas.translate(0f, offsetY.toPx())
        canvas.drawOutline(outline, spotPaint)
        canvas.restore()
    }
}

fun Modifier.elevated3dShadow(
    shape: Shape = RoundedCornerShape(16.dp),
    isDark: Boolean,
    offsetY: Dp = 4.dp,
    blurRadius: Dp = 8.dp
): Modifier = this

@Composable
fun elevatedInputFieldColors(
    isDark: Boolean = isAppInDarkMode(),
    accentColor: Color = MaterialTheme.colorScheme.primary
): TextFieldColors {
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    return OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent,
        errorContainerColor = Color.Transparent,
        focusedTextColor = textPrimary,
        unfocusedTextColor = textPrimary,
        disabledTextColor = textSecondary.copy(alpha = 0.6f),
        errorTextColor = textPrimary,
        focusedBorderColor = accentColor.copy(alpha = 0.85f),
        unfocusedBorderColor = Color.Transparent,
        disabledBorderColor = Color.Transparent,
        errorBorderColor = MaterialTheme.colorScheme.error,
        focusedLabelColor = accentColor,
        unfocusedLabelColor = if (isDark) Color(0xFFCBD5E1) else Color(0xFF64748B),
        disabledLabelColor = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
        errorLabelColor = MaterialTheme.colorScheme.error,
        cursorColor = accentColor,
        errorCursorColor = MaterialTheme.colorScheme.error,
        focusedLeadingIconColor = accentColor,
        unfocusedLeadingIconColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
        disabledLeadingIconColor = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
        errorLeadingIconColor = MaterialTheme.colorScheme.error,
        focusedTrailingIconColor = accentColor,
        unfocusedTrailingIconColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
        disabledTrailingIconColor = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
        errorTrailingIconColor = MaterialTheme.colorScheme.error,
        focusedPlaceholderColor = textSecondary,
        unfocusedPlaceholderColor = textSecondary,
        disabledPlaceholderColor = textSecondary.copy(alpha = 0.5f),
        errorPlaceholderColor = textSecondary,
        focusedPrefixColor = textPrimary,
        unfocusedPrefixColor = textPrimary,
        focusedSuffixColor = textPrimary,
        unfocusedSuffixColor = textPrimary,
        focusedSupportingTextColor = textSecondary,
        unfocusedSupportingTextColor = textSecondary,
        errorSupportingTextColor = MaterialTheme.colorScheme.error
    )
}

/**
 * Reusable Glassmorphism Card Modifier.
 * Provides clear visual separation from background canvas through:
 * 1. Clean frosted translucent surface fill (dense milk-glass optical diffusion in Light Mode, slate in Dark Mode)
 * 2. Multi-layer ambient/spot elevation shadows with floating depth
 * 3. 1.25.dp vertical specular border highlight (stronger top specular white and subtle accent rim)
 * 4. Top specular sheen highlight for authentic glass thickness
 * 5. Distinct boundary contrast in Light, Dark, and AMOLED modes with zero color bleed over text.
 */
@Composable
fun Modifier.glassCardBackground(
    isDark: Boolean = isAppInDarkMode(),
    accentColor: Color = MaterialTheme.colorScheme.primary,
    shape: Shape? = null,
    cornerRadius: Dp? = null,
    themeMode: AppThemeMode? = null,
    elevation: Dp = 6.dp,
    borderWidth: Dp = 1.25.dp,
    flatStyle: Boolean = false
): Modifier {
    val effectiveIsDark = if (themeMode != null) {
        when (themeMode) {
            AppThemeMode.SYSTEM -> isDark
            AppThemeMode.LIGHT -> false
            AppThemeMode.DARK, AppThemeMode.AMOLED -> true
        }
    } else {
        isDark
    }

    val effectiveShape = shape ?: RoundedCornerShape(cornerRadius ?: 16.dp)
    val isAmoled = themeMode == AppThemeMode.AMOLED || (effectiveIsDark && MaterialTheme.colorScheme.background == Color(0xFF000000))

    val containerGradient = remember(effectiveIsDark, isAmoled, accentColor, flatStyle) {
        if (flatStyle) {
            val flatColor = when {
                isAmoled -> Color(0xFF0C0A09).copy(alpha = 0.96f)
                effectiveIsDark -> Color(0xFF162032).copy(alpha = 0.95f)
                else -> Color(0xFFF8FAFC).copy(alpha = 0.95f)
            }
            Brush.linearGradient(listOf(flatColor, flatColor))
        } else {
            when {
                isAmoled -> Brush.verticalGradient(
                    0.0f to Color(0xFF1C1917).copy(alpha = 0.98f),
                    0.30f to Color(0xFF0C0A09).copy(alpha = 0.95f),
                    0.70f to accentColor.copy(alpha = 0.12f),
                    1.0f to Color(0xFF000000).copy(alpha = 0.98f)
                )
                effectiveIsDark -> Brush.verticalGradient(
                    0.0f to Color(0xFF1E293B).copy(alpha = 0.96f),
                    0.25f to Color(0xFF162032).copy(alpha = 0.93f),
                    0.60f to accentColor.copy(alpha = 0.09f),
                    1.0f to Color(0xFF0F172A).copy(alpha = 0.97f)
                )
                else -> Brush.verticalGradient(
                    0.0f to Color.White.copy(alpha = 0.97f),
                    0.20f to Color(0xFFF8FAFC).copy(alpha = 0.94f),
                    0.55f to Color.White.copy(alpha = 0.93f),
                    0.80f to accentColor.copy(alpha = 0.06f),
                    1.0f to Color(0xFFF1F5F9).copy(alpha = 0.96f)
                )
            }
        }
    }

    val specularBorderBrush = remember(effectiveIsDark, isAmoled, accentColor) {
        when {
            isAmoled -> Brush.verticalGradient(
                0.0f to Color.White.copy(alpha = 0.80f),
                0.25f to accentColor.copy(alpha = 0.65f),
                0.60f to Color(0xFF27272A).copy(alpha = 0.80f),
                1.0f to Color(0xFF3F3F46).copy(alpha = 0.50f)
            )
            effectiveIsDark -> Brush.verticalGradient(
                0.0f to Color.White.copy(alpha = 0.70f),
                0.25f to accentColor.copy(alpha = 0.55f),
                0.60f to Color(0xFF334155).copy(alpha = 0.65f),
                1.0f to Color.White.copy(alpha = 0.30f)
            )
            else -> Brush.verticalGradient(
                0.0f to Color.White.copy(alpha = 0.98f),
                0.25f to accentColor.copy(alpha = 0.50f),
                0.60f to Color(0xFFCBD5E1).copy(alpha = 0.65f),
                1.0f to Color.White.copy(alpha = 0.80f)
            )
        }
    }

    val sheenBrush = remember(effectiveIsDark) {
        Brush.verticalGradient(
            0.0f to (if (effectiveIsDark) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.35f)),
            0.20f to (if (effectiveIsDark) Color.White.copy(alpha = 0.03f) else Color.White.copy(alpha = 0.08f)),
            1.0f to Color.Transparent
        )
    }

    return this
        .drawElevatedShadow(
            shape = effectiveShape,
            isDark = effectiveIsDark,
            offsetY = (elevation * 0.55f).coerceAtLeast(3.dp),
            blurRadius = (elevation * 1.4f).coerceAtLeast(8.dp),
            elevationAlphaScale = if (elevation > 8.dp) 1.5f else 1.0f
        )
        .clip(effectiveShape)
        .background(containerGradient)
        .drawBehind {
            // Draw top specular sheen for authentic optical glass refraction
            drawRect(brush = sheenBrush)
        }
        .border(
            width = borderWidth,
            brush = specularBorderBrush,
            shape = effectiveShape
        )
}

