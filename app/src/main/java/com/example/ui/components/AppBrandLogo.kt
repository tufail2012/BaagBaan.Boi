package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.AppThemeMode

/**
 * Reusable theme-adaptive App Logo Composable.
 * Renders the realistic red apple icon with pointed leaf on the right side.
 * Automatically adapts the background container based on the current theme mode:
 * - Light Mode: Pure White background (#FFFFFF)
 * - Dark Mode: Dark background (#121212)
 * - AMOLED Mode: Pure Black background (#000000)
 */
@Composable
fun AppBrandLogo(
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    shape: Shape = RoundedCornerShape(22.dp),
    themeMode: AppThemeMode? = null,
    elevation: Dp = 2.dp,
    contentDescription: String = "Baagbaan Boi Apple Logo"
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
        AppThemeMode.AMOLED -> true
        null -> MaterialTheme.colorScheme.background.red < 0.25f
    }
    val isAmoled = themeMode == AppThemeMode.AMOLED ||
            (isDark && (MaterialTheme.colorScheme.background == Color(0xFF000000) || MaterialTheme.colorScheme.surface == Color(0xFF000000)))

    val backgroundColor = when {
        isAmoled -> Color(0xFF000000)
        isDark -> Color(0xFF121212)
        else -> Color(0xFFFFFFFF)
    }

    val borderColor = when {
        isAmoled -> Color(0xFF262626)
        isDark -> Color(0xFF2E2E2E)
        else -> Color(0xFFE5E7EB)
    }

    Box(
        modifier = modifier
            .size(size)
            .shadow(if (isDark) 0.dp else elevation, shape)
            .clip(shape)
            .background(backgroundColor)
            .border(1.dp, borderColor, shape)
            .padding(size * 0.08f),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_apple_logo),
            contentDescription = contentDescription,
            modifier = Modifier.size(size * 0.84f)
        )
    }
}

