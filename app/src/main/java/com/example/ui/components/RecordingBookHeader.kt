package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.HazeMaterials

/**
 * Shared Recording Book Header Banner with Frosted Liquid Glass styling and
 * prominent Record Counter Badge (e.g., "Total Records: 100").
 */
@Composable
fun RecordingBookHeader(
    title: String,
    count: Int,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = LocalAppGlassHazeState.current
) {
    val isDark = isAppInDarkMode()
    val headerShape = RoundedCornerShape(18.dp)

    val fillBrush = Brush.verticalGradient(
        if (isDark) listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.04f))
        else listOf(Color.White.copy(alpha = 0.42f), Color.White.copy(alpha = 0.14f))
    )

    val borderBrush = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = if (isDark) 0.50f else 0.70f),
            Color.White.copy(alpha = if (isDark) 0.10f else 0.20f)
        )
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 2.dp)
            .shadow(
                elevation = 2.dp,
                shape = headerShape,
                spotColor = Color.Black.copy(alpha = if (isDark) 0.25f else 0.06f),
                ambientColor = Color.Black.copy(alpha = if (isDark) 0.15f else 0.03f)
            )
            .then(
                if (hazeState != null) {
                    Modifier.hazeEffect(state = hazeState, style = HazeMaterials.thin())
                } else Modifier
            )
            .clip(headerShape)
            .background(fillBrush, shape = headerShape)
            .border(BorderStroke(1.dp, borderBrush), shape = headerShape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Book Icon & Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.22f else 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = title,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) Color.White else Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Target Component 2: Frosted Liquid Glass Record Counter Badge (e.g., "Total Records: 100")
            val badgeFillBrush = Brush.verticalGradient(
                if (isDark) listOf(Color.White.copy(alpha = 0.20f), Color.White.copy(alpha = 0.08f))
                else listOf(Color.White.copy(alpha = 0.65f), Color.White.copy(alpha = 0.35f))
            )
            val badgeBorderBrush = Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = if (isDark) 0.65f else 0.85f),
                    Color.White.copy(alpha = if (isDark) 0.20f else 0.40f)
                )
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(badgeFillBrush, shape = RoundedCornerShape(percent = 50))
                    .border(BorderStroke(1.dp, badgeBorderBrush), shape = RoundedCornerShape(percent = 50))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    // Small glowing dot
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Text(
                        text = "Total Records: $count",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
