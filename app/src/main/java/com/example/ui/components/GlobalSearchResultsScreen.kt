package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.data.GardenPlanningEntry
import com.example.data.GlobalSearchResult
import com.example.ui.CropViewModel
import com.example.ui.GardenPlanningViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GlobalSearchResultsScreen(
    viewModel: CropViewModel,
    gardenPlanningViewModel: GardenPlanningViewModel? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val searchResults by viewModel.globalSearchResults.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isDark = isAppInDarkMode()
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }

    var selectedDetailCropRecord by remember { mutableStateOf<CropRecord?>(null) }
    var selectedDetailGardenEntry by remember { mutableStateOf<GardenPlanningEntry?>(null) }
    var selectedCategoryFilter by remember { mutableStateOf("All Categories") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val categoryFilterOptions = listOf(
        "All Categories",
        "Local Plants",
        "Imported Plants",
        "Imported Rootstocks",
        "Site Visit",
        "Pruning",
        "Garden Planning"
    )

    val finalFilteredResults = remember(searchResults, selectedCategoryFilter) {
        if (selectedCategoryFilter == "All Categories") {
            searchResults
        } else {
            searchResults.filter { result ->
                when (selectedCategoryFilter) {
                    "Local Plants" -> result.serviceType.contains("Local", ignoreCase = true)
                    "Imported Plants" -> result.serviceType.contains("Imported", ignoreCase = true) && !result.serviceType.contains("Rootstock", ignoreCase = true)
                    "Imported Rootstocks" -> result.serviceType.contains("Rootstock", ignoreCase = true)
                    "Site Visit" -> result.serviceType.contains("Site Visit", ignoreCase = true)
                    "Pruning" -> result.serviceType.contains("Pruning", ignoreCase = true)
                    "Garden Planning" -> result is GlobalSearchResult.Garden || result.serviceType.contains("Garden", ignoreCase = true)
                    else -> result.serviceType.equals(selectedCategoryFilter, ignoreCase = true)
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
                    items(
                        items = finalFilteredResults,
                        key = { item ->
                            when (item) {
                                is GlobalSearchResult.Crop -> "crop_${item.record.id}"
                                is GlobalSearchResult.Garden -> "garden_${item.entry.id}"
                            }
                        }
                    ) { item ->
                        val onDeleteItem: () -> Unit = {
                            when (item) {
                                is GlobalSearchResult.Crop -> viewModel.deleteRecord(item.record)
                                is GlobalSearchResult.Garden -> {
                                    gardenPlanningViewModel?.deleteEntry(item.entry)
                                    Unit
                                }
                            }
                        }

                        SwipeableSearchResultItem(
                            farmerName = item.farmerName,
                            serialNumber = item.serialNumber,
                            onDelete = onDeleteItem
                        ) {
                            GlobalSearchResultCard(
                                item = item,
                                searchQuery = searchQuery,
                                isDark = isDark,
                                onCall = {
                                    if (item.contactNumber.isNotBlank()) {
                                        val intent = Intent(Intent.ACTION_DIAL).apply {
                                            data = Uri.parse("tel:${item.contactNumber}")
                                        }
                                        context.startActivity(intent)
                                    }
                                },
                                onEdit = {
                                    when (item) {
                                        is GlobalSearchResult.Crop -> {
                                            viewModel.loadRecordForEditing(item.record)
                                            onBack()
                                        }
                                        is GlobalSearchResult.Garden -> {
                                            gardenPlanningViewModel?.loadEntryForEdit(item.entry)
                                            viewModel.selectServiceCategory("Garden Planning")
                                            onBack()
                                        }
                                    }
                                },
                                onDelete = onDeleteItem,
                                onOpenDetail = {
                                    when (item) {
                                        is GlobalSearchResult.Crop -> selectedDetailCropRecord = item.record
                                        is GlobalSearchResult.Garden -> selectedDetailGardenEntry = item.entry
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    selectedDetailCropRecord?.let { record ->
        BookingRecordDetailDialog(
            record = record,
            onDismiss = { selectedDetailCropRecord = null },
            onEdit = { rec ->
                selectedDetailCropRecord = null
                viewModel.loadRecordForEditing(rec)
                onBack()
            },
            onDelete = { rec ->
                selectedDetailCropRecord = null
                viewModel.deleteRecord(rec)
            },
            onUpdateRecord = { updatedRec ->
                selectedDetailCropRecord = updatedRec
                viewModel.updateRecordSync(updatedRec)
            }
        )
    }

    selectedDetailGardenEntry?.let { entry ->
        if (gardenPlanningViewModel != null) {
            GardenBookingRecordDetailDialog(
                entry = entry,
                viewModel = gardenPlanningViewModel,
                isDark = isDark,
                onDismiss = { selectedDetailGardenEntry = null },
                onEdit = { edited ->
                    selectedDetailGardenEntry = null
                    gardenPlanningViewModel.loadEntryForEdit(edited)
                    viewModel.selectServiceCategory("Garden Planning")
                    onBack()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableSearchResultItem(
    farmerName: String,
    serialNumber: String,
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
        }
    ) {
        content()
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete this booking?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete the record for $farmerName ($serialNumber)? It will be moved to the Recycle Bin.") },
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

@Composable
private fun GlobalSearchResultCard(
    item: GlobalSearchResult,
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

    val totalAmount = item.totalAmount
    val initial = item.farmerName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "F"

    val (serviceBadgeBg, serviceBadgeText, serviceBadgeIcon) = when (item.serviceType.lowercase()) {
        "imported", "imported plants" -> Triple(
            if (isDark) Color(0xFF2E1065) else Color(0xFFF3E8FF),
            if (isDark) Color(0xFFC084FC) else Color(0xFF7E22CE),
            Icons.Default.LocalShipping
        )
        "rootstocks", "rootstock" -> Triple(
            if (isDark) Color(0xFF451A03) else Color(0xFFFFEDD5),
            if (isDark) Color(0xFFFDBA74) else Color(0xFFC2410C),
            Icons.Default.Spa
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
        "garden planning", "garden" -> Triple(
            if (isDark) Color(0xFF1E3A8A) else Color(0xFFDBEAFE),
            if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8),
            Icons.Default.Park
        )
        else -> Triple(
            if (isDark) Color(0xFF14532D) else Color(0xFFDCFCE7),
            if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D),
            Icons.Outlined.LocalFlorist
        )
    }

    val (statusBg, statusTextColor) = when {
        item.isPaymentCleared -> {
            if (isDark) Color(0xFF1B382B) to Color(0xFF6EE7B7)
            else Color(0xFFDCFCE7) to Color(0xFF15803D)
        }
        item.amountPaid > 0 -> {
            if (isDark) Color(0xFF382A13) to Color(0xFFFDE047)
            else Color(0xFFFEF3C7) to Color(0xFFB45309)
        }
        else -> {
            if (isDark) Color(0xFF381A1A) to Color(0xFFFCA5A5)
            else Color(0xFFFEE2E2) to Color(0xFFB91C1C)
        }
    }

    val statusLabel = when {
        item.isPaymentCleared -> "Fully Paid"
        item.amountPaid > 0 -> "Advance Paid"
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
            .testTag("global_search_card_${item.id}")
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
                            item.isPaymentCleared -> Color(0xFF16A34A)
                            item.amountPaid > 0 -> Color(0xFFE65100)
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
                // Header tag
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
                                text = item.serviceType.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = serviceBadgeText
                            )
                        }
                    }

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

                // Farmer avatar, name, serial number
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
                            text = item.farmerName.ifBlank { "Unknown Farmer" },
                            query = searchQuery,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            isDark = isDark
                        )

                        HighlightedText(
                            text = "Serial No: ${item.serialNumber}",
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
                    if (item.contactNumber.isNotBlank() || item.farmerAddress.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            HighlightedText(
                                text = "Contact: ${item.contactNumber.ifBlank { "N/A" }}",
                                query = searchQuery,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                                isDark = isDark
                            )
                            if (item.farmerAddress.isNotBlank()) {
                                HighlightedText(
                                    text = "• ${item.farmerAddress}",
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

                    val detailLine = when (item) {
                        is GlobalSearchResult.Crop -> {
                            val rec = item.record
                            buildString {
                                if (rec.plantVariety.isNotBlank()) append(rec.plantVariety)
                                if (rec.rootstock.isNotBlank()) {
                                    if (isNotEmpty()) append(" (${rec.rootstock})") else append(rec.rootstock)
                                }
                                if (rec.quantity > 0) {
                                    if (isNotEmpty()) append(" • ")
                                    append("${rec.quantity} Units")
                                }
                            }
                        }
                        is GlobalSearchResult.Garden -> {
                            val entry = item.entry
                            buildString {
                                if (entry.plantVariety.isNotBlank()) append(entry.plantVariety)
                                if (entry.rootStock.isNotBlank()) {
                                    if (isNotEmpty()) append(" (${entry.rootStock})") else append(entry.rootStock)
                                }
                                if (entry.totalKanalArea > 0) {
                                    if (isNotEmpty()) append(" • ")
                                    append("${entry.totalKanalArea} Kanals (${(entry.totalKanalArea * entry.plantsPerKanal).toInt()} Plants)")
                                }
                            }
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

                    val bookingDate = when (item) {
                        is GlobalSearchResult.Crop -> item.record.bookingDate
                        is GlobalSearchResult.Garden -> item.entry.bookingDate
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total: ₹${totalAmount.toInt()} (Paid: ₹${item.amountPaid.toInt()})",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )

                        if (bookingDate.isNotBlank()) {
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
                                    text = bookingDate,
                                    fontSize = 11.5.sp,
                                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(
                    color = (if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)).copy(alpha = 0.6f),
                    thickness = 1.dp
                )

                // Bottom Action Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. WhatsApp Button
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isDark) Color(0xFF14532D) else Color(0xFFDCFCE7),
                        modifier = Modifier.clickable {
                            if (item.contactNumber.isNotBlank()) {
                                showWhatsAppConfirm = true
                            } else {
                                Toast.makeText(context, "No contact number available", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = "WhatsApp",
                                tint = if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "WhatsApp",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D)
                            )
                        }
                    }

                    // Right Side: View Details, Edit, Delete
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
                                contentDescription = "View Details",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Edit Icon
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

                        // Delete Icon
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
                                contentDescription = "Delete Record",
                                tint = if (isDark) Color(0xFFFCA5A5) else Color(0xFFE11D48),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showWhatsAppConfirm) {
        AlertDialog(
            onDismissRequest = { showWhatsAppConfirm = false },
            title = { Text("Open WhatsApp Chat", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to open WhatsApp to chat with ${item.farmerName} (${item.contactNumber})?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showWhatsAppConfirm = false
                        openWhatsAppHelper(context, item.contactNumber)
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
            title = { Text("Delete this booking?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete the record for ${item.farmerName} (${item.serialNumber})? It will be moved to the Recycle Bin.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
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
