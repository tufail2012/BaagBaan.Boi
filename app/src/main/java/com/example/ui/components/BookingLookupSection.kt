package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CropRecord
import com.example.data.GardenPlanningEntry

/**
 * Unified model for booking history lookup results across all booking tabs.
 */
data class MatchingBookingItem(
    val id: Long,
    val serialNumber: String,
    val farmerName: String,
    val category: String,
    val bookingDate: String,
    val paymentStatus: String,
    val isCancelled: Boolean = false,
    val isReceived: Boolean = false,
    val isGardenPlanning: Boolean = false,
    val sortTimestamp: Long = 0L
)

object BookingLookupHelper {

    /**
     * Normalizes a phone number to standard 10 digits for accurate lookup across various entry formats
     * (e.g. "+91 9876543210", "9876543210", "+919876543210", "09876543210").
     */
    fun normalizePhone(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        var clean = raw.trim()
        if (clean.startsWith("+91")) clean = clean.removePrefix("+91").trim()
        else if (clean.startsWith("91") && clean.length > 10) clean = clean.removePrefix("91").trim()
        else if (clean.startsWith("+")) clean = clean.removePrefix("+").trim()
        else if (clean.startsWith("0") && clean.length == 11) clean = clean.substring(1).trim()
        val digits = clean.filter { it.isDigit() }
        return if (digits.length >= 10) digits.takeLast(10) else ""
    }

    /**
     * Searches all crop records and garden planning entries across the entire database by phone number.
     */
    fun findMatchingBookings(
        rawInputPhone: String,
        cropRecords: List<CropRecord>,
        gardenEntries: List<GardenPlanningEntry>,
        excludeCropId: Long? = null,
        excludeGardenId: Long? = null
    ): List<MatchingBookingItem> {
        val normalizedInput = normalizePhone(rawInputPhone)
        if (normalizedInput.length < 10) return emptyList()

        val results = mutableListOf<MatchingBookingItem>()

        // 1. Crop Records (Local Plants, Imported Plants, Rootstocks, Pruning, Site Visit, etc.)
        cropRecords.forEach { rec ->
            if (excludeCropId != null && rec.id == excludeCropId) return@forEach
            val recNorm = normalizePhone(rec.contactNumber)
            if (recNorm == normalizedInput) {
                val displayCategory = when (rec.serviceType.trim().lowercase()) {
                    "imported" -> "Imported Plants"
                    "local" -> "Local Plants"
                    else -> rec.serviceType.ifBlank { "Crop Booking" }
                }
                results.add(
                    MatchingBookingItem(
                        id = rec.id,
                        serialNumber = rec.serialNumber.ifBlank { "REC-${rec.id}" },
                        farmerName = rec.farmerName,
                        category = displayCategory,
                        bookingDate = rec.bookingDate,
                        paymentStatus = rec.paymentStatus.ifBlank { "Pending" },
                        isCancelled = rec.isCancelled,
                        isReceived = rec.isReceived,
                        isGardenPlanning = false,
                        sortTimestamp = rec.timestamp
                    )
                )
            }
        }

        // 2. Garden Planning Entries
        gardenEntries.forEach { entry ->
            if (excludeGardenId != null && entry.id == excludeGardenId) return@forEach
            val entryNorm = normalizePhone(entry.contactNumber)
            if (entryNorm == normalizedInput) {
                results.add(
                    MatchingBookingItem(
                        id = entry.id,
                        serialNumber = entry.serialNumber.ifBlank { "GP-${entry.id}" },
                        farmerName = entry.farmerName,
                        category = "Garden Planning",
                        bookingDate = entry.bookingDate,
                        paymentStatus = entry.paymentStatus.ifBlank { "Pending" },
                        isCancelled = entry.paymentStatus.equals("Cancelled", ignoreCase = true),
                        isReceived = false,
                        isGardenPlanning = true,
                        sortTimestamp = entry.timestamp
                    )
                )
            }
        }

        // Sort by timestamp descending (newest bookings first)
        return results.sortedByDescending { it.sortTimestamp }
    }
}

/**
 * Reusable, compact, theme-adaptive Booking Lookup Result UI.
 * Displays matching previous booking records directly under the Contact Number field.
 */
@Composable
fun ExistingBookingsLookupSection(
    matchingBookings: List<MatchingBookingItem>,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = matchingBookings.isNotEmpty(),
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
            border = BorderStroke(
                1.dp,
                if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 4.dp)
                .testTag("existing_bookings_lookup_section")
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Existing Booking(s)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "${matchingBookings.size} Found",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                // Compact vertically scrollable list of matching booking cards
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    matchingBookings.forEach { item ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isDark) Color(0xFF0F172A) else Color.White,
                            border = BorderStroke(
                                1.dp,
                                if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("existing_booking_item_${item.serialNumber}")
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Top Row: Serial Number • Service Category • Booking Date
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.weight(1f, fill = false)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                        ) {
                                            Text(
                                                text = item.serialNumber,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = "•",
                                            fontSize = 11.sp,
                                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                        )
                                        Text(
                                            text = item.category,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (item.bookingDate.isNotBlank()) {
                                            Text(
                                                text = "•",
                                                fontSize = 11.sp,
                                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                            )
                                            Text(
                                                text = item.bookingDate,
                                                fontSize = 11.sp,
                                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                            )
                                        }
                                    }
                                }

                                // Bottom Row: Customer Name & Status Badge
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (item.farmerName.isNotBlank()) {
                                        Text(
                                            text = "Customer: ${item.farmerName}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Normal,
                                            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }

                                    val statusLabel = when {
                                        item.isCancelled -> "Cancelled"
                                        item.isReceived -> "Received"
                                        item.paymentStatus.isNotBlank() -> item.paymentStatus
                                        else -> "Pending"
                                    }

                                    val (badgeBg, badgeFg) = when (statusLabel.lowercase().trim()) {
                                        "fully paid", "cleared" -> Pair(
                                            if (isDark) Color(0xFF14532D) else Color(0xFFE8F5E9),
                                            if (isDark) Color(0xFF86EFAC) else Color(0xFF2E7D32)
                                        )
                                        "advance paid" -> Pair(
                                            if (isDark) Color(0xFF1E3A8A) else Color(0xFFE3F2FD),
                                            if (isDark) Color(0xFF93C5FD) else Color(0xFF1565C0)
                                        )
                                        "received", "completed" -> Pair(
                                            if (isDark) Color(0xFF134E4A) else Color(0xFFE0F2F1),
                                            if (isDark) Color(0xFF5EEAD4) else Color(0xFF00695C)
                                        )
                                        "cancelled" -> Pair(
                                            if (isDark) Color(0xFF7F1D1D) else Color(0xFFFFEBEE),
                                            if (isDark) Color(0xFFFCA5A5) else Color(0xFFC62828)
                                        )
                                        else -> Pair(
                                            if (isDark) Color(0xFF7C2D12) else Color(0xFFFFF3E0),
                                            if (isDark) Color(0xFFFDBA74) else Color(0xFFE65100)
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = badgeBg
                                    ) {
                                        Text(
                                            text = "Status: $statusLabel",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = badgeFg,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
