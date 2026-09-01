package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.AppThemeMode
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
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
 * Manages per-character animated fade and scale for newly typed characters.
 * Keeps existing characters stable without re-animation.
 */
@Composable
fun rememberTypingAnimationTransformation(
    text: String,
    baseTransformation: VisualTransformation = VisualTransformation.None,
    baseTextColor: Color = if (isAppInDarkMode()) Color.White else Color(0xFF0F172A),
    textStyle: TextStyle = LocalTextStyle.current
): VisualTransformation {
    var previousText by remember { mutableStateOf(text) }
    val animProgress = remember { Animatable(1f) }
    var animatedRange by remember { mutableStateOf<IntRange?>(null) }

    LaunchedEffect(text) {
        if (text != previousText) {
            val oldText = previousText
            previousText = text
            // Detect single or multiple character insertion
            if (text.length > oldText.length) {
                // Find common prefix
                var prefixLen = 0
                while (prefixLen < oldText.length && prefixLen < text.length && oldText[prefixLen] == text[prefixLen]) {
                    prefixLen++
                }
                // Find common suffix
                var suffixOld = oldText.length - 1
                var suffixNew = text.length - 1
                while (suffixOld >= prefixLen && suffixNew >= prefixLen && oldText[suffixOld] == text[suffixNew]) {
                    suffixOld--
                    suffixNew--
                }
                val insertStart = prefixLen
                val insertEnd = suffixNew + 1
                if (insertStart < insertEnd) {
                    animatedRange = insertStart until insertEnd
                    animProgress.snapTo(0f)
                    animProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = 140,
                            easing = FastOutSlowInEasing
                        )
                    )
                    animatedRange = null
                }
            } else {
                // Deletion: instantly update without animation
                animatedRange = null
            }
        }
    }

    val currentProgress = animProgress.value
    val currentRange = animatedRange

    return remember(baseTransformation, baseTextColor, textStyle, currentProgress, currentRange, text) {
        VisualTransformation { originalText ->
            val baseTransformed = baseTransformation.filter(originalText)
            val transformedStr = baseTransformed.text.text

            if (currentRange != null && currentProgress < 1f && transformedStr.isNotEmpty()) {
                val safeStart = currentRange.first.coerceIn(0, transformedStr.length)
                val safeEnd = (currentRange.last + 1).coerceIn(safeStart, transformedStr.length)

                if (safeStart < safeEnd) {
                    val builder = AnnotatedString.Builder(transformedStr)
                    val alpha = currentProgress.coerceIn(0f, 1f)
                    val scale = (0.85f + (0.15f * currentProgress)).coerceIn(0.85f, 1f)
                    val baseFontSizeSp = if (textStyle.fontSize.isSpecified) textStyle.fontSize.value else 16f
                    
                    val animatedColor = baseTextColor.copy(alpha = alpha)
                    builder.addStyle(
                        style = SpanStyle(
                            color = animatedColor,
                            fontSize = (baseFontSizeSp * scale).sp
                        ),
                        start = safeStart,
                        end = safeEnd
                    )

                    TransformedText(
                        text = builder.toAnnotatedString(),
                        offsetMapping = baseTransformed.offsetMapping
                    )
                } else {
                    baseTransformed
                }
            } else {
                baseTransformed
            }
        }
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
    hazeState: Any? = null,
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
    accentColor: Color = MaterialTheme.colorScheme.primary,
    colors: TextFieldColors = elevatedInputFieldColors(accentColor = accentColor),
    autoCapitalizeWords: Boolean = true
) {
    val isDark = isAppInDarkMode()
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val animatedTransformation = rememberTypingAnimationTransformation(
        text = value,
        baseTransformation = visualTransformation,
        baseTextColor = textPrimary,
        textStyle = textStyle
    )

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
            hazeState = hazeState,
            shape = shape,
            accentColor = accentColor,
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
        visualTransformation = animatedTransformation,
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
    hazeState: Any? = null,
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
    accentColor: Color = MaterialTheme.colorScheme.primary,
    colors: TextFieldColors = elevatedInputFieldColors(accentColor = accentColor),
    autoCapitalizeWords: Boolean = true
) {
    val isDark = isAppInDarkMode()
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val animatedTransformation = rememberTypingAnimationTransformation(
        text = value.text,
        baseTransformation = visualTransformation,
        baseTextColor = textPrimary,
        textStyle = textStyle
    )

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
            hazeState = hazeState,
            shape = shape,
            accentColor = accentColor,
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
        visualTransformation = animatedTransformation,
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
    return MaterialTheme.colorScheme.surface.luminance() < 0.5f || MaterialTheme.colorScheme.background.luminance() < 0.5f
}

@Composable
fun isAppInAmoledMode(): Boolean {
    val bg = MaterialTheme.colorScheme.background
    val surface = MaterialTheme.colorScheme.surface
    return bg == Color.Black || bg == Color(0xFF000000) || (surface.luminance() < 0.04f && bg.luminance() < 0.04f)
}

/**
 * Clean water-ripple interaction effect for form fields.
 * Subtle, soft accent color wave with zero specular glare, zero white crests, and zero glass strips.
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
                            durationMillis = 400,
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

                    val maxSpread = (size.width / 2f) * 1.12f
                    val currentSpread = maxSpread * progress
                    val fadeAlpha = ((1f - progress) * (if (isDark) 0.30f else 0.20f)).coerceIn(0f, 1f)

                    // 1. Center droplet impact glow (subtle accent tone, zero white glare)
                    val dropletDecay = (1f - (progress * 2.6f)).coerceIn(0f, 1f)
                    if (dropletDecay > 0f) {
                        val dropRadius = 20.dp.toPx() * (0.8f + progress * 1.4f)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = dropletDecay * 0.20f),
                                    accentColor.copy(alpha = dropletDecay * 0.08f),
                                    Color.Transparent
                                ),
                                center = Offset(centerX, centerY),
                                radius = dropRadius
                            ),
                            radius = dropRadius,
                            center = Offset(centerX, centerY)
                        )
                    }

                    // 2. Horizontal soft accent wave spreading left and right (zero white strips, zero glass reflection)
                    if (currentSpread > 1f) {
                        val leftEdge = (centerX - currentSpread).coerceAtLeast(0f)
                        val rightEdge = (centerX + currentSpread).coerceAtMost(size.width)
                        val waveWidth = rightEdge - leftEdge

                        if (waveWidth > 0f) {
                            val waveBrush = Brush.horizontalGradient(
                                colorStops = arrayOf(
                                    0.00f to Color.Transparent,
                                    0.25f to accentColor.copy(alpha = fadeAlpha * 0.45f),
                                    0.50f to accentColor.copy(alpha = fadeAlpha * 0.15f),
                                    0.75f to accentColor.copy(alpha = fadeAlpha * 0.45f),
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
                        }
                    }
                }
            }
        }
}

/**
 * Modifier extension to attach a smooth, center-origin horizontal water ripple effect to form fields.
 * For clickable cards/selectors (onClick != null), applies the centralized glassCardBackground modifier.
 * For text input fields, applies clean background and focus handling with zero glass glare.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.boundedFormFieldRipple(
    hazeState: Any? = null,
    shape: Shape = RoundedCornerShape(16.dp),
    accentColor: Color = MaterialTheme.colorScheme.primary,
    rippleColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null
): Modifier {
    val isDark = isAppInDarkMode()
    val isInteractionFocused by interactionSource.collectIsFocusedAsState()
    var isEventFocused by remember { mutableStateOf(false) }
    val effectiveIsFocused = isInteractionFocused || isEventFocused

    val baseMod = this
        .bringIntoViewOnFocus()
        .onFocusEvent { focusState ->
            isEventFocused = focusState.isFocused || focusState.hasFocus
        }
        .glassCardBackground(
            hazeState = hazeState,
            isDark = isDark,
            accentColor = accentColor,
            shape = shape,
            isFocused = effectiveIsFocused
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
 * Elevation shadow helper.
 */
fun Modifier.drawElevatedShadow(
    shape: Shape = RoundedCornerShape(16.dp),
    isDark: Boolean,
    offsetY: Dp = 2.dp,
    blurRadius: Dp = 6.dp,
    elevationAlphaScale: Float = 1.0f
): Modifier = this

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
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)

    return OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent,
        errorContainerColor = Color.Transparent,
        focusedTextColor = textPrimary,
        unfocusedTextColor = textPrimary,
        disabledTextColor = textSecondary.copy(alpha = 0.6f),
        errorTextColor = textPrimary,
        focusedBorderColor = Color.Transparent,
        unfocusedBorderColor = Color.Transparent,
        disabledBorderColor = Color.Transparent,
        errorBorderColor = MaterialTheme.colorScheme.error,
        focusedLabelColor = accentColor,
        unfocusedLabelColor = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
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
 * Solid Background Modifier for Input Fields, Selectors, and Cards.
 * Applied across all forms and text fields:
 * - Solid pure white (#FFFFFF) in Light mode
 * - Solid dark color (#1E293B) in Dark mode
 * - Solid pure black (#000000) in AMOLED mode
 * - Clean crisp border with zero glass sheen and zero inner white strips.
 */
@Composable
fun Modifier.glassCardBackground(
    hazeState: Any? = null,
    isDark: Boolean = isAppInDarkMode(),
    accentColor: Color = MaterialTheme.colorScheme.primary,
    shape: Shape? = null,
    cornerRadius: Dp? = null,
    themeMode: AppThemeMode? = null,
    elevation: Dp = 0.dp,
    borderWidth: Dp = 1.dp,
    flatStyle: Boolean = true,
    isFocused: Boolean = false
): Modifier {
    val effectiveShape = shape ?: RoundedCornerShape(cornerRadius ?: 16.dp)

    val effectiveIsAmoled = when (themeMode) {
        AppThemeMode.AMOLED -> true
        AppThemeMode.DARK, AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM, null -> isAppInAmoledMode()
    }
    val effectiveIsDark = when (themeMode) {
        AppThemeMode.AMOLED, AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM, null -> isDark || effectiveIsAmoled
    }

    val containerColor = when {
        effectiveIsAmoled -> Color(0xFF000000) // Pure black in AMOLED mode
        effectiveIsDark -> Color(0xFF1E293B)   // Solid dark color in Dark mode
        else -> Color(0xFFFFFFFF)             // Pure white in Light mode
    }

    val borderColor = when {
        isFocused -> accentColor
        effectiveIsAmoled -> Color(0xFF262626)
        effectiveIsDark -> Color(0xFF334155)
        else -> Color(0xFFE2E8F0)
    }

    return this
        .clip(effectiveShape)
        .background(color = containerColor, shape = effectiveShape)
        .border(
            width = if (isFocused) (borderWidth * 1.5f).coerceAtLeast(1.5.dp) else borderWidth,
            color = borderColor,
            shape = effectiveShape
        )
}


