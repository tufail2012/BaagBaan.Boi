package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
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
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
    defaultColor: Color = com.example.ui.theme.AgriRedPrimary
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
    shape: Shape = RoundedCornerShape(14.dp),
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
    shape: Shape = RoundedCornerShape(14.dp),
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
 * Clean interaction helper for form fields.
 */
@Composable
fun Modifier.centerWaterRipple(
    shape: Shape = RoundedCornerShape(14.dp),
    accentColor: Color = MaterialTheme.colorScheme.primary,
    interactionSource: MutableInteractionSource? = null,
    enabled: Boolean = true
): Modifier = this

/**
 * Modifier extension to apply clean frosted glass background and focus handling to form fields.
 * Maintains a clean, semi-transparent frosted glass backdrop with uniform opacity.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.boundedFormFieldRipple(
    shape: Shape = RoundedCornerShape(14.dp),
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
            isDark = isDark,
            accentColor = accentColor,
            shape = shape,
            isFocused = effectiveIsFocused
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
 * Flat elevation helper (zero unwanted grey shadows or top shades).
 */
fun Modifier.drawElevatedShadow(
    shape: Shape = RoundedCornerShape(14.dp),
    isDark: Boolean = false,
    offsetY: Dp = 0.dp,
    blurRadius: Dp = 0.dp,
    elevationAlphaScale: Float = 1.0f
): Modifier = this

fun Modifier.elevated3dShadow(
    shape: Shape = RoundedCornerShape(14.dp),
    isDark: Boolean = false,
    offsetY: Dp = 0.dp,
    blurRadius: Dp = 0.dp
): Modifier = this

@Composable
fun elevatedInputFieldColors(
    isDark: Boolean = isAppInDarkMode(),
    accentColor: Color = MaterialTheme.colorScheme.primary
): TextFieldColors {
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val labelUnfocused = if (isDark) Color(0xFFCBD5E1) else Color(0xFF64748B)

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
        unfocusedLabelColor = labelUnfocused,
        disabledLabelColor = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
        errorLabelColor = MaterialTheme.colorScheme.error,
        cursorColor = accentColor,
        errorCursorColor = MaterialTheme.colorScheme.error,
        focusedLeadingIconColor = accentColor,
        unfocusedLeadingIconColor = accentColor,
        disabledLeadingIconColor = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
        errorLeadingIconColor = MaterialTheme.colorScheme.error,
        focusedTrailingIconColor = accentColor,
        unfocusedTrailingIconColor = accentColor,
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
 * Clean Backdrop Modifier for Input Fields, Selectors, and Cards.
 * In Dark Mode, provides a rich glossy dark bevel card with top specular highlight,
 * perfectly matching the reference screenshot.
 */
@Composable
fun Modifier.glassCardBackground(
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
    val effectiveShape = shape ?: RoundedCornerShape(cornerRadius ?: 22.dp)

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

    val glowAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0f,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "inputFieldGlow"
    )

    val effectiveElevation = if (elevation > 0.dp) {
        elevation + (4.dp * glowAlpha)
    } else {
        4.dp * glowAlpha
    }

    val glowShadowModifier = if (effectiveElevation > 0.dp) {
        Modifier.shadow(
            elevation = effectiveElevation,
            shape = effectiveShape,
            spotColor = if (isFocused || glowAlpha > 0.05f) {
                accentColor.copy(alpha = (if (effectiveIsDark) 0.50f else 0.35f) * glowAlpha)
            } else {
                Color.Black.copy(alpha = if (effectiveIsDark) 0.50f else 0.10f)
            },
            ambientColor = if (isFocused || glowAlpha > 0.05f) {
                accentColor.copy(alpha = 0.25f * glowAlpha)
            } else {
                Color.Black.copy(alpha = if (effectiveIsDark) 0.30f else 0.05f)
            }
        )
    } else {
        Modifier
    }

    val glowAuraModifier = if (glowAlpha > 0.01f) {
        Modifier.drawBehind {
            val outline = effectiveShape.createOutline(size, layoutDirection, this)
            // Outer diffuse subtle glow rings using current accent color
            drawOutline(
                outline = outline,
                color = accentColor.copy(alpha = (if (effectiveIsDark) 0.32f else 0.22f) * glowAlpha),
                style = Stroke(width = 3.5.dp.toPx())
            )
            drawOutline(
                outline = outline,
                color = accentColor.copy(alpha = (if (effectiveIsDark) 0.14f else 0.09f) * glowAlpha),
                style = Stroke(width = 7.dp.toPx())
            )
        }
    } else {
        Modifier
    }

    val hazeState = LocalAppGlassHazeState.current

    val hazeStyle = remember(effectiveIsDark, effectiveIsAmoled) {
        HazeStyle(
            backgroundColor = Color.Transparent,
            blurRadius = 24.dp,
            tints = listOf(
                HazeTint(
                    color = if (effectiveIsAmoled) Color.Black.copy(alpha = 0.10f)
                    else if (effectiveIsDark) Color(0xFF14121B).copy(alpha = 0.12f)
                    else Color.White.copy(alpha = 0.06f)
                )
            ),
            noiseFactor = 0f
        )
    }

    val cardBgBrush = Brush.verticalGradient(
        colors = if (effectiveIsDark) {
            if (effectiveIsAmoled) {
                listOf(
                    Color(0xFF161418).copy(alpha = 0.88f),
                    Color(0xFF0C0B0E).copy(alpha = 0.82f),
                    Color(0xFF000000).copy(alpha = 0.88f)
                )
            } else {
                listOf(
                    Color(0xFF2C2834).copy(alpha = 0.82f),
                    Color(0xFF221F2A).copy(alpha = 0.76f),
                    Color(0xFF191720).copy(alpha = 0.82f)
                )
            }
        } else {
            listOf(
                Color.White.copy(alpha = 0.78f),
                Color.White.copy(alpha = 0.62f),
                Color.White.copy(alpha = 0.72f)
            )
        }
    )

    val borderStroke = if (isFocused || glowAlpha > 0.05f) {
        BorderStroke(1.5.dp, accentColor)
    } else {
        val borderBrush = Brush.verticalGradient(
            colors = if (effectiveIsDark) {
                listOf(
                    Color.White.copy(alpha = if (effectiveIsAmoled) 0.35f else 0.40f),
                    Color.White.copy(alpha = if (effectiveIsAmoled) 0.12f else 0.15f),
                    Color.White.copy(alpha = if (effectiveIsAmoled) 0.04f else 0.08f)
                )
            } else {
                listOf(
                    Color.White.copy(alpha = 0.65f),
                    Color.White.copy(alpha = 0.25f),
                    Color.White.copy(alpha = 0.15f)
                )
            }
        )
        BorderStroke(borderWidth, borderBrush)
    }

    return this
        .then(glowShadowModifier)
        .then(glowAuraModifier)
        .then(
            if (hazeState != null) {
                Modifier.hazeEffect(state = hazeState, style = hazeStyle)
            } else {
                Modifier
            }
        )
        .clip(effectiveShape)
        .background(brush = cardBgBrush, shape = effectiveShape)
        .drawWithContent {
            drawContent()
            if (!isFocused) {
                val w = size.width
                val highlightH = 1.2.dp.toPx()
                val margin = 14.dp.toPx()
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = if (effectiveIsDark) 0.25f else 0.35f),
                            Color.Transparent
                        ),
                        startX = margin,
                        endX = w - margin
                    ),
                    topLeft = Offset(margin, 0.5.dp.toPx()),
                    size = Size(w - (margin * 2), highlightH)
                )
            }
        }
        .border(
            border = borderStroke,
            shape = effectiveShape
        )
}

/**
 * Modern Payment Status Button Row.
 * Ensures the selected button maintains the dynamic theme color (or section accent color),
 * with glowing border, elevated shadow, and high-contrast text that is always clearly visible.
 */
@Composable
fun PaymentStatusSelector(
    selectedStatus: String,
    onStatusSelected: (String) -> Unit,
    accentColor: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    testTagPrefix: String = "payment_status"
) {
    val paymentStatusOptions = listOf("Pending", "Advance Paid", "Fully Paid")
    
    // Calculate high contrast text color for the selected theme color
    val isLightAccent = (0.299f * accentColor.red + 0.587f * accentColor.green + 0.114f * accentColor.blue) > 0.62f
    val selectedTextColor = if (isLightAccent) Color(0xFF0F172A) else Color.White
    val selectedIconColor = selectedTextColor

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        paymentStatusOptions.forEach { statusOption ->
            val isSelected = selectedStatus.equals(statusOption, ignoreCase = true)
            
            val buttonShape = RoundedCornerShape(24.dp)
            
            // Container surface brush & border
            val backgroundBrush = if (isSelected) {
                Brush.verticalGradient(
                    colors = listOf(
                        accentColor,
                        accentColor.copy(alpha = 0.92f),
                        Color(
                            red = (accentColor.red * 0.6f).coerceIn(0f, 1f),
                            green = (accentColor.green * 0.6f).coerceIn(0f, 1f),
                            blue = (accentColor.blue * 0.6f).coerceIn(0f, 1f),
                            alpha = 1f
                        )
                    )
                )
            } else {
                Brush.verticalGradient(
                    colors = if (isDark) {
                        listOf(
                            Color(0xFF242127),
                            Color(0xFF151318)
                        )
                    } else {
                        listOf(
                            Color.White.copy(alpha = 0.95f),
                            Color(0xFFF1F5F9).copy(alpha = 0.85f)
                        )
                    }
                )
            }

            val borderStroke = if (isSelected) {
                BorderStroke(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (isDark) 0.70f else 0.90f),
                            accentColor,
                            accentColor.copy(alpha = 0.80f)
                        )
                    )
                )
            } else {
                BorderStroke(
                    width = 1.dp,
                    color = if (isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFCBD5E1)
                )
            }

            val textColor = if (isSelected) {
                selectedTextColor
            } else {
                if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B)
            }

            Surface(
                shape = buttonShape,
                color = Color.Transparent,
                border = borderStroke,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .shadow(
                        elevation = if (isSelected) 4.dp else 1.dp,
                        shape = buttonShape,
                        spotColor = if (isSelected) accentColor.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.10f),
                        ambientColor = if (isSelected) accentColor.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.05f)
                    )
                    .clip(buttonShape)
                    .background(brush = backgroundBrush, shape = buttonShape)
                    .clickable { onStatusSelected(statusOption) }
                    .testTag("${testTagPrefix}_$statusOption")
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isSelected) {
                            val icon = when (statusOption) {
                                "Fully Paid" -> Icons.Default.CheckCircle
                                "Advance Paid" -> Icons.Default.AccountBalanceWallet
                                else -> Icons.Default.Lock
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = "$statusOption selected",
                                tint = selectedIconColor,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Text(
                            text = statusOption,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            color = textColor,
                            maxLines = 1,
                            letterSpacing = 0.2.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Persistent 3D Fluid Liquid Pill Indicator Modifier.
 * Features:
 * - True translucent liquid glass highlight (NOT an opaque solid pill).
 * - Persistent 3D look using graphicsLayer (subtle rotationX, slight scale, and elevation/drop shadow).
 * - Embossed physical depth with top specular meniscus reflection and subtle bottom bevel shadow.
 * - Soft liquid/glass glow layer with clear color diffusion for high contrast in both Dark and Light modes.
 * - Static while active with zero continuous looping animations.
 */
/**
 * 3D Dimensional "Bubbly Glass" Capsule (Liquid Capsule) Modifier.
 * Specifications:
 * - Full pill shape.
 * - Surface Gradient:
 *   linear-gradient(180deg, rgba(255, 255, 255, 0.85) 0%, rgba(255, 255, 255, 0.4) 60%, rgba(var(--theme-primary-rgb), 0.15) 100%)
 * - Top Specular Highlight & Depth (Inner Glow):
 *   inset 0 1.5px 2px 0 rgba(255, 255, 255, 0.95), /* Top light reflection */
 *   inset 0 -2px 3px 0 rgba(0, 0, 0, 0.04),         /* Bottom curvature shading */
 *   0 4px 12px 0 rgba(var(--theme-primary-rgb), 0.12) /* Ambient colored drop shadow */
 * - Border: 1px solid rgba(255, 255, 255, 0.7)
 * - Blur: backdrop-filter: blur(14px)
 */
@Composable
fun Modifier.bubbleDropletPillIndicator(
    hazeState: HazeState? = null,
    shape: Shape = RoundedCornerShape(percent = 50),
    accentColor: Color = MaterialTheme.colorScheme.primary,
    isDark: Boolean = isAppInDarkMode(),
    isAmoled: Boolean = isAppInAmoledMode()
): Modifier {
    val surfaceGradient = if (isDark || isAmoled) {
        Brush.verticalGradient(
            colorStops = arrayOf(
                0.0f to Color.White.copy(alpha = 0.35f),
                0.60f to Color.White.copy(alpha = 0.18f),
                1.0f to accentColor.copy(alpha = 0.22f)
            )
        )
    } else {
        // Light mode: linear-gradient(180deg, rgba(255, 255, 255, 0.85) 0%, rgba(255, 255, 255, 0.4) 60%, rgba(var(--theme-primary-rgb), 0.15) 100%)
        Brush.verticalGradient(
            colorStops = arrayOf(
                0.0f to Color.White.copy(alpha = 0.85f),
                0.60f to Color.White.copy(alpha = 0.40f),
                1.0f to accentColor.copy(alpha = 0.15f)
            )
        )
    }

    // Border: 1px solid rgba(255, 255, 255, 0.7)
    val bubblyBorderBrush = if (isDark || isAmoled) {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.70f),
                accentColor.copy(alpha = 0.35f),
                Color.White.copy(alpha = 0.25f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.85f),
                Color.White.copy(alpha = 0.70f),
                accentColor.copy(alpha = 0.25f)
            )
        )
    }

    return this
        // 0 4px 12px 0 rgba(var(--theme-primary-rgb), 0.12)
        .shadow(
            elevation = 4.dp,
            shape = shape,
            spotColor = accentColor.copy(alpha = if (isDark || isAmoled) 0.24f else 0.16f),
            ambientColor = accentColor.copy(alpha = if (isDark || isAmoled) 0.14f else 0.10f)
        )
        .clip(shape)
        // backdrop-filter: blur(14px)
        .then(
            if (hazeState != null) {
                Modifier.hazeEffect(
                    state = hazeState,
                    style = HazeStyle(
                        blurRadius = 14.dp,
                        tints = listOf(
                            HazeTint(color = accentColor.copy(alpha = if (isDark) 0.08f else 0.05f))
                        ),
                        backgroundColor = Color.Transparent
                    )
                )
            } else Modifier
        )
        // Surface Gradient
        .background(
            brush = surfaceGradient,
            shape = shape
        )
        // Top Specular Highlight & Bottom Depth (Inner Glow):
        // inset 0 1.5px 2px 0 rgba(255, 255, 255, 0.95)
        // inset 0 -2px 3px 0 rgba(0, 0, 0, 0.04)
        .drawWithContent {
            drawContent()
            val w = size.width
            val h = size.height
            val cornerRadius = CornerRadius(h / 2f, h / 2f)

            // Top specular light reflection
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isDark || isAmoled) 0.85f else 0.95f),
                        Color.White.copy(alpha = if (isDark || isAmoled) 0.35f else 0.50f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = h * 0.55f
                ),
                topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                size = Size(w - 2.dp.toPx(), h - 2.dp.toPx()),
                cornerRadius = cornerRadius,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Bottom curvature shading
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = if (isDark || isAmoled) 0.10f else 0.04f)
                    ),
                    startY = h * 0.50f,
                    endY = h
                ),
                topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                size = Size(w - 2.dp.toPx(), h - 2.dp.toPx()),
                cornerRadius = cornerRadius,
                style = Stroke(width = 2.dp.toPx())
            )
        }
        // Border: 1px solid rgba(255, 255, 255, 0.7)
        .border(
            width = 1.dp,
            brush = bubblyBorderBrush,
            shape = shape
        )
}

/**
 * Frosted Liquid Glass Detail Card Modifier
 * Directly follows specifications:
 * - background: linear-gradient(180deg, rgba(255, 255, 255, 0.65) 0%, rgba(255, 255, 255, 0.35) 100%)
 * - backdrop-filter: blur(16px)
 * - border: 1px solid rgba(255, 255, 255, 0.65)
 * - border-radius: 22px
 * - box-shadow: inset 0 1px 1.5px rgba(255, 255, 255, 0.9), 0 6px 20px rgba(0, 0, 0, 0.04)
 */
@Composable
fun Modifier.frostedLiquidGlassDetailCard(
    isDark: Boolean = isAppInDarkMode(),
    accentColor: Color = MaterialTheme.colorScheme.primary,
    shape: Shape = RoundedCornerShape(22.dp),
    hazeState: HazeState? = null,
    cornerRadius: Dp = 22.dp
): Modifier {
    val effectiveShape = shape

    val cardBackgroundBrush = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1E293B).copy(alpha = 0.92f),
                accentColor.copy(alpha = 0.08f),
                Color(0xFF0F172A).copy(alpha = 0.90f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFFFFF).copy(alpha = 0.94f),
                accentColor.copy(alpha = 0.03f),
                Color(0xFFFFFFFF).copy(alpha = 0.90f)
            )
        )
    }

    val borderBrush = Brush.verticalGradient(
        colors = if (isDark) {
            listOf(
                Color.White.copy(alpha = 0.40f),
                accentColor.copy(alpha = 0.25f),
                Color.White.copy(alpha = 0.15f)
            )
        } else {
            listOf(
                Color.White.copy(alpha = 0.85f),
                Color.White.copy(alpha = 0.70f),
                accentColor.copy(alpha = 0.20f)
            )
        }
    )

    return this
        // 0 6px 20px rgba(0, 0, 0, 0.04)
        .shadow(
            elevation = 4.dp,
            shape = effectiveShape,
            spotColor = Color.Black.copy(alpha = if (isDark) 0.16f else 0.04f),
            ambientColor = Color.Black.copy(alpha = if (isDark) 0.08f else 0.02f)
        )
        .clip(effectiveShape)
        // backdrop-filter: blur(16px)
        .then(
            if (hazeState != null) {
                Modifier.hazeEffect(
                    state = hazeState,
                    style = HazeStyle(
                        blurRadius = 16.dp,
                        tints = listOf(
                            HazeTint(color = accentColor.copy(alpha = if (isDark) 0.06f else 0.03f))
                        ),
                        backgroundColor = Color.Transparent
                    )
                )
            } else Modifier
        )
        .background(
            brush = cardBackgroundBrush,
            shape = effectiveShape
        )
        // inset 0 1px 1.5px rgba(255, 255, 255, 0.9) - Upper specular reflection
        .drawWithContent {
            drawContent()
            val w = size.width
            val h = size.height
            val crPx = cornerRadius.toPx()
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isDark) 0.50f else 0.90f),
                        Color.White.copy(alpha = if (isDark) 0.15f else 0.30f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = 16.dp.toPx()
                ),
                topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                size = Size(w - 2.dp.toPx(), h - 2.dp.toPx()),
                cornerRadius = CornerRadius(crPx, crPx),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }
        // border: 1px solid rgba(255, 255, 255, 0.65)
        .border(
            width = 1.dp,
            brush = borderBrush,
            shape = effectiveShape
        )
}

/**
 * Frosted Circle Action Button
 * width: 38px; height: 38px; border-radius: 50%; background: rgba(255, 255, 255, 0.5); backdrop-filter: blur(10px);
 */
@Composable
fun FrostedCircleActionButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color,
    modifier: Modifier = Modifier,
    isDark: Boolean = isAppInDarkMode(),
    hazeState: HazeState? = null,
    testTag: String? = null
) {
    Box(
        modifier = modifier
            .size(38.dp)
            .shadow(
                elevation = 2.dp,
                shape = CircleShape,
                spotColor = Color.Black.copy(alpha = if (isDark) 0.18f else 0.05f),
                ambientColor = Color.Black.copy(alpha = if (isDark) 0.08f else 0.02f)
            )
            .clip(CircleShape)
            .then(
                if (hazeState != null) {
                    Modifier.hazeEffect(
                        state = hazeState,
                        style = HazeStyle(
                            blurRadius = 10.dp,
                            tints = listOf(HazeTint(color = tint.copy(alpha = 0.04f))),
                            backgroundColor = Color.Transparent
                        )
                    )
                } else Modifier
            )
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isDark) {
                        listOf(
                            Color.White.copy(alpha = 0.20f),
                            Color.White.copy(alpha = 0.10f)
                        )
                    } else {
                        listOf(
                            Color.White.copy(alpha = 0.65f),
                            Color.White.copy(alpha = 0.45f)
                        )
                    }
                ),
                shape = CircleShape
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isDark) 0.60f else 0.85f),
                        Color.White.copy(alpha = if (isDark) 0.20f else 0.35f)
                    )
                ),
                shape = CircleShape
            )
            .clickable(onClick = onClick)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
    }
}



