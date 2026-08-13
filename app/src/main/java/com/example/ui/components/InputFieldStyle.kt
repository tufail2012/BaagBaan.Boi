package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

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
 * Modifier extension to attach a smooth, bounded ripple effect animation to form fields.
 * The ripple originates from the point of tap, is contained strictly within [shape],
 * and provides responsive, lag-free visual feedback without overflowing or affecting surrounding UI.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.boundedFormFieldRipple(
    shape: Shape = RoundedCornerShape(16.dp),
    rippleColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null
): Modifier {
    val clipMod = this.bringIntoViewOnFocus().clip(shape)
    return if (onClick != null) {
        clipMod.clickable(
            interactionSource = interactionSource,
            indication = ripple(bounded = true, color = rippleColor),
            enabled = enabled,
            onClick = onClick
        )
    } else {
        clipMod.indication(
            interactionSource = interactionSource,
            indication = ripple(bounded = true, color = rippleColor)
        )
    }
}

fun Modifier.elevated3dShadow(
    shape: Shape = RoundedCornerShape(16.dp),
    isDark: Boolean,
    offsetY: Dp = 4.dp,
    blurRadius: Dp = 8.dp
): Modifier = this.drawBehind {
    val shadowColor = if (isDark) {
        Color.White.copy(alpha = 0.15f)
    } else {
        Color.Black.copy(alpha = 0.12f)
    }

    drawIntoCanvas { canvas ->
        val paint = Paint()
        val frameworkPaint = paint.asFrameworkPaint()
        frameworkPaint.color = shadowColor.toArgb()

        val blurPx = blurRadius.toPx()
        if (blurPx > 0f) {
            frameworkPaint.maskFilter = android.graphics.BlurMaskFilter(
                blurPx,
                android.graphics.BlurMaskFilter.Blur.NORMAL
            )
        }
        val outline = shape.createOutline(size, layoutDirection, this)
        canvas.save()
        canvas.translate(0f, offsetY.toPx())
        canvas.drawOutline(outline, paint)
        canvas.restore()
    }
}

@Composable
fun elevatedInputFieldColors(
    isDark: Boolean = isAppInDarkMode()
): TextFieldColors {
    val primaryColor = MaterialTheme.colorScheme.primary
    return OutlinedTextFieldDefaults.colors(
        focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color(0xFFFAFAFA),
        unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else Color(0xFFFAFAFA),
        disabledContainerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f) else Color(0xFFF2F2F2),
        focusedTextColor = if (isDark) Color.White else Color(0xFF1C1B1F),
        unfocusedTextColor = if (isDark) Color.White else Color(0xFF1C1B1F),
        disabledTextColor = if (isDark) Color(0xFFAAAAAA) else Color(0xFF777777),
        focusedBorderColor = primaryColor,
        unfocusedBorderColor = if (isDark) Color(0xFF4A4D58) else Color(0xFFC8C8C8),
        focusedLabelColor = primaryColor,
        unfocusedLabelColor = if (isDark) Color(0xFFDDDDDD) else Color(0xFF555555),
        cursorColor = primaryColor,
        focusedLeadingIconColor = primaryColor,
        unfocusedLeadingIconColor = if (isDark) Color(0xFFCCCCCC) else Color(0xFF666666),
        focusedTrailingIconColor = primaryColor,
        unfocusedTrailingIconColor = if (isDark) Color(0xFFCCCCCC) else Color(0xFF666666),
        focusedPlaceholderColor = if (isDark) Color(0xFF888888) else Color(0xFF888888),
        unfocusedPlaceholderColor = if (isDark) Color(0xFF888888) else Color(0xFF888888)
    )
}
