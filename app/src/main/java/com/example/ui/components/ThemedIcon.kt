package com.example.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.theme.IconPresetSettings
import com.example.ui.theme.presetIconColor

val LocalSolidIconTileState = compositionLocalOf<MutableState<ImageVector?>?> { null }

@Composable
fun ThemedIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    keepTint: Boolean = false
) {
    val solidTileState = LocalSolidIconTileState.current
    if (solidTileState != null) {
        solidTileState.value = imageVector
    }
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    if (!keepTint && IconPresetSettings.enabled) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = if (solidTileState != null) Color.White else presetIconColor(imageVector, isDark)
        )
    } else {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = tint
        )
    }
}

@Composable
fun PresetPreviewIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    isDark: Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f
) {
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = presetIconColor(imageVector, isDark)
    )
}

fun iconTileColor(icon: ImageVector, fallback: Color, isDark: Boolean): Color {
    if (!IconPresetSettings.enabled) return fallback
    return presetIconColor(icon, isDark).copy(alpha = fallback.alpha)
}
