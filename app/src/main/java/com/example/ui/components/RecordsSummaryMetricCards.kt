package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import java.text.NumberFormat
import java.util.Locale

/**
 * 2x2 Metric Grid Component rendered globally across Records views.
 *
 * Displays 4 summary cards:
 * 1. Total Payment (Wallet, ₹Total)
 * 2. Received Payment (Checkmark circle, Green accent, ₹Received)
 * 3. Pending Payment (Hourglass / Alert, Red/Orange accent, ₹Pending)
 * 4. Total Quantity (Box / Package, Blue accent, X Units / X Plants)
 *
 * Styled with frosted glassmorphism (10dp blur, 16dp rounded corners, 1px specular border, subtle shadow).
 */
@Composable
fun RecordsSummaryMetricCards(
    totalPayment: Double,
    receivedPayment: Double,
    pendingPayment: Double,
    totalQuantity: Int,
    quantityLabel: String = "Plants",
    isDark: Boolean,
    paletteAccent: Color = MaterialTheme.colorScheme.primary,
    hazeState: HazeState? = null,
    modifier: Modifier = Modifier
) {
    val numberFmt = remember { NumberFormat.getNumberInstance(Locale("en", "IN")) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("records_summary_metric_grid"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Row 1: Total Payment & Received Payment
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Total Payment
            SummaryMetricCardItem(
                title = "Total Payment",
                value = "₹${numberFmt.format(totalPayment.toLong())}",
                icon = Icons.Default.AccountBalanceWallet,
                cardAccent = paletteAccent,
                isDark = isDark,
                paletteAccent = paletteAccent,
                hazeState = hazeState,
                testTag = "metric_card_total_payment",
                modifier = Modifier.weight(1f)
            )

            // 2. Received Payment (Green accent)
            val receivedColor = if (isDark) Color(0xFF4ADE80) else Color(0xFF15803D)
            SummaryMetricCardItem(
                title = "Received Payment",
                value = "₹${numberFmt.format(receivedPayment.toLong())}",
                icon = Icons.Default.CheckCircle,
                cardAccent = receivedColor,
                isDark = isDark,
                paletteAccent = paletteAccent,
                hazeState = hazeState,
                testTag = "metric_card_received_payment",
                modifier = Modifier.weight(1f)
            )
        }

        // Row 2: Pending Payment & Total Quantity
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 3. Pending Payment (Red/Orange accent)
            val pendingColor = if (isDark) Color(0xFFF87171) else Color(0xFFB91C1C)
            SummaryMetricCardItem(
                title = "Pending Payment",
                value = "₹${numberFmt.format(pendingPayment.toLong())}",
                icon = Icons.Default.HourglassTop,
                cardAccent = pendingColor,
                isDark = isDark,
                paletteAccent = paletteAccent,
                hazeState = hazeState,
                testTag = "metric_card_pending_payment",
                modifier = Modifier.weight(1f)
            )

            // 4. Total Quantity (Blue accent)
            val quantityColor = if (isDark) Color(0xFF60A5FA) else Color(0xFF1D4ED8)
            SummaryMetricCardItem(
                title = "Total Quantity",
                value = "${numberFmt.format(totalQuantity)} $quantityLabel",
                icon = Icons.Default.Inventory2,
                cardAccent = quantityColor,
                isDark = isDark,
                paletteAccent = paletteAccent,
                hazeState = hazeState,
                testTag = "metric_card_total_quantity",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SummaryMetricCardItem(
    title: String,
    value: String,
    icon: ImageVector,
    cardAccent: Color,
    isDark: Boolean,
    paletteAccent: Color,
    hazeState: HazeState?,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(16.dp)

    // Glassmorphism styling matching the active theme specs:
    // Background: rgba(255, 255, 255, 0.45) (or tinted with theme color)
    // Blur: 10dp
    // Border: 1px solid rgba(255, 255, 255, 0.45)
    // Shadow: 0 4px 12px rgba(0, 0, 0, 0.03)
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

    val cardBorderBrush = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = if (isDark) 0.35f else 0.55f),
            paletteAccent.copy(alpha = 0.20f),
            Color.White.copy(alpha = 0.25f)
        )
    )

    Box(
        modifier = modifier
            .shadow(
                elevation = 3.dp,
                shape = cardShape,
                spotColor = Color.Black.copy(alpha = 0.03f),
                ambientColor = Color.Black.copy(alpha = 0.02f)
            )
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
            .clip(cardShape)
            .background(cardBgBrush, shape = cardShape)
            .border(BorderStroke(1.dp, cardBorderBrush), shape = cardShape)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Circular Icon badge
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(cardAccent.copy(alpha = if (isDark) 0.22f else 0.14f))
                    .border(
                        1.dp,
                        cardAccent.copy(alpha = if (isDark) 0.45f else 0.28f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = cardAccent,
                    modifier = Modifier.size(17.dp)
                )
            }

            // Labels & Metrics Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = value,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = cardAccent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
