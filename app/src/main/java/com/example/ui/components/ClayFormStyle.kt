package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val CLAY_CARD_SHAPE = RoundedCornerShape(28.dp)
val CLAY_FIELD_SHAPE = RoundedCornerShape(26.dp)
val CLAY_ICON_CHIP_SIZE = 40.dp

@Composable
fun clayAccentGradient(accentColor: Color): Brush {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(accentColor.toArgb(), hsv)
    val warmer = Color(
        android.graphics.Color.HSVToColor(
            floatArrayOf((hsv[0] + 28f) % 360f, hsv[1] * 0.9f, (hsv[2] * 1.05f).coerceAtMost(1f))
        )
    )
    return Brush.linearGradient(listOf(accentColor, warmer))
}

@Composable
fun clayCardBrush(isDark: Boolean): Brush {
    val top = if (isDark) Color(0xFF362A3E) else Color(0xFFFFFFFF)
    val bot = if (isDark) Color(0xFF281F30) else Color(0xFFF7F3FC)
    return Brush.verticalGradient(listOf(top, bot))
}

@Composable
fun clayFieldBrush(isDark: Boolean): Brush {
    val top = if (isDark) Color(0xFF2C2233) else Color(0xFFECE7F6)
    val bot = if (isDark) Color(0xFF221A28) else Color(0xFFF4F0FA)
    return Brush.verticalGradient(listOf(top, bot))
}

@Composable
fun clayTextColor(isDark: Boolean): Color = if (isDark) Color(0xFFF5F0FA) else Color(0xFF2E2138)

@Composable
fun claySubTextColor(isDark: Boolean): Color = if (isDark) Color(0xFFBCACC4) else Color(0xFF8C7896)

@Composable
fun claySectionTitleColor(accentColor: Color, isDark: Boolean): Color {
    if (!isDark) return accentColor
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(accentColor.toArgb(), hsv)
    hsv[2] = (hsv[2] * 1.25f).coerceAtMost(1f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

/** Raised "clay" surface: soft directional shadow + a small top-left light glow. No blur/refraction. */
fun Modifier.clayRaised(shape: Shape, isDark: Boolean): Modifier = this
    .shadow(
        elevation = if (isDark) 10.dp else 8.dp,
        shape = shape,
        clip = false,
        ambientColor = Color.Black.copy(alpha = if (isDark) 0.55f else 0.22f),
        spotColor = Color.Black.copy(alpha = if (isDark) 0.55f else 0.22f)
    )
    // If ambientColor/spotColor are unresolved on this Compose version, fall back to:
    // .shadow(elevation = if (isDark) 10.dp else 8.dp, shape = shape, clip = false)
    .drawBehind {
        drawRoundRect(
            color = Color.White.copy(alpha = if (isDark) 0.05f else 0.55f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.6f, size.height * 0.3f),
            cornerRadius = CornerRadius(24.dp.toPx())
        )
    }

/** Inset ("pressed into the clay") look for fields: dark band top-inside, light band bottom-inside. */
fun Modifier.clayInset(shape: CornerBasedShape): Modifier = this.drawBehind {
    val r = shape.topStart.toPx(size, this)
    val corner = CornerRadius(r)
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.16f), Color.Transparent), endY = size.height * 0.6f),
        cornerRadius = corner
    )
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.35f)), startY = size.height * 0.5f),
        cornerRadius = corner
    )
}

@Composable
fun clayFieldColors(isDark: Boolean, accentColor: Color): TextFieldColors {
    val text = clayTextColor(isDark)
    val sub = claySubTextColor(isDark)
    return TextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent,
        errorContainerColor = Color.Transparent,
        focusedTextColor = text,
        unfocusedTextColor = text,
        focusedLabelColor = accentColor,
        unfocusedLabelColor = sub,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
        cursorColor = accentColor,
        focusedTrailingIconColor = accentColor,
        unfocusedTrailingIconColor = accentColor.copy(alpha = 0.85f),
        focusedPlaceholderColor = sub,
        unfocusedPlaceholderColor = sub
    )
}

/** Small glossy gradient circle chip for a field's leading icon. */
@Composable
fun ClayFieldIcon(accentColor: Color, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(CLAY_ICON_CHIP_SIZE)
            .shadow(elevation = 4.dp, shape = CircleShape, clip = false)
            .clip(CircleShape)
            .background(clayAccentGradient(accentColor)),
        contentAlignment = Alignment.Center
    ) { content() }
}

/**
 * Vibrant 3D "Clay Pop" section card for New Entry forms. Keeps backdrop/hazeState
 * params (unused) so existing glass-card call sites swap in with minimal edits.
 */
@Composable
fun ClaySectionCard(
    title: String = "",
    accentColor: Color = Color.Unspecified,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    backdrop: com.kyant.backdrop.Backdrop? = null,
    hazeState: dev.chrisbanes.haze.HazeState? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(modifier = modifier.fillMaxWidth().padding(6.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clayRaised(CLAY_CARD_SHAPE, isDark)
                .clip(CLAY_CARD_SHAPE)
                .background(clayCardBrush(isDark))
                .padding(20.dp)
        ) {
            if (title.isNotEmpty()) {
                Text(
                    text = title.uppercase(),
                    color = claySectionTitleColor(if (accentColor != Color.Unspecified) accentColor else MaterialTheme.colorScheme.primary, isDark),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            content()
        }
    }
}
