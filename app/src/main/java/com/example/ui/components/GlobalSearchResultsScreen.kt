package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CropRecord
import com.example.data.calculateRemainingBalance
import com.example.data.calculateTotalAmount
import com.example.data.isPaymentCleared
import com.example.ui.CropViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GlobalSearchResultsScreen(
    viewModel: CropViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val searchResults by viewModel.globalSearchResults.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isDark = isAppInDarkMode()
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }

    var selectedDetailRecord by remember { mutableStateOf<CropRecord?>(null) }
    var selectedCategoryFilter by remember { mutableStateOf("All Categories") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val categoryFilterOptions = listOf(
        "All Categories",
        "Local Plants",
        "Imported Plants",
        "Imported Rootstocks",
        "Site Visit",
        "Pruning"
    )

    val finalFilteredResults = remember(searchResults, selectedCategoryFilter) {
        if (selectedCategoryFilter == "All Categories") {
            searchResults
        } else {
            searchResults.filter { record ->
                when (selectedCategoryFilter) {
                    "Local Plants" -> record.serviceType.contains("Local", ignoreCase = true)
                    "Imported Plants" -> record.serviceType.contains("Imported", ignoreCase = true) && !record.serviceType.contains("Rootstock", ignoreCase = true)
                    "Imported Rootstocks" -> record.serviceType.contains("Rootstock", ignoreCase = true)
                    "Site Visit" -> record.serviceType.contains("Site Visit", ignoreCase = true)
                    "Pruning" -> record.serviceType.contains("Pruning", ignoreCase = true)
                    else -> record.serviceType.equals(selectedCategoryFilter, ignoreCase = true)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                color = if (isDark) Color(0xFF1E293B) else Color.White,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = {
                            Text(
                                text = "Search name, serial no, contact no...",
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }, modifier = Modifier.size(32.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear search",
                                        tint = if (isDark) Color.White else Color.DarkGray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        shape = CircleShape,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = if (isDark) Color.White else Color.Black
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1),
                            focusedContainerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                            unfocusedContainerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC),
                            focusedTextColor = if (isDark) Color.White else Color.Black,
                            unfocusedTextColor = if (isDark) Color.White else Color.Black
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp)
                            .focusRequester(focusRequester)
                            .testTag("global_search_input_page")
                    )
                }
            }

            // Results Counter & Category Filter Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                // Banner showing status with badge shifted left and dropdown on right
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDark) Color(0xFF1E293B) else MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left section: Title & Results counter badge shifted to the left
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Text(
                                text = if (searchQuery.isBlank()) "Global Records" else "Search Results",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = "${finalFilteredResults.size} ${if (finalFilteredResults.size == 1) "record" else "records"}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Right section: Clickable Dropdown Menu replacing old badge position
                        Box {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDark) Color(0xFF0F172A) else Color.White,
                                border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)),
                                modifier = Modifier.clickable { dropdownExpanded = true }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = selectedCategoryFilter,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color.White else Color(0xFF0F172A)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Select Category",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                            ) {
                                categoryFilterOptions.forEach { cat ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = cat,
                                                fontSize = 13.sp,
                                                fontWeight = if (selectedCategoryFilter == cat) FontWeight.Bold else FontWeight.Normal,
                                                color = if (selectedCategoryFilter == cat) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        onClick = {
                                            selectedCategoryFilter = cat
                                            dropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Results List or Empty State
            if (finalFilteredResults.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = if (isDark) Color(0xFF64748B) else Color.Gray
                        )
                        Text(
                            text = "No Matching Records",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                        Text(
                            text = if (searchQuery.isNotBlank())
                                "No records found matching '$searchQuery' across any service or sub-category."
                            else
                                "No records available in the application database.",
                            fontSize = 13.sp,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        if (searchQuery.isNotBlank()) {
                            TextButton(onClick = { viewModel.setSearchQuery("") }) {
                                Text("Clear Search Query", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 40.dp)
                ) {
                    items(finalFilteredResults, key = { it.id }) { record ->
                        SwipeableRecordItem(
                            record = record,
                            onDelete = { viewModel.deleteRecord(record) }
                        ) {
                            GlobalSearchResultCard(
                                record = record,
                                searchQuery = searchQuery,
                                isDark = isDark,
                                onCall = {
                                    if (record.contactNumber.isNotBlank()) {
                                        val intent = Intent(Intent.ACTION_DIAL).apply {
                                            data = Uri.parse("tel:${record.contactNumber}")
                                        }
                                        context.startActivity(intent)
                                    }
                                },
                                onEdit = {
                                    viewModel.loadRecordForEditing(record)
                                    onBack()
                                },
                                onDelete = { viewModel.deleteRecord(record) },
                                onOpenDetail = { selectedDetailRecord = record }
                            )
                        }
                    }
                }
            }
        }
    }

    selectedDetailRecord?.let { record ->
        BookingRecordDetailDialog(
            record = record,
            onDismiss = { selectedDetailRecord = null },
            onEdit = { rec ->
                selectedDetailRecord = null
                viewModel.loadRecordForEditing(rec)
                onBack()
            },
            onDelete = { rec ->
                selectedDetailRecord = null
                viewModel.deleteRecord(rec)
            },
            onUpdateRecord = { updatedRec ->
                selectedDetailRecord = updatedRec
                viewModel.updateRecord(updatedRec)
            }
        )
    }
}

@Composable
private fun GlobalSearchResultCard(
    record: CropRecord,
    searchQuery: String = "",
    isDark: Boolean,
    onCall: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenDetail: () -> Unit
) {
    val context = LocalContext.current
    var showWhatsAppConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val totalAmount = record.calculateTotalAmount()
    val initial = record.farmerName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "F"

    val (serviceBadgeBg, serviceBadgeText, serviceBadgeIcon) = when (record.serviceType.lowercase()) {
        "imported", "imported plants" -> Triple(
            if (isDark) Color(0xFF2E1065) else Color(0xFFF3E8FF),
            if (isDark) Color(0xFFC084FC) else Color(0xFF7E22CE),
            Icons.Default.LocalShipping
        )
        "rootstocks", "rootstock" -> Triple(
            if (isDark) Color(0xFF451A03) else Color(0xFFFFEDD5),
            if (isDark) Color(0xFFFDBA74) else Color(0xFFC2410C),
            Icons.Default.Park
        )
        "site visit" -> Triple(
            if (isDark) Color(0xFF134E4A) else Color(0xFFCCFBF1),
            if (isDark) Color(0xFF5EEAD4) else Color(0xFF0F766E),
            Icons.Outlined.Assignment
        )
        "pruning" -> Triple(
            if (isDark) Color(0xFF3B0764) else Color(0xFFFCE7F3),
            if (isDark) Color(0xFFF472B6) else Color(0xFFBE185D),
            Icons.Default.ContentCut
        )
        else -> Triple(
            if (isDark) Color(0xFF14532D) else Color(0xFFDCFCE7),
            if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D),
            Icons.Outlined.LocalFlorist
        )
    }

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
            .testTag("global_search_card_${record.id}")
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
                // Service Category Tag Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = serviceBadgeBg
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = serviceBadgeIcon,
                                contentDescription = null,
                                tint = serviceBadgeText,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = record.serviceType.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = serviceBadgeText
                            )
                        }
                    }

                    // Payment Status Tag
                    Surface(
                        shape = RoundedCornerShape(50.dp),
                        color = statusBg
                    ) {
                        Text(
                            text = statusLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusTextColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // Farmer Avatar + Name + Serial Number
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                if (isDark) Color(0xFF3B1E22) else Color(0xFFFFEBEE)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initial,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFEF4444) else Color(0xFFDC2626)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        HighlightedText(
                            text = record.farmerName.ifBlank { "Unknown Farmer" },
                            query = searchQuery,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            isDark = isDark
                        )

                        HighlightedText(
                            text = "Serial No: ${record.serialNumber}",
                            query = searchQuery,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A),
                            isDark = isDark
                        )
                    }
                }

                // Details: Phone, Variety/Rootstock, Booking Date
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (record.contactNumber.isNotBlank() || record.farmerAddress.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            HighlightedText(
                                text = "Contact: ${record.contactNumber.ifBlank { "N/A" }}",
                                query = searchQuery,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                                isDark = isDark
                            )
                            if (record.farmerAddress.isNotBlank()) {
                                HighlightedText(
                                    text = "• ${record.farmerAddress}",
                                    query = searchQuery,
                                    fontSize = 13.sp,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    isDark = isDark
                                )
                            }
                        }
                    }

                    val detailLine = buildString {
                        if (record.plantVariety.isNotBlank()) append(record.plantVariety)
                        if (record.rootstock.isNotBlank()) {
                            if (isNotEmpty()) append(" (${record.rootstock})") else append(record.rootstock)
                        }
                        if (record.quantity > 0) {
                            if (isNotEmpty()) append(" • ")
                            append("${record.quantity} Units")
                        }
                    }

                    if (detailLine.isNotBlank()) {
                        HighlightedText(
                            text = detailLine,
                            query = searchQuery,
                            fontSize = 13.sp,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            isDark = isDark
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total: ₹${totalAmount.toInt()} (Paid: ₹${record.amountPaid.toInt()})",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )

                        if (record.bookingDate.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = record.bookingDate,
                                    fontSize = 12.sp,
                                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                                )
                            }
                        }
                    }
                }

                // Action Bar: Call, WhatsApp, Edit, Delete, View Details
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (record.contactNumber.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0xFF1E3A5F) else Color(0xFFE0F2FE))
                                    .clickable { onCall() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Call",
                                    tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0xFF14532D) else Color(0xFFDCFCE7))
                                    .clickable { showWhatsAppConfirm = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Chat,
                                    contentDescription = "WhatsApp",
                                    tint = if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9))
                                .clickable { onEdit() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF451A1A) else Color(0xFFFFE4E6))
                                .clickable { showDeleteConfirm = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete",
                                tint = if (isDark) Color(0xFFFCA5A5) else Color(0xFFE11D48),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.clickable { onOpenDetail() }
                    ) {
                        Text(
                            text = "View Details",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

    if (showWhatsAppConfirm) {
        AlertDialog(
            onDismissRequest = { showWhatsAppConfirm = false },
            title = { Text("Open WhatsApp Chat", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to open WhatsApp to chat with ${record.farmerName} (${record.contactNumber})?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showWhatsAppConfirm = false
                        openWhatsAppHelper(context, record.contactNumber)
                    }
                ) {
                    Text("Open", color = Color(0xFF22C55E), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWhatsAppConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Record", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete the record for ${record.farmerName} (${record.serialNumber})?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    }
                ) {
                    Text("Delete", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun openWhatsAppHelper(context: Context, phoneNumber: String) {
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
