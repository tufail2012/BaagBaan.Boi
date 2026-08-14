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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.example.data.CropRecord
import com.example.data.calculateRemainingBalance
import com.example.data.calculateTotalAmount
import com.example.data.isPaymentCleared
import com.example.ui.CropViewModel
import com.example.ui.theme.AgriGreenLight
import com.example.ui.theme.AgriGreenPrimary
import com.example.ui.theme.AgriGreenPrimary
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FarmerRecordsScreen(
    viewModel: CropViewModel,
    modifier: Modifier = Modifier
) {
    val records by viewModel.filteredRecords.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedPaymentFilter by viewModel.selectedPaymentFilter.collectAsState()
    val selectedService by viewModel.selectedService.collectAsState()
    val selectedPruningSubTab by viewModel.selectedPruningSubTab.collectAsState()
    val selectedRootstockSubTab by viewModel.selectedRootstockSubTab.collectAsState()
    val selectedGenevaOption by viewModel.selectedGenevaOption.collectAsState()
    var selectedDetailRecord by remember { mutableStateOf<CropRecord?>(null) }

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

    var isInitialLoading by remember { mutableStateOf(true) }
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

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // 1. Active Recording Book Header Banner
        item {
            RecordingBookHeader(
                title = bookTitle,
                count = records.size
            )
        }

        // 2. Search Bar with Payment Status Filter Dropdown
        stickyHeader {
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxWidth()
            ) {
                SearchBarWithStatusFilter(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    selectedFilter = selectedPaymentFilter,
                    onFilterSelected = { viewModel.setPaymentFilter(it) },
                    placeholderText = "Search by farmer name, phone, serial no...",
                    isDark = isDark,
                    testTagPrefix = "crop_search",
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }

        // 3. Summary Cards
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

        // Records List or Empty State
        if (records.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isInitialLoading) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
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
            items(records, key = { it.id }) { record ->
                SwipeableRecordItem(
                    record = record,
                    onDelete = { viewModel.deleteRecord(record) }
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
                        onDelete = { viewModel.deleteRecord(record) },
                        onOpenDetail = { selectedDetailRecord = record }
                    )
                }
            }
        }
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
                value = "₹${numberFmt.format(totalPayment.toLong())}",
                icon = Icons.Default.AccountBalanceWallet,
                accentColor = MaterialTheme.colorScheme.primary,
                bgColor = if (isDark) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.weight(1f)
            )

            // Card 2: Received Payment
            SummaryCardItem(
                title = "Received Payment",
                value = "₹${numberFmt.format(receivedPayment.toLong())}",
                icon = Icons.Default.CheckCircle,
                accentColor = Color(0xFF2E7D32),
                bgColor = if (isDark) Color(0xFF1B2E1B) else Color(0xFFE8F5E9),
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
                value = "₹${numberFmt.format(pendingPayment.toLong())}",
                icon = Icons.Default.HourglassTop,
                accentColor = Color(0xFFC62828),
                bgColor = if (isDark) Color(0xFF331C1C) else Color(0xFFFFEBEE),
                modifier = Modifier.weight(1f)
            )

            // Card 4: Total Quantity (Hidden on Pruning and Site Visit screens)
            if (!isPruning && !isSiteVisit) {
                SummaryCardItem(
                    title = "Total Quantity",
                    value = "${numberFmt.format(totalQuantity)} Units",
                    icon = Icons.Default.Inventory2,
                    accentColor = Color(0xFF0288D1),
                    bgColor = if (isDark) Color(0xFF1A2A38) else Color(0xFFE1F5FE),
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
    value: String,
    icon: ImageVector,
    accentColor: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        tonalElevation = 1.dp
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
                    .background(accentColor.copy(alpha = 0.15f)),
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = value,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun openWhatsApp(context: Context, phoneNumber: String) {
    if (phoneNumber.isNotBlank()) {
        val cleanNumber = phoneNumber.replace("[^0-9]".toRegex(), "")
        val formattedNumber = if (cleanNumber.length == 10) "91$cleanNumber" else cleanNumber
        val url = "https://api.whatsapp.com/send?phone=$formattedNumber"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
            context.startActivity(dialIntent)
        }
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
    var showCardDeleteConfirm by remember { mutableStateOf(false) }

    val totalAmount = record.calculateTotalAmount()
    val remBalance = record.calculateRemainingBalance()

    val initial = record.farmerName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "F"

    val (statusBg, statusTextColor) = when {
        record.isPaymentCleared() -> {
            if (isDark) Color(0xFF1B382B) to Color(0xFF6EE7B7)
            else Color(0xFFDCFCE7) to Color(0xFF15803D)
        }
        record.amountPaid > 0 -> {
            if (isDark) Color(0xFF382A13) to Color(0xFFFDE047)
            else Color(0xFFFEF3C7) to Color(0xFFB45309)
        }
        else -> {
            if (isDark) Color(0xFF381A1A) to Color(0xFFFCA5A5)
            else Color(0xFFFEE2E2) to Color(0xFFB91C1C)
        }
    }
    val statusLabel = when {
        record.isPaymentCleared() -> "Fully Paid"
        record.amountPaid > 0 -> "Advance Paid"
        else -> "Pending"
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1E293B) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable { onOpenDetail() }
            .testTag("farmer_record_card_${record.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Left vertical accent strip
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(
                        color = when {
                            record.isPaymentCleared() -> Color(0xFF16A34A)
                            record.amountPaid > 0 -> Color(0xFFE65100)
                            else -> Color(0xFFDC2626)
                        }
                    )
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Top Row: Avatar + Name + Booking Date & Status Tag
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        // Profile Avatar showing First Letter of Farmer's Name
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isDark) Color(0xFF3B1E22) else Color(0xFFFFEBEE)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initial,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFEF4444) else Color(0xFFDC2626)
                            )
                        }

                        Column {
                            // Farmer Name
                            HighlightedText(
                                text = record.farmerName.ifBlank { "Unknown Farmer" },
                                query = searchQuery,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                isDark = isDark
                            )

                            // Booking Date with Calendar Icon
                            if (record.bookingDate.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = "Booking Date",
                                        tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = record.bookingDate,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                                    )
                                }
                            }
                        }
                    }

                    // Payment Status Tag
                    Surface(
                        shape = RoundedCornerShape(50.dp),
                        color = statusBg
                    ) {
                        Text(
                            text = statusLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusTextColor,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }

                // Middle Row 1: Serial Number
                if (record.serialNumber.isNotBlank()) {
                    HighlightedText(
                        text = "Serial: ${record.serialNumber}",
                        query = searchQuery,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
                        isDark = isDark
                    )
                }

                // Middle Row 2: Quantity and Total Amount
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val amountText = if (record.amountPaid > 0 && !record.isPaymentCleared()) {
                        "${record.quantity} Units • ₹${totalAmount.toLong()}"
                    } else {
                        "${record.quantity} Units • ₹${totalAmount.toLong()}"
                    }
                    Text(
                        text = amountText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Contact Number & Address
                val contactAndAddress = buildString {
                    if (record.contactNumber.isNotBlank()) {
                        append(record.contactNumber)
                    }
                    if (record.farmerAddress.isNotBlank()) {
                        if (isNotEmpty()) append(" • ")
                        append(record.farmerAddress)
                    }
                }

                if (contactAndAddress.isNotBlank()) {
                    HighlightedText(
                        text = contactAndAddress,
                        query = searchQuery,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        isDark = isDark,
                        modifier = Modifier.clickable { onCallFarmer() }
                    )
                }

                HorizontalDivider(
                    color = (if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)).copy(alpha = 0.6f),
                    thickness = 1.dp
                )

                // Uniform Bottom Action Row: 1. WhatsApp, 2. "View Details", 3. Right Arrow, 4. Edit, 5. Delete
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. WhatsApp Icon
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF14532D) else Color(0xFFDCFCE7))
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
                            .clickable { onOpenDetail() }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
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
                            .clickable { onEdit() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Record",
                            tint = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // 5. Delete Icon
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF451A1A) else Color(0xFFFFE4E6))
                            .clickable { showCardDeleteConfirm = true },
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
    }

    if (showCardDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showCardDeleteConfirm = false },
            title = { Text("Delete this booking?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete the record for '${record.farmerName}' (${if (record.serialNumber.isNotBlank()) record.serialNumber else record.serviceType})? It will be moved to the Recycle Bin.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCardDeleteConfirm = false
                        onDelete()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCardDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showCardWhatsAppConfirm) {
        WhatsAppTemplateDialog(
            farmerName = record.farmerName.ifBlank { "Farmer" },
            contactNumber = record.contactNumber,
            serviceType = record.serviceType,
            amountPaid = record.amountPaid,
            totalAmount = totalAmount,
            remainingBalance = remBalance,
            paymentStatus = statusLabel,
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
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart || dismissValue == SwipeToDismissBoxValue.StartToEnd) {
                showDeleteConfirmation = true
                false
            } else {
                false
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
            val alignment = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                else -> Alignment.CenterEnd
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFFDC2626))
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                contentAlignment = alignment
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Swipe to Delete",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Delete",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        },
        content = {
            content()
        }
    )

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete this booking?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete the record for '${record.farmerName}' (${if (record.serialNumber.isNotBlank()) record.serialNumber else record.serviceType})? It will be moved to the Recycle Bin.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onDelete()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
