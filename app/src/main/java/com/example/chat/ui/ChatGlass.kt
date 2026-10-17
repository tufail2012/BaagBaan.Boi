package com.example.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.ui.components.glassContentColor
import com.example.ui.components.isGlassSupported
import com.example.ui.components.liquidGlassNav
import com.kyant.backdrop.Backdrop

@Composable
internal fun Modifier.chatHeaderGlass(
    backdrop: Backdrop?,
    shape: CornerBasedShape = RoundedCornerShape(percent = 50)
): Modifier {
    return if (backdrop != null && isGlassSupported()) {
        this.liquidGlassNav(shape = shape, backdrop = backdrop)
    } else {
        this
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            .border(0.5.dp, glassContentColor().copy(alpha = 0.15f), shape)
    }
}
