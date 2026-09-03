package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import com.example.util.rememberScrollHapticFeedback
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import com.example.util.MapHelper
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.example.ui.components.BrandedPullToRefreshBox
import com.example.data.CropRecord
import com.example.data.calculateRemainingBalance
import com.example.data.calculateTotalAmount
import com.example.data.isPaymentCleared
import com.example.ui.CropViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FarmerRecordsScreen(
    viewModel: CropViewModel,
    modifier: Modifier = Modifier
) {
    val records by viewModel.filteredRecords.collectAsState()
    val searchQuery by viewModel.recordsSearchQuery.collectAsState()
    val selectedPaymentFilter by viewModel.selectedPaymentFilter.collectAsState()
    val selectedService by viewModel.selectedService.collectAsState()
    val selectedPruningSubTab by viewModel.selectedPruningSubTab.collectAsState()
    val selectedRootstockSubTab by viewModel.selectedRootstockSubTab.collectAsState()
    val selectedGenevaOption by viewModel.selectedGenevaOption.collectAsState()
    var selectedDetailRecord by remember { mutableStateOf<CropRecord?>(null) }
    var recordToDelete by remember { mutableStateOf<CropRecord?>(null) }

    val listState = rememberLazyListState()
    listState.rememberScrollHapticFeedback()

    LaunchedEffect(selectedService, selectedPruningSubTab, selectedRootstockSubTab, selectedGenevaOption, selectedPaymentFilter) {
        listState.scrollToItem(0)
    }

    val context = LocalContext.current

    val bookTitleName = when {
        selectedService.equals("Pruning", ignoreCase = true) -> selectedPruningSubTab
        selectedService.equals("Rootstocks", ignoreCase = true) -> {
            if (selectedRootstockSubTab.startsWith("Geneva") && selectedGenevaOption != null) {
                "Geneva ($selectedGenevaOption)"
            } else {
                selectedRootstockSubTab
            }
        }
        selectedService.equals("Imported", ignoreCase = true) -> "Imported Plants"
        else -> selectedService
    }

    val bookTitle = if (selectedService.equals("Imported", ignoreCase = true)) "Imported Plants" else "$bookTitleName Recording Book"

    val isDark = isAppInDarkMode()
    val searchShape = RoundedCornerShape(24.dp)
    val isPruning = selectedService.equals("Pruning", ignoreCase = true)
    val isSiteVisit = selectedService.equals("Site Visit", ignoreCase = true)

    val animatedItemIds = remember(bookTitle, selectedPaymentFilter, searchQuery) { mutableSetOf<Any>() }

    var isInitialLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    androidx.compose.runtime.LaunchedEffect(records) {
        if (records.isNotEmpty()) {
            isInitialLoading = false
        } else {
            kotlinx.coroutines.delay(300)
            isInitialLoading = false
        }
    }

    // Financial & Quantity Summary Metrics for current records
    val totalPayment = records.sumOf { it.calculateTotalAmount() }
    val receivedPayment = records.sumOf { it.amountPaid }
    val pendingPayment = records.sumOf { it.calculateRemainingBalance() }
    val totalQuantity = records.sumOf { it.quantity }

    BrandedPullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            if (isRefreshing) return@BrandedPullToRefreshBox
            isRefreshing = true
            coroutineScope.launch {
                try {
                    delay(500)
                } catch (_: Exception) {
                } finally {
                    isRefreshing = false
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Fixed top spacing for floating AgriSegmentedControl
            Spacer(modifier = Modifier.height(72.dp))

            // Sticky Fixed Top Section: Recording Book Header Banner + Search Bar & Filter
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RecordingBookHeader(
                        title = bookTitle,
                        count = records.size
                    )
                    SearchBarWithStatusFilter(
                        searchQuery = searchQuery,
                        onSearchQueryChange = { viewModel.setRecordsSearchQuery(it) },
                        selectedFilter = selectedPaymentFilter,
                        onFilterSelected = { viewModel.setPaymentFilter(it) },
                        placeholderText = "Search by farmer name, phone, serial no...",
                        isDark = isDark,
                        testTagPrefix = "crop_search",
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }

            // Scrollable content (Total Payment, summaries, records) scrolling underneath
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 100.dp)
            ) {
                // 3. Summary Cards (Total Payment, Received, Pending, Quantity)
                item {
                    RecordSummaryCards(
                        totalPayment = totalPayment,
                        receivedPayment = receivedPayment,
                        pendingPayment = pendingPayment,
                        totalQuantity = totalQuantity,
                        isPruning = isPruning,
                        isSiteVisit = isSiteVisit,
                        isDark = isDark
                    )
                }

        // Records List, Shimmer Skeleton Loading, or Empty State
        if (records.isEmpty()) {
            if (isInitialLoading) {
                items(4) {
                    SkeletonCard(isDark = isDark, lineCount = 4, hasActionRow = true)
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                        Icon(
                            imageVector = if (selectedService.equals("Rootstocks", ignoreCase = true)) Icons.Default.Spa else Icons.Default.Park,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray
                        )
                        Text(
                            text = "No Records Found",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (searchQuery.isNotBlank() || selectedPaymentFilter != "All Records")
                                "No records in $bookTitleName match your search/filter criteria."
                            else
                                "No entries saved in the $bookTitleName Recording Book yet. Create a new entry under $bookTitleName to add it here.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = { viewModel.setViewMode(0) },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("Add New Entry to $bookTitleName")
                        }
                    }
                }
                }
            }
        } else {
            itemsIndexed(records, key = { _, record -> record.id }) { index, record ->
                StaggeredEntranceWrapper(
                    itemId = record.id,
                    index = index,
                    animatedItemIds = animatedItemIds
                ) {
                    SwipeableRecordItem(
                        record = record,
                        onDelete = { recordToDelete = record }
                    ) {
                        FarmerRecordCard(
                            record = record,
                            searchQuery = searchQuery,
                            onCallFarmer = {
                                if (record.contactNumber.isNotBlank()) {
                                    val intent = Intent(Intent.ACTION_DIAL).apply {
                                        data = Uri.parse("tel:${record.contactNumber}")
                                    }
                                    context.startActivity(intent)
                                }
                            },
                            onEdit = { viewModel.loadRecordForEditing(record) },
                            onDelete = { recordToDelete = record },
                            onOpenDetail = { selectedDetailRecord = record }
                        )
                    }
                }
            }
        }
    }
    }
    }

    recordToDelete?.let { rec ->
        DeleteBookingConfirmationDialog(
            title = "Delete this booking?",
            farmerName = rec.farmerName,
            identifier = if (rec.serialNumber.isNotBlank()) rec.serialNumber else rec.serviceType,
            onConfirm = {
                viewModel.deleteRecord(rec)
                recordToDelete = null
            },
            onDismiss = { recordToDelete = null }
        )
    }

    selectedDetailRecord?.let { detailRec ->
        BookingRecordDetailDialog(
            record = detailRec,
            onDismiss = { selectedDetailRecord = null },
            onEdit = { rec ->
                selectedDetailRecord = null
                viewModel.loadRecordForEditing(rec)
            },
            onDelete = { rec ->
                selectedDetailRecord = null
                viewModel.deleteRecord(rec)
            },
            onUpdateRecord = { updatedRec ->
                selectedDetailRecord = updatedRec
                viewModel.updateRecordSync(updatedRec)
            }
        )
    }
}

@Composable
private fun RecordSummaryCards(
    totalPayment: Double,
    receivedPayment: Double,
    pendingPayment: Double,
    totalQuantity: Int,
    isPruning: Boolean,
    isSiteVisit: Boolean = false,
    isDark: Boolean
) {
    val numberFmt = NumberFormat.getNumberInstance(Locale("en", "IN"))

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Card 1: Total Payment
            SummaryCardItem(
                title = "Total Payment",
                targetValue = totalPayment,
                formatter = { "₹${numberFmt.format(it.toLong())}" },
                icon = Icons.Default.AccountBalanceWallet,
                accentColor = MaterialTheme.colorScheme.primary,
                isDark = isDark,
                modifier = Modifier.weight(1f)
            )

            // Card 2: Received Payment
            SummaryCardItem(
                title = "Received Payment",
                targetValue = receivedPayment,
                formatter = { "₹${numberFmt.format(it.toLong())}" },
                icon = Icons.Default.CheckCircle,
                accentColor = if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.90f) else MaterialTheme.colorScheme.primary,
                isDark = isDark,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Card 3: Pending Payment
            SummaryCardItem(
                title = "Pending Payment",
                targetValue = pendingPayment,
                formatter = { "₹${numberFmt.format(it.toLong())}" },
                icon = Icons.Default.HourglassTop,
                accentColor = if (isDark) Color(0xFFE57373) else Color(0xFFC62828),
                isDark = isDark,
                modifier = Modifier.weight(1f)
            )

            // Card 4: Total Quantity (Hidden on Pruning and Site Visit screens)
            if (!isPruning && !isSiteVisit) {
                SummaryCardItem(
                    title = "Total Quantity",
                    targetValue = totalQuantity.toDouble(),
                    formatter = { "${numberFmt.format(it.toInt())} Units" },
                    icon = Icons.Default.Inventory2,
                    accentColor = if (isDark) Color(0xFF64B5F6) else Color(0xFF0288D1),
                    isDark = isDark,
                    modifier = Modifier.weight(1f)
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SummaryCardItem(
    title: String,
    targetValue: Double,
    formatter: (Double) -> String,
    icon: ImageVector,
    accentColor: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .glassCardBackground(
                isDark = isDark,
                accentColor = accentColor,
                shape = RoundedCornerShape(14.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = if (isDark) 0.22f else 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                CountUpText(
                    targetValue = targetValue,
                    formatter = formatter,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }
        }
    }
}

private fun openWhatsApp(context: Context, phoneNumber: String) {
    if (phoneNumber.isNotBlank()) {
        com.example.util.WhatsAppHelper.openWhatsAppChat(
            context = context,
            rawPhone = phoneNumber,
            onInvalidNumber = {
                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
                try {
                    context.startActivity(dialIntent)
                } catch (_: Exception) {
                    Toast.makeText(context, "Cannot open dialer or WhatsApp", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FarmerRecordCard(
    record: CropRecord,
    searchQuery: String = "",
    onCallFarmer: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenDetail: () -> Unit
) {
    val context = LocalContext.current
    val isDark = isAppInDarkMode()
    var showCardWhatsAppConfirm by remember { mutableStateOf(false) }

    val totalAmount = record.calculateTotalAmount()
    val remBalance = record.calculateRemainingBalance()

    val initial = record.farmerName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "F"

    val avatarBgColor = MaterialTheme.colorScheme.primary

    val statusLabel = when {
        record.isCancelled -> "Cancelled"
        record.isReceived -> "Received"
        record.isPaymentCleared() -> "Fully Paid"
        record.amountPaid > 0 -> "Advance Paid"
        else -> "Pending"
    }

    val (statusBadgeBg, statusBadgeText) = when {
        record.isCancelled -> Pair(if (isDark) Color(0xFF450A0A) else Color(0xFFFEE2E2), if (isDark) Color(0xFFFCA5A5) else Color(0xFFDC2626))
        record.isReceived -> Pair(if (isDark) Color(0xFF134E4A) else Color(0xFFCCFBF1), if (isDark) Color(0xFF5EEAD4) else Color(0xFF0F766E))
        record.isPaymentCleared() -> Pair(MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.30f else 0.15f), if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.95f) else MaterialTheme.colorScheme.primary)
        record.amountPaid > 0 -> Pair(if (isDark) Color(0xFF7C2D12) else Color(0xFFFFEDD5), if (isDark) Color(0xFFFDBA74) else Color(0xFFC2410C))
        else -> Pair(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), MaterialTheme.colorScheme.primary)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassCardBackground(
                isDark = isDark,
                accentColor = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .testTag("farmer_record_card_${record.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Row 1: Profile Initial Avatar + Farmer Name + Serial Number & Payment Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Profile Avatar with Initial
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(avatarBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initial,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Farmer Name & Serial No
                Column(modifier = Modifier.weight(1f)) {
                    HighlightedText(
                        text = record.farmerName.ifBlank { "Farmer Name Not Specified" },
                        query = searchQuery,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        isDark = isDark
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (record.serialNumber.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1))
                            ) {
                                Text(
                                    text = "#${record.serialNumber}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (record.bookingDate.isNotBlank()) {
                            Text(
                                text = "• ${record.bookingDate}",
                                fontSize = 10.5.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
                            )
                        }
                    }
                }

                // Payment Status Badge
                Surface(
                    color = statusBadgeBg,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = statusLabel,
                        color = statusBadgeText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 2.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // Address & Contact Info
            if (record.farmerAddress.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    HighlightedText(
                        text = record.farmerAddress,
                        query = searchQuery,
                        fontSize = 12.sp,
                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        isDark = isDark
                    )
                }
            }

            val isSiteVisit = record.serviceType.equals("Site Visit", ignoreCase = true)

            if (record.location.isNotBlank()) {
                if (MapHelper.isGoogleMapsUrl(record.location)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                MapHelper.openGoogleMaps(context, record.location)
                            }
                            .padding(vertical = 1.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = "Open in Google Maps",
                            tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF1D4ED8),
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "📍 Open Location in Google Maps",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color(0xFF60A5FA) else Color(0xFF1D4ED8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else if (isSiteVisit) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        HighlightedText(
                            text = "Location: ${record.location}",
                            query = searchQuery,
                            fontSize = 12.sp,
                            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            isDark = isDark
                        )
                    }
                }
            }

            if (record.contactNumber.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.clickable { onCallFarmer() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    HighlightedText(
                        text = record.contactNumber,
                        query = searchQuery,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        isDark = isDark
                    )
                }
            }

            // Specs / Quantity & Price Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val specDetail = buildString {
                    if (record.plantVariety.isNotBlank()) append(record.plantVariety)
                    if (record.rootstock.isNotBlank()) {
                        if (isNotEmpty()) append(" • ")
                        append(record.rootstock)
                    }
                    val unitLabel = if (record.serviceType.equals("Rootstocks", ignoreCase = true) || record.serviceType.contains("Rootstock", ignoreCase = true)) "Roots" else if (record.serviceType.equals("Site Visit", ignoreCase = true)) "Visits" else "Plants"
                    if (isNotEmpty()) append(" • ")
                    append("${record.quantity} $unitLabel")
                }
                Text(
                    text = specDetail.ifBlank { "${record.quantity} Units" },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Text(
                    text = "₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(totalAmount.toLong())}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 1.dp
            )

            // Uniform Bottom Action Row: 1. WhatsApp, 2. "View Details", 3. Right Arrow, 4. Edit, 5. Delete
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. WhatsApp Icon
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF14532D) else Color(0xFFDCFCE7))
                        .border(1.dp, if (isDark) Color(0xFF166534) else Color(0xFF86EFAC).copy(alpha = 0.5f), CircleShape)
                        .clickable {
                            if (record.contactNumber.isNotBlank()) {
                                showCardWhatsAppConfirm = true
                            } else {
                                Toast.makeText(context, "No contact number available", Toast.LENGTH_SHORT).show()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "WhatsApp",
                        tint = if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // 2. Text 'View Details' & 3. Right-pointing Arrow Icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDark) Color(0xFF1E293B).copy(alpha = 0.7f) else Color(0xFFF1F5F9).copy(alpha = 0.9f))
                        .border(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1).copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                        .clickable { onOpenDetail() }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "View Details",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.KeyboardArrowRight,
                        contentDescription = "View Details",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // 4. Edit Icon
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                        .border(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1), CircleShape)
                        .clickable { onEdit() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Record",
                        tint = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // 5. Delete Icon
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF451A1A) else Color(0xFFFFE4E6))
                        .border(1.dp, if (isDark) Color(0xFF7F1D1D) else Color(0xFFFECDD3), CircleShape)
                        .clickable { onDelete() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Record",
                        tint = if (isDark) Color(0xFFFCA5A5) else Color(0xFFDC2626),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    if (showCardWhatsAppConfirm) {
        val extScion = Regex("Scion:\\s*([^|\\]\n]+)").find(record.notes)?.groupValues?.get(1)?.trim() ?: ""
        val extDiameter = Regex("Root Diameter:\\s*([^|\\]\n]+)").find(record.notes)?.groupValues?.get(1)?.trim() ?: ""
        val extRootstock = if (record.rootstock.isNotBlank()) record.rootstock else (Regex("Rootstock:\\s*([^|\\]\n]+)").find(record.notes)?.groupValues?.get(1)?.trim() ?: "")

        WhatsAppTemplateDialog(
            farmerName = record.farmerName.ifBlank { "Farmer" },
            contactNumber = record.contactNumber,
            serviceType = record.serviceType,
            amountPaid = record.amountPaid,
            totalAmount = totalAmount,
            remainingBalance = remBalance,
            paymentStatus = statusLabel,
            serialNumber = if (record.serialNumber.isBlank()) "N/A" else record.serialNumber,
            plantVariety = record.plantVariety,
            scionVariety = extScion.ifBlank { record.plantVariety.ifBlank { "" } },
            rootstock = extRootstock,
            rootDiameter = extDiameter,
            quantity = "${record.quantity}",
            notes = record.notes,
            varietyLinesJson = record.varietyLinesJson,
            expectedDelivery = record.expectedDelivery,
            onDismiss = { showCardWhatsAppConfirm = false }
        )
    }
}

@Composable
private fun DetailChip(label: String, value: String) {
    val isDark = isAppInDarkMode()
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isDark) Color(0xFF1E293B) else Color(0xFFF0F4F8)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$label: ",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (isDark) Color(0xFF94A3B8) else Color.Gray
            )
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else Color(0xFF1E293B)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableRecordItem(
    record: CropRecord,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    var showWhatsAppDialog by remember { mutableStateOf(false) }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    // Swipe right -> Send via WhatsApp
                    if (record.contactNumber.isNotBlank()) {
                        showWhatsAppDialog = true
                    } else {
                        Toast.makeText(
                            context,
                            "No contact number available for ${record.farmerName.ifBlank { "Farmer" }}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    // Swipe left -> Delete
                    onDelete()
                    false
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        modifier = modifier,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            if (direction != SwipeToDismissBoxValue.Settled) {
                val isStartToEnd = direction == SwipeToDismissBoxValue.StartToEnd
                val bgColor = if (isStartToEnd) Color(0xFF16A34A) else Color(0xFFDC2626)
                val alignment = if (isStartToEnd) Alignment.CenterStart else Alignment.CenterEnd
                val icon = if (isStartToEnd) Icons.Default.Chat else Icons.Default.DeleteOutline
                val text = if (isStartToEnd) "WhatsApp" else "Delete"

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(bgColor)
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    contentAlignment = alignment
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isStartToEnd) {
                            Icon(
                                imageVector = icon,
                                contentDescription = "Swipe to Send WhatsApp",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = text,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        } else {
                            Text(
                                text = text,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Icon(
                                imageVector = icon,
                                contentDescription = "Swipe to Delete",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        },
        content = {
            content()
        }
    )

    if (showWhatsAppDialog) {
        val totalAmount = record.calculateTotalAmount()
        val remBalance = record.calculateRemainingBalance()
        val statusLabel = when {
            record.isPaymentCleared() -> "Fully Paid"
            record.amountPaid > 0 -> "Advance Paid"
            else -> "Pending"
        }
        val extScion = Regex("Scion:\\s*([^|\\]\n]+)").find(record.notes)?.groupValues?.get(1)?.trim() ?: ""
        val extDiameter = Regex("Root Diameter:\\s*([^|\\]\n]+)").find(record.notes)?.groupValues?.get(1)?.trim() ?: ""
        val extRootstock = if (record.rootstock.isNotBlank()) record.rootstock else (Regex("Rootstock:\\s*([^|\\]\n]+)").find(record.notes)?.groupValues?.get(1)?.trim() ?: "")

        WhatsAppTemplateDialog(
            farmerName = record.farmerName.ifBlank { "Farmer" },
            contactNumber = record.contactNumber,
            serviceType = record.serviceType,
            amountPaid = record.amountPaid,
            totalAmount = totalAmount,
            remainingBalance = remBalance,
            paymentStatus = statusLabel,
            serialNumber = if (record.serialNumber.isBlank()) "N/A" else record.serialNumber,
            plantVariety = record.plantVariety,
            scionVariety = extScion.ifBlank { record.plantVariety.ifBlank { "" } },
            rootstock = extRootstock,
            rootDiameter = extDiameter,
            quantity = "${record.quantity}",
            notes = record.notes,
            varietyLinesJson = record.varietyLinesJson,
            expectedDelivery = record.expectedDelivery,
            onDismiss = { showWhatsAppDialog = false }
        )
    }
}
