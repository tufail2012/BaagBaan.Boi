package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.ui.theme.getAppDimBackgroundBrush
import com.example.util.VarietyQuantityRow
import com.kyant.backdrop.Backdrop
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import java.text.NumberFormat
import java.util.Locale

@Composable
fun VarietyQuantityScreen(
    title: String,
    rows: List<VarietyQuantityRow>,
    totalQuantity: Int,
    unitLabel: String,
    isDark: Boolean,
    paletteAccent: Color,
    backdrop: Backdrop? = null,
    hazeState: HazeState? = null,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val numberFmt = remember { NumberFormat.getNumberInstance(Locale("en", "IN")) }
    val isAmoled = isAppInAmoledMode()
    val pageBackgroundBrush = remember(isDark, isAmoled, paletteAccent) {
        getAppDimBackgroundBrush(paletteAccent, isDark = isDark, isAmoled = isAmoled)
    }
    val primaryTextColor = if (isDark) Color.White else Color(0xFF0F172A)
    val secondaryTextColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(12f)
            .background(pageBackgroundBrush)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Top bar with back arrow and title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("variety_quantity_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = primaryTextColor
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Summary card: "Total" on the left, totalQuantity + unitLabel on the right
            VarietyQuantityGlassCard(
                isDark = isDark,
                paletteAccent = paletteAccent,
                backdrop = backdrop,
                hazeState = hazeState,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("variety_quantity_total")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${numberFmt.format(totalQuantity)} $unitLabel",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = paletteAccent,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (rows.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(bottom = 110.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No varieties entered yet",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = secondaryTextColor
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 110.dp)
                ) {
                    itemsIndexed(rows) { index, row ->
                        VarietyQuantityGlassCard(
                            isDark = isDark,
                            paletteAccent = paletteAccent,
                            backdrop = backdrop,
                            hazeState = hazeState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("variety_quantity_row_$index")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = row.variety,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryTextColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 12.dp)
                                )
                                Text(
                                    text = "${numberFmt.format(row.quantity)} $unitLabel",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = paletteAccent,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VarietyQuantityGlassCard(
    isDark: Boolean,
    paletteAccent: Color,
    backdrop: Backdrop?,
    hazeState: HazeState?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val cardShape = RoundedCornerShape(16.dp)
    val glassOn = isGlassSupported()

    val cardBgBrush = Brush.verticalGradient(
        if (isDark) listOf(
            paletteAccent.copy(alpha = 0.16f),
            Color(0xFF0F172A).copy(alpha = 0.55f),
            Color(0xFF0F172A).copy(alpha = 0.42f)
        )
        else listOf(
            Color.White.copy(alpha = 0.55f),
            paletteAccent.copy(alpha = 0.08f),
            Color.White.copy(alpha = 0.45f)
        )
    )

    val solidCardBgBrush = Brush.verticalGradient(
        if (isDark) listOf(
            Color(0xFF1E293B),
            Color(0xFF0F172A)
        )
        else listOf(
            Color.White,
            Color(0xFFF8FAFC)
        )
    )

    val cardBorderBrush = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = if (isDark) 0.35f else 0.55f),
            paletteAccent.copy(alpha = 0.20f),
            Color.White.copy(alpha = 0.25f)
        )
    )

    Box(
        modifier = modifier.then(
            if (backdrop != null && glassOn) {
                Modifier.recordsLiquidGlass(
                    backdrop = backdrop,
                    shape = cardShape
                )
            } else if (glassOn) {
                Modifier
                    .shadow(
                        elevation = 3.dp,
                        shape = cardShape,
                        spotColor = Color.Black.copy(alpha = 0.03f),
                        ambientColor = Color.Black.copy(alpha = 0.02f)
                    )
                    .clip(cardShape)
                    .then(
                        if (hazeState != null) {
                            Modifier.hazeEffect(
                                state = hazeState,
                                style = HazeStyle(
                                    blurRadius = 10.dp,
                                    tints = listOf(
                                        HazeTint(paletteAccent.copy(alpha = if (isDark) 0.10f else 0.06f))
                                    ),
                                    backgroundColor = Color.Transparent
                                )
                            )
                        } else Modifier
                    )
                    .background(cardBgBrush, shape = cardShape)
                    .border(BorderStroke(1.dp, cardBorderBrush), shape = cardShape)
            } else {
                Modifier
                    .shadow(
                        elevation = 3.dp,
                        shape = cardShape,
                        spotColor = Color.Black.copy(alpha = 0.03f),
                        ambientColor = Color.Black.copy(alpha = 0.02f)
                    )
                    .clip(cardShape)
                    .background(solidCardBgBrush, shape = cardShape)
                    .background(cardBgBrush, shape = cardShape)
                    .border(BorderStroke(1.dp, cardBorderBrush), shape = cardShape)
            }
        )
    ) {
        content()
    }
}
