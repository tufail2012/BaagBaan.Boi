package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GlassTuning
import kotlin.math.roundToInt

@Composable
fun GlassTuningControls() {
    val context = LocalContext.current
    var advancedOpen by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth()) {
        GlassPercentRow(
            title = "Blur strength",
            percent = GlassTuning.blurPercent,
            onChange = { GlassTuning.setBlurPercent(context, it) },
        )

        Row(
            Modifier
                .fillMaxWidth()
                .clickable { advancedOpen = !advancedOpen }
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Advanced glass settings",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = if (advancedOpen) "Hide" else "Show",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        AnimatedVisibility(
            visible = advancedOpen,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(Modifier.fillMaxWidth()) {
                GlassPercentRow(
                    title = "Lens height",
                    percent = GlassTuning.lensHeightPercent,
                    onChange = { GlassTuning.setLensHeightPercent(context, it) },
                )
                GlassPercentRow(
                    title = "Lens amount",
                    percent = GlassTuning.lensAmountPercent,
                    onChange = { GlassTuning.setLensAmountPercent(context, it) },
                )
                GlassPercentRow(
                    title = "Surface opacity",
                    percent = GlassTuning.surfacePercent,
                    onChange = { GlassTuning.setSurfacePercent(context, it) },
                )
                TextButton(onClick = { GlassTuning.resetAll(context) }) {
                    Text("Reset glass to default")
                }
            }
        }
    }
}

@Composable
private fun GlassPercentRow(
    title: String,
    percent: Int,
    onChange: (Int) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "$percent%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        GlassSlider(
            value = percent.toFloat(),
            valueRange = 0f..200f,
            activeColor = MaterialTheme.colorScheme.primary,
            inactiveColor = MaterialTheme.colorScheme.surfaceVariant,
            backdrop = null,
            onValueChange = { onChange(it.roundToInt()) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
