package com.example.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow

/**
 * Builds an AnnotatedString that dynamically highlights matching occurrences of [query] within [text].
 */
fun buildHighlightedAnnotatedString(
    text: String,
    query: String,
    normalStyle: SpanStyle = SpanStyle(),
    highlightStyle: SpanStyle = SpanStyle(
        fontWeight = FontWeight.ExtraBold,
        background = Color(0xFFFEF08A), // Vibrant golden yellow background highlight
        color = Color(0xFF713F12)      // High contrast deep amber text
    ),
    isDark: Boolean = false
): AnnotatedString {
    if (text.isEmpty()) return AnnotatedString("")
    val cleanQuery = query.trim()
    if (cleanQuery.isEmpty()) {
        return buildAnnotatedString {
            pushStyle(normalStyle)
            append(text)
            pop()
        }
    }

    val activeHighlightStyle = if (isDark) {
        SpanStyle(
            fontWeight = FontWeight.ExtraBold,
            background = Color(0xFF854D0E), // Deep amber background for dark mode
            color = Color(0xFFFEF08A)      // Bright yellow text for high contrast
        )
    } else {
        highlightStyle
    }

    return buildAnnotatedString {
        var start = 0
        val lowerText = text.lowercase()
        val lowerQuery = cleanQuery.lowercase()
        var index = lowerText.indexOf(lowerQuery, start)

        while (index >= 0) {
            // Append preceding un-highlighted portion
            if (index > start) {
                pushStyle(normalStyle)
                append(text.substring(start, index))
                pop()
            }
            // Append highlighted match portion
            pushStyle(activeHighlightStyle)
            append(text.substring(index, index + cleanQuery.length))
            pop()

            start = index + cleanQuery.length
            index = lowerText.indexOf(lowerQuery, start)
        }

        // Append remaining un-highlighted text
        if (start < text.length) {
            pushStyle(normalStyle)
            append(text.substring(start))
            pop()
        }
    }
}

/**
 * Highlighting Text Composable that dynamically highlights search matches as the user types.
 */
@Composable
fun HighlightedText(
    text: String,
    query: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    textAlign: TextAlign? = null,
    style: TextStyle = TextStyle.Default,
    isDark: Boolean = false,
    highlightColor: Color? = null,
    highlightTextColor: Color? = null,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    val effectiveHighlightBg = highlightColor ?: if (isDark) Color(0xFF854D0E) else Color(0xFFFEF08A)
    val effectiveHighlightText = highlightTextColor ?: if (isDark) Color(0xFFFEF08A) else Color(0xFF713F12)

    val annotatedString = buildHighlightedAnnotatedString(
        text = text,
        query = query,
        normalStyle = SpanStyle(),
        highlightStyle = SpanStyle(
            fontWeight = FontWeight.ExtraBold,
            background = effectiveHighlightBg,
            color = effectiveHighlightText,
            textDecoration = TextDecoration.None
        ),
        isDark = isDark
    )

    Text(
        text = annotatedString,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        maxLines = maxLines,
        overflow = overflow,
        textAlign = textAlign,
        style = style,
        onTextLayout = onTextLayout
    )
}
