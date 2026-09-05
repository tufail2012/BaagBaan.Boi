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
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.HazeMaterials
import com.example.ui.components.BrandedPullToRefreshBox
import com.example.data.CropRecord
import com.example.data.calculateRemainingBalance
import com.example.data.calculateTotalAmount
import com.example.data.calculateTotalAmountMultiVariety
import com.example.data.isPaymentCleared
import com.example.data.parseVarietyLines
import com.example.ui.CropViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FarmerRecordsScreen(
    viewModel: CropViewModel,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = LocalAppGlassHazeState.current
) {
    val effectiveHazeState = hazeState ?: LocalAppGlassHazeState.current
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

    val accentColorHex by viewModel.accentColorHex.collectAsState()
    val paletteColor = remember(accentColorHex) {
        runCatching { Color(android.graphics.Color.parseColor(accentColorHex)) }.getOrNull()
            ?: Color(0xFF10B981)
    }

    val animatedItemIds = remember(bookTitle, selectedPaymentFilter, searchQuery) { mutableSetOf<Any>() }

    // Dynamic Computation of 4 Summary Metrics across records (respecting active filters/search)
    val totalPayment = remember(records) {
        records.sumOf { it.calculateTotalAmountMultiVariety() }
    }
    val receivedPayment = remember(records) {
        records.sumOf { it.amountPaid }
    }
    val pendingPayment = remember(records) {
        records.sumOf { if (it.isCancelled || it.paymentStatus.equals("Cancelled", ignoreCase = true)) 0.0 else it.calculateRemainingBalance() }
    }
    val totalQuantity = remember(records) {
        records.sumOf { record ->
            val lines = parseVarietyLines(record.varietyLinesJson)
            if (lines.isNotEmpty()) {
                lines.sumOf { it.effectiveQuantity }
            } else {
                record.quantity
            }
        }
    }
    val quantityUnitLabel = remember(selectedService) {
        when {
            selectedService.equals("Site Visit", ignoreCase = true) -> "Visits"
            selectedService.equals("Rootstocks", ignoreCase = true) || selectedService.contains("Rootstock", ignoreCase = true) -> "Roots"
            else -> "Plants"
        }
    }

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
        // Unified container with hazeSource so list items dynamically blur under sticky top bar and bottom nav
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (effectiveHazeState != null) {
                        Modifier.hazeSource(state = effectiveHazeState)
                    } else Modifier
                )
        ) {
            // Scrollable content (Entire screen in unified scroll flow)
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 110.dp)
            ) {
                // Unified Controls Header: Sub-Tabs, Switcher, Header Pill, Search Bar, and 4 Summary Metric Cards
                // Sits directly on the single continuous background canvas and scrolls together with records
                item(key = "records_header_controls") {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 1. Dedicated Sub-Tabs for Pruning & Rootstocks
                        if (selectedService.equals("Pruning", ignoreCase = true)) {
                            PruningSubTabs(
                                selectedSubTab = selectedPruningSubTab,
                                onSelectSubTab = { viewModel.selectPruningSubTab(it) },
                                accentColor = paletteColor,
                                hazeState = effectiveHazeState,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else if (selectedService.equals("Rootstocks", ignoreCase = true)) {
                            RootstockSubTabs(
                                selectedSubTab = selectedRootstockSubTab,
                                selectedGenevaOption = selectedGenevaOption,
                                onSelectSubTab = { subTab, genevaOpt ->
                                    viewModel.selectRootstockSubTab(subTab, genevaOpt)
                                },
                                accentColor = paletteColor,
                                hazeState = effectiveHazeState,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // 2. Liquid Glass Switcher (New Entry / Records)
                        val viewMode by viewModel.viewMode.collectAsState()
                        val isEditing = viewModel.editingRecordId.collectAsState().value != null
                        if (effectiveHazeState != null) {
                            AgriSegmentedControl(
                                selectedMode = viewMode,
                                onModeSelected = { viewModel.setViewMode(it) },
                                hazeState = effectiveHazeState,
                                newEntryLabel = if (isEditing) "Edit Entry" else "New Entry",
                                recordsLabel = "Records (${records.size})",
                                accentColor = paletteColor,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // 3. Sub-Header Recording Book Pill
                        RecordingBookHeader(
                            title = bookTitle,
                            count = records.size,
                            hazeState = effectiveHazeState
                        )
                        SearchBarWithStatusFilter(
                            searchQuery = searchQuery,
                            onSearchQueryChange = { viewModel.setRecordsSearchQuery(it) },
                            selectedFilter = selectedPaymentFilter,
                            onFilterSelected = { viewModel.setPaymentFilter(it) },
                            placeholderText = "Search by farmer name, phone, serial no...",
                            isDark = isDark,
                            testTagPrefix = "crop_search",
                            hazeState = effectiveHazeState
                        )
                        // 2x2 Metric Grid Component placed directly below Search Bar
                        RecordsSummaryMetricCards(
                            totalPayment = totalPayment,
                            receivedPayment = receivedPayment,
                            pendingPayment = pendingPayment,
                            totalQuantity = totalQuantity,
                            quantityLabel = quantityUnitLabel,
                            isDark = isDark,
                            paletteAccent = paletteColor,
                            hazeState = effectiveHazeState
                        )
                    }
                }

                // Records List, Shimmer Skeleton Loading, or Empty State
                if (records.isEmpty()) {
                    if (isInitialLoading) {
                        items(4) {
                            SkeletonCard(isDark = isDark, lineCount = 4, hasActionRow = true)
                        }
                    } else {
                        item {
                            StaggeredEntranceWrapper(
                                itemId = "empty_records_${bookTitle}",
                                index = 0,
                                animatedItemIds = animatedItemIds
                            ) {
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
                                    onOpenDetail = { selectedDetailRecord = record },
                                    hazeState = effectiveHazeState
                                )
                            }
                        }
                    }
                }
            }

            // Target Component 4: Floating Action Button (FAB)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 96.dp, end = 16.dp)
                    .zIndex(8f)
            ) {
                Surface(
                    onClick = { viewModel.setViewMode(0) },
                    shape = RoundedCornerShape(percent = 50),
                    color = Color.Transparent,
                    modifier = Modifier
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(percent = 50),
                            spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        .then(
                            if (effectiveHazeState != null) {
                                Modifier.hazeEffect(state = effectiveHazeState, style = HazeMaterials.thin())
                            } else Modifier
                        )
                        .background(
                            brush = Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.88f else 0.92f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.68f else 0.76f)
                                )
                            ),
                            shape = RoundedCornerShape(percent = 50)
                        )
                        .border(
                            BorderStroke(
                                1.dp,
                                Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.80f),
                                        Color.White.copy(alpha = 0.25f)
                                    )
                                )
                            ),
                            shape = RoundedCornerShape(percent = 50)
                        )
                        .testTag("fab_add_crop_record")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Entry",
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                        Text(
                            text = "New Entry",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Scroll to Top quick action button
            AnimatedVisibility(
                visible = listState.firstVisibleItemIndex > 2,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 152.dp, end = 20.dp)
                    .zIndex(8f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .then(
                            if (effectiveHazeState != null) {
                                Modifier.hazeEffect(state = effectiveHazeState, style = HazeMaterials.thin())
                            } else Modifier
                        )
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = if (isDark) 0.22f else 0.50f),
                                    Color.White.copy(alpha = if (isDark) 0.08f else 0.20f)
                                )
                            ),
                            CircleShape
                        )
                        .border(
                            BorderStroke(
                                1.dp,
                                Brush.verticalGradient(
                                    listOf(Color.White.copy(alpha = 0.70f), Color.White.copy(alpha = 0.20f))
                                )
                            ),
                            CircleShape
                        )
                        .clip(CircleShape)
                        .clickable {
                            coroutineScope.launch {
                                listState.animateScrollToItem(0)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Scroll to top",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
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
    onOpenDetail: () -> Unit,
    hazeState: HazeState? = null
) {
    val context = LocalContext.current
    val isDark = isAppInDarkMode()
    var showCardWhatsAppConfirm by remember { mutableStateOf(false) }

    val totalAmount = record.calculateTotalAmount()
    val remBalance = record.calculateRemainingBalance()

    val initial = record.farmerName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "F"
    val avatarBgColor = MaterialTheme.colorScheme.primary
    val cardShape = RoundedCornerShape(22.dp)

    // Liquid Frosted Glass Cards (List Items)
    // Refraction Fill: 0.25 opacity tinted with active palette
    val cardFillBrush = Brush.verticalGradient(
        if (isDark) listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
            Color(0xFF0F172A).copy(alpha = 0.55f),
            Color(0xFF0F172A).copy(alpha = 0.45f)
        )
        else listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            Color.White.copy(alpha = 0.25f),
            Color.White.copy(alpha = 0.18f)
        )
    )

    // Specular Edge Highlight: 1px solid rgba(255, 255, 255, 0.3) tinted border
    val cardBorderBrush = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = if (isDark) 0.35f else 0.45f),
            MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
            Color.White.copy(alpha = 0.15f)
        )
    )

    val statusLabel = when {
        record.isCancelled -> "Cancelled"
        record.isReceived -> "Received"
        record.isPaymentCleared() -> "Fully Paid"
        record.amountPaid > 0 -> "Advance Paid"
        else -> "Pending"
    }

    // Status badges: Semi-transparent frosted pills with vibrant accent text
    val (statusBadgeBg, statusBadgeBorder, statusBadgeText) = when {
        record.isCancelled -> Triple(
            if (isDark) Color(0xFF64748B).copy(alpha = 0.22f) else Color(0xFF64748B).copy(alpha = 0.16f),
            if (isDark) Color(0xFF64748B).copy(alpha = 0.45f) else Color(0xFF64748B).copy(alpha = 0.35f),
            if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
        )
        record.isReceived -> Triple(
            if (isDark) Color(0xFF06B6D4).copy(alpha = 0.22f) else Color(0xFF06B6D4).copy(alpha = 0.16f),
            if (isDark) Color(0xFF06B6D4).copy(alpha = 0.45f) else Color(0xFF06B6D4).copy(alpha = 0.35f),
            if (isDark) Color(0xFF22D3EE) else Color(0xFF0891B2)
        )
        record.isPaymentCleared() -> Triple(
            if (isDark) Color(0xFF10B981).copy(alpha = 0.22f) else Color(0xFF10B981).copy(alpha = 0.16f),
            if (isDark) Color(0xFF10B981).copy(alpha = 0.45f) else Color(0xFF10B981).copy(alpha = 0.35f),
            if (isDark) Color(0xFF34D399) else Color(0xFF059669)
        )
        record.amountPaid > 0 -> Triple(
            if (isDark) Color(0xFFF59E0B).copy(alpha = 0.22f) else Color(0xFFF59E0B).copy(alpha = 0.16f),
            if (isDark) Color(0xFFF59E0B).copy(alpha = 0.45f) else Color(0xFFF59E0B).copy(alpha = 0.35f),
            if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
        )
        else -> Triple(
            if (isDark) Color(0xFFEF4444).copy(alpha = 0.22f) else Color(0xFFEF4444).copy(alpha = 0.16f),
            if (isDark) Color(0xFFEF4444).copy(alpha = 0.45f) else Color(0xFFEF4444).copy(alpha = 0.35f),
            if (isDark) Color(0xFFF87171) else Color(0xFFDC2626)
        )
    }

    /* CSS glassmorphism:
     * background: rgba(255, 255, 255, 0.25);
     * -webkit-backdrop-filter: blur(12px);
     * backdrop-filter: blur(12px);
     * border: 1px solid rgba(255, 255, 255, 0.3);
     * box-shadow: 0 4px 20px 0 rgba(0, 0, 0, 0.05);
     */
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = cardShape,
                spotColor = Color.Black.copy(alpha = 0.05f),
                ambientColor = Color.Black.copy(alpha = 0.02f)
            )
            .then(
                if (hazeState != null) {
                    Modifier.hazeEffect(
                        state = hazeState,
                        style = HazeStyle(
                            blurRadius = 12.dp,
                            tints = listOf(
                                HazeTint(MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.12f else 0.08f))
                            ),
                            backgroundColor = Color.Transparent
                        )
                    )
                } else Modifier
            )
            .clip(cardShape)
            .background(cardFillBrush, shape = cardShape)
            .border(BorderStroke(1.dp, cardBorderBrush), shape = cardShape)
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
                                shape = RoundedCornerShape(8.dp),
                                color = if (isDark) Color(0xFF334155).copy(alpha = 0.60f) else Color(0xFFE2E8F0).copy(alpha = 0.75f),
                                border = BorderStroke(
                                    1.dp,
                                    Brush.verticalGradient(
                                        listOf(Color.White.copy(alpha = 0.50f), Color.White.copy(alpha = 0.15f))
                                    )
                                )
                            ) {
                                Text(
                                    text = "#${record.serialNumber}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A),
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
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

                // Payment Status Badge: Semi-transparent frosted pill with vibrant accent text
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(statusBadgeBg, shape = RoundedCornerShape(12.dp))
                        .border(BorderStroke(1.dp, statusBadgeBorder), shape = RoundedCornerShape(12.dp))
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusLabel,
                        color = statusBadgeText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 2.dp),
                color = if (isDark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.45f)
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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (remBalance > 0 && !record.isCancelled) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDark) Color(0xFFEF4444).copy(alpha = 0.20f) else Color(0xFFEF4444).copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.30f))
                        ) {
                            Text(
                                text = "Due: ₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(remBalance.toLong())}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFFCA5A5) else Color(0xFFDC2626),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = "₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(totalAmount.toLong())}",
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            HorizontalDivider(
                color = if (isDark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.45f),
                thickness = 1.dp
            )

            // Target Component 4: Uniform Bottom Action Row with Frosted Liquid Glass Buttons
            // 1. WhatsApp, 2. "View Details", 3. Edit, 4. Delete
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. WhatsApp Action
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                if (isDark) listOf(Color(0xFF14532D).copy(alpha = 0.45f), Color(0xFF14532D).copy(alpha = 0.25f))
                                else listOf(Color(0xFF16A34A).copy(alpha = 0.25f), Color(0xFF16A34A).copy(alpha = 0.15f))
                            ),
                            CircleShape
                        )
                        .border(
                            BorderStroke(
                                1.dp,
                                Brush.verticalGradient(
                                    listOf(Color(0xFF86EFAC).copy(alpha = 0.6f), Color(0xFF16A34A).copy(alpha = 0.25f))
                                )
                            ),
                            CircleShape
                        )
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
                        modifier = Modifier.size(17.dp)
                    )
                }

                // 2. "View Details" Frosted Pill with Right Arrow
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(
                            Brush.verticalGradient(
                                if (isDark) listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), Color(0xFF0F172A).copy(alpha = 0.50f))
                                else listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), Color.White.copy(alpha = 0.25f))
                            ),
                            RoundedCornerShape(percent = 50)
                        )
                        .border(
                            BorderStroke(
                                1.dp,
                                Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = if (isDark) 0.35f else 0.45f),
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                                        Color.White.copy(alpha = 0.15f)
                                    )
                                )
                            ),
                            RoundedCornerShape(percent = 50)
                        )
                        .clickable { onOpenDetail() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
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
                        modifier = Modifier.size(16.dp)
                    )
                }

                // 3. Edit Action
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                if (isDark) listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.08f))
                                else listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.15f))
                            ),
                            CircleShape
                        )
                        .border(
                            BorderStroke(
                                1.dp,
                                Brush.verticalGradient(
                                    listOf(Color.White.copy(alpha = 0.45f), Color.White.copy(alpha = 0.15f))
                                )
                            ),
                            CircleShape
                        )
                        .clickable { onEdit() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Record",
                        tint = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155),
                        modifier = Modifier.size(17.dp)
                    )
                }

                // 4. Delete Action
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                if (isDark) listOf(Color(0xFF451A1A).copy(alpha = 0.45f), Color(0xFF451A1A).copy(alpha = 0.25f))
                                else listOf(Color(0xFFDC2626).copy(alpha = 0.25f), Color(0xFFDC2626).copy(alpha = 0.15f))
                            ),
                            CircleShape
                        )
                        .border(
                            BorderStroke(
                                1.dp,
                                Brush.verticalGradient(
                                    listOf(Color(0xFFFECDD3).copy(alpha = 0.6f), Color(0xFFDC2626).copy(alpha = 0.25f))
                                )
                            ),
                            CircleShape
                        )
                        .clickable { onDelete() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Record",
                        tint = if (isDark) Color(0xFFFCA5A5) else Color(0xFFDC2626),
                        modifier = Modifier.size(17.dp)
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
                val alignment = if (isStartToEnd) Alignment.CenterStart else Alignment.CenterEnd
                val icon = if (isStartToEnd) Icons.Default.Chat else Icons.Default.DeleteOutline
                val text = if (isStartToEnd) "WhatsApp" else "Delete"
                val trayShape = RoundedCornerShape(22.dp)
                val isDark = isAppInDarkMode()

                /* CSS glassmorphism:
                 * background: rgba(..., 0.25);
                 * -webkit-backdrop-filter: blur(12px);
                 * backdrop-filter: blur(12px);
                 * border: 1px solid rgba(255, 255, 255, 0.3);
                 * box-shadow: 0 4px 20px 0 rgba(0, 0, 0, 0.05);
                 */
                val trayBrush = if (isStartToEnd) {
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFF16A34A).copy(alpha = if (isDark) 0.35f else 0.25f),
                            Color(0xFF16A34A).copy(alpha = if (isDark) 0.20f else 0.14f),
                            Color.Transparent
                        )
                    )
                } else {
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            Color(0xFFDC2626).copy(alpha = if (isDark) 0.20f else 0.14f),
                            Color(0xFFDC2626).copy(alpha = if (isDark) 0.35f else 0.25f)
                        )
                    )
                }
                val trayBorderBrush = Brush.horizontalGradient(
                    if (isStartToEnd) {
                        listOf(
                            Color.White.copy(alpha = 0.35f),
                            Color(0xFF86EFAC).copy(alpha = 0.30f),
                            Color.Transparent
                        )
                    } else {
                        listOf(
                            Color.Transparent,
                            Color(0xFFFECDD3).copy(alpha = 0.30f),
                            Color.White.copy(alpha = 0.35f)
                        )
                    }
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .shadow(
                            elevation = 4.dp,
                            shape = trayShape,
                            spotColor = Color.Black.copy(alpha = 0.05f),
                            ambientColor = Color.Black.copy(alpha = 0.02f)
                        )
                        .clip(trayShape)
                        .background(trayBrush, shape = trayShape)
                        .border(BorderStroke(1.dp, trayBorderBrush), shape = trayShape)
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
