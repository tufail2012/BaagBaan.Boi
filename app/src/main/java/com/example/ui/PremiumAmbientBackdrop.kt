package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Top-level alias exposing [com.example.ui.components.PremiumGlassAmbientBackdrop]
 * to callers in the [com.example.ui] package without requiring an explicit import.
 */
@Composable
fun PremiumGlassAmbientBackdrop(
    accentColor: Color,
    isDark: Boolean,
    isAmoled: Boolean = false,
    modifier: Modifier = Modifier
) {
    com.example.ui.components.PremiumGlassAmbientBackdrop(
        accentColor = accentColor,
        isDark = isDark,
        isAmoled = isAmoled,
        modifier = modifier
    )
}
