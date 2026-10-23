@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import kotlinx.coroutines.delay
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.AppDatabase
import com.example.data.CropRecord
import com.example.data.GardenPlanningEntry
import com.example.data.calculateRemainingBalance
import com.example.data.calculateTotalAmount
import com.example.ui.theme.getAppDimBackgroundBrush
import com.example.util.PdfReceiptManager
import com.example.util.SerialNumberUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

data class PendingPaymentItem(
    val id: Long,
    val serialNumber: String,
    val farmerName: String,
    val contactNumber: String,
    val amountDue: Double,
    val totalCost: Double,
    val amountPaid: Double,
    val paymentStatus: String,
    val serviceType: String,
    val source: String // "CROP" or "GARDEN"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentRemindersDialog(
    onDismiss: () -> Unit,
    viewModel: com.example.ui.CropViewModel? = null,
    customPaletteColor: Color? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context, scope) }
    val isDark = isAppInDarkMode()

    val vmAccentHex by (viewModel?.accentColorHex ?: kotlinx.coroutines.flow.MutableStateFlow(null)).collectAsState()
    val parsedPaletteColor = remember(vmAccentHex, customPaletteColor) {
        if (customPaletteColor != null) return@remember customPaletteColor
        val hex = vmAccentHex
        if (hex != null) {
            try {
                Color(android.graphics.Color.parseColor(hex))
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedServiceTypes by remember { mutableStateOf(setOf<String>()) }
    var selectedPaidBands by remember { mutableStateOf(setOf<Int>()) }
    var serviceTypeDropdownExpanded by remember { mutableStateOf(false) }
    var paidBandDropdownExpanded by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var selectedReceiptItem by remember { mutableStateOf<PendingPaymentItem?>(null) }

    var loadErrorTrace by remember { mutableStateOf<String?>(null) }

    // Load Crop Records and Garden Planning Entries
    val cropRecords by db.cropRecordDao().getAllRecords().collectAsState(initial = emptyList())
    val gardenEntries by db.gardenPlanningDao().getAllEntries().collectAsState(initial = emptyList())

    var isInitialLoading by remember { mutableStateOf(true) }
    LaunchedEffect(cropRecords, gardenEntries) {
        if (cropRecords.isNotEmpty() || gardenEntries.isNotEmpty()) {
            isInitialLoading = false
        } else {
            kotlinx.coroutines.delay(300)
            isInitialLoading = false
        }
    }

    val numberFormat = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }

    // Predefined service type order
    val preferredServiceOrder = remember {
        listOf(
            "Local Plants",
            "Imported",
            "Rootstocks",
            "Pruning",
            "Site Visit",
            "Garden Planning"
        )
    }

    val paidBandOptions = remember {
        (10..100 step 10).toList()
    }

    // Helper function for paid percentage band (clamped to 0..100, 10-point bands)
    fun getPaidBand(item: PendingPaymentItem): Int {
        if (item.totalCost <= 0.0) return 0
        val paidShare = ((item.amountPaid / item.totalCost) * 100.0).coerceIn(0.0, 100.0)
        return (((paidShare + 1e-6) / 10.0).toInt() * 10).coerceAtMost(100)
    }

    // All pending payment items extracted from DB records
    val allPendingRecords = remember(cropRecords, gardenEntries) {
        try {
            val list = mutableListOf<PendingPaymentItem>()

            // 1. Process Crop Records
            cropRecords.forEach { crop ->
                val remDue = crop.calculateRemainingBalance()
                val isPending = !crop.paymentStatus.equals("Fully Paid", ignoreCase = true) && remDue > 0.01
                if (isPending) {
                    list.add(
                        PendingPaymentItem(
                            id = crop.id,
                            serialNumber = crop.serialNumber,
                            farmerName = crop.farmerName,
                            contactNumber = crop.contactNumber,
                            amountDue = remDue,
                            totalCost = crop.calculateTotalAmount(),
                            amountPaid = crop.amountPaid,
                            paymentStatus = crop.paymentStatus,
                            serviceType = if (crop.serviceType.isNotBlank()) crop.serviceType else "Crop Order",
                            source = "CROP"
                        )
                    )
                }
            }

            // 2. Process Garden Planning Entries
            gardenEntries.forEach { garden ->
                val remDue = (garden.totalCost - garden.amountPaid).coerceAtLeast(0.0)
                val isPending = !garden.paymentStatus.equals("Fully Paid", ignoreCase = true) && remDue > 0.01
                if (isPending) {
                    list.add(
                        PendingPaymentItem(
                            id = garden.id,
                            serialNumber = garden.serialNumber,
                            farmerName = garden.farmerName,
                            contactNumber = garden.contactNumber,
                            amountDue = remDue,
                            totalCost = garden.totalCost,
                            amountPaid = garden.amountPaid,
                            paymentStatus = garden.paymentStatus,
                            serviceType = "Garden Planning",
                            source = "GARDEN"
                        )
                    )
                }
            }

            list
        } catch (e: Exception) {
            loadErrorTrace = "Error loading payment reminders: ${e.message}\n${e.stackTraceToString()}"
            emptyList()
        }
    }

    // Service type options: always includes known types in preferred order, plus any other distinct pending types alphabetically
    val serviceTypeOptions = remember(allPendingRecords) {
        val pendingTypes = allPendingRecords.map { it.serviceType }.filter { it.isNotBlank() }.distinct()
        val combined = (preferredServiceOrder + pendingTypes).distinct()
        combined.sortedWith { a, b ->
            val idxA = preferredServiceOrder.indexOf(a)
            val idxB = preferredServiceOrder.indexOf(b)
            when {
                idxA != -1 && idxB != -1 -> idxA.compareTo(idxB)
                idxA != -1 -> -1
                idxB != -1 -> 1
                else -> a.compareTo(b, ignoreCase = true)
            }
        }
    }

    // Filtered items based on search query, service type filter, and paid % band filter
    val pendingItems = remember(allPendingRecords, searchQuery, selectedServiceTypes, selectedPaidBands) {
        val q = searchQuery.trim().lowercase()
        val filteredList = allPendingRecords.filter { item ->
            val matchesSearch = if (q.isBlank()) true else {
                item.farmerName.lowercase().contains(q) ||
                        item.serialNumber.lowercase().contains(q) ||
                        item.contactNumber.contains(q) ||
                        item.serviceType.lowercase().contains(q)
            }
            val matchesServiceType = if (selectedServiceTypes.isEmpty()) true else {
                item.serviceType in selectedServiceTypes
            }
            val matchesPaidBand = if (selectedPaidBands.isEmpty()) true else {
                getPaidBand(item) in selectedPaidBands
            }

            matchesSearch && matchesServiceType && matchesPaidBand
        }
        filteredList.sortedWith { i1, i2 ->
            SerialNumberUtils.compareSerials(i1.serialNumber, i2.serialNumber)
        }
    }

    val totalOutstanding = remember(pendingItems) {
        pendingItems.sumOf { it.amountDue }
    }

    fun openWhatsAppReminder(item: PendingPaymentItem) {
        val formattedDue = numberFormat.format(item.amountDue)
        val farmerDisplayName = if (item.farmerName.isBlank()) "Valued Customer" else item.farmerName
        val message = com.example.data.MessageTemplateRepository.renderTemplate(
            templateId = "quick_payment_reminder",
            data = mapOf(
                "farmerName" to farmerDisplayName,
                "serialNumber" to item.serialNumber,
                "serviceType" to item.serviceType,
                "amountDue" to formattedDue
            )
        )

        com.example.util.WhatsAppHelper.openWhatsAppChat(
            context = context,
            rawPhone = item.contactNumber,
            messageText = message,
            onInvalidNumber = {
                Toast.makeText(context, "No valid phone number for ${item.farmerName}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    fun makePhoneCall(phone: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot make phone call", Toast.LENGTH_SHORT).show()
        }
    }

    // PDF Receipt Preview & Print Dialog
    selectedReceiptItem?.let { item ->
        FarmerPaymentReceiptDialog(
            item = item,
            cropRecords = cropRecords,
            gardenEntries = gardenEntries,
            onDismiss = { selectedReceiptItem = null }
        )
    }

    val paymentAccent = parsedPaletteColor ?: MaterialTheme.colorScheme.primary
    val paymentBgBrush = remember(isDark, paymentAccent) {
        getAppDimBackgroundBrush(paymentAccent, isDark = isDark)
    }

    BackHandler(onBack = onDismiss)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(paymentBgBrush)
            .testTag("payment_reminders_dialog")
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Wide Pill-Shaped Glass Header (Matching Dashboard Header Style)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .frostedGlassChrome(
                        isDark = isDark,
                        accentColor = paymentAccent,
                        shape = RoundedCornerShape(percent = 50)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("payment_reminders_back_button")
                        ) {
                            ThemedIcon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = paymentAccent
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(paymentAccent.copy(alpha = if (isDark) 0.25f else 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            ThemedIcon(
                                imageVector = Icons.Default.Payment,
                                contentDescription = "Payment Icon",
                                tint = paymentAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Payment Reminders",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    letterSpacing = (-0.3).sp
                                ),
                                color = if (isDark) Color.White else Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${pendingItems.size} Pending • ${numberFormat.format(totalOutstanding)} Dues",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Outstanding Summary Card - Liquid Glass
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .glassCardBackground(
                                cornerRadius = 16.dp,
                                accentColor = Color(0xFFEF5350),
                                isDark = isDark,
                            ),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Transparent
                        ),
                        border = null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isDark) Color(0xFFEF5350).copy(alpha = 0.20f)
                                        else Color(0xFFD32F2F).copy(alpha = 0.12f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                ThemedIcon(
                                    imageVector = Icons.Default.Payment,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFFEF5350) else Color(0xFFD32F2F),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Total Outstanding Dues",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                                CountUpText(
                                    targetValue = totalOutstanding,
                                    formatter = { numberFormat.format(it) },
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFEF5350) else Color(0xFFD32F2F)
                                )
                            }
                        }
                    }

                    // Search input - Liquid Glass Input Field
                    val searchShape = RoundedCornerShape(16.dp)
                    AppOutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .bringIntoViewOnFocus()
                            .glassCardBackground(
                                isDark = isDark,
                                accentColor = paymentAccent,
                                shape = searchShape,
                            )
                            .testTag("payment_reminders_search_input"),
                        placeholder = {
                            Text(
                                text = "Search by farmer name, serial or phone...",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leadingIcon = {
                            ThemedIcon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(0.dp)
                            ) {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        ThemedIcon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear search",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                VoiceSearchIconButton(
                                    onQueryChange = { searchQuery = capitalizeWordsNaturally(it) },
                                    accentColor = MaterialTheme.colorScheme.primary,
                                    isDark = isDark,
                                    buttonSize = 34.dp,
                                    iconSize = 18.dp,
                                    testTag = "payment_reminders_voice_btn"
                                )
                            }
                        },
                        shape = searchShape,
                        singleLine = true,
                        colors = elevatedInputFieldColors(isDark = isDark, accentColor = paymentAccent)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Filters header & Clear all button
                    val totalSelectedFilters = selectedServiceTypes.size + selectedPaidBands.size
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Filters",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (totalSelectedFilters > 0) {
                            TextButton(
                                onClick = {
                                    selectedServiceTypes = emptySet()
                                    selectedPaidBands = emptySet()
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier
                                    .height(28.dp)
                                    .testTag("payment_reminders_clear_all_filters")
                            ) {
                                Text(
                                    text = "Clear all ($totalSelectedFilters)",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Spacer(modifier = Modifier.height(6.dp))

                    // Dropdowns side-by-side
                    val dropdownContainerBg = if (isDark) {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    } else {
                        Color(0xFFF1F5F9)
                    }
                    val dropdownBorderColor = if (isDark) {
                        Color.White.copy(alpha = 0.12f)
                    } else {
                        Color(0xFFCBD5E1)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Dropdown 1: Service type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                        ) {
                            val serviceSummary = when (selectedServiceTypes.size) {
                                0 -> "All"
                                1, 2 -> {
                                    // Keep preferred display order in summary
                                    serviceTypeOptions.filter { it in selectedServiceTypes }.joinToString(", ")
                                }
                                else -> "${selectedServiceTypes.size} selected"
                            }

                            var serviceTypeAnchorBounds by remember { mutableStateOf(Rect.Zero) }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = dropdownContainerBg,
                                border = BorderStroke(1.dp, dropdownBorderColor),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onGloballyPositioned { serviceTypeAnchorBounds = it.boundsInWindow() }
                                    .clickable { serviceTypeDropdownExpanded = true }
                                    .testTag("payment_reminders_service_type_dropdown")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = "Service type",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = serviceSummary,
                                            fontSize = 12.sp,
                                            fontWeight = if (selectedServiceTypes.isNotEmpty()) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selectedServiceTypes.isNotEmpty()) paymentAccent else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    ThemedIcon(
                                        imageVector = if (serviceTypeDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            MorphPopupMenu(
                                expanded = serviceTypeDropdownExpanded,
                                anchorBounds = serviceTypeAnchorBounds,
                                onDismissRequest = { serviceTypeDropdownExpanded = false },
                                modifier = Modifier.testTag("payment_reminders_service_type_menu")
                            ) {
                                serviceTypeOptions.forEach { type ->
                                    val isChecked = type in selectedServiceTypes
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Checkbox(
                                                    checked = isChecked,
                                                    onCheckedChange = null,
                                                    colors = CheckboxDefaults.colors(
                                                        checkedColor = paymentAccent
                                                    )
                                                )
                                                Text(
                                                    text = type,
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedServiceTypes = if (isChecked) {
                                                selectedServiceTypes - type
                                            } else {
                                                selectedServiceTypes + type
                                            }
                                        },
                                        modifier = Modifier.testTag("service_type_item_${type.lowercase().replace(" ", "_")}")
                                    )
                                }
                            }
                        }

                        // Dropdown 2: Paid %
                        Box(
                            modifier = Modifier
                                .weight(1f)
                        ) {
                            val paidSummary = when (selectedPaidBands.size) {
                                0 -> "All"
                                1, 2 -> {
                                    // Order numerically
                                    paidBandOptions.filter { it in selectedPaidBands }.joinToString(", ") { "$it%" }
                                }
                                else -> "${selectedPaidBands.size} selected"
                            }

                            var paidBandAnchorBounds by remember { mutableStateOf(Rect.Zero) }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = dropdownContainerBg,
                                border = BorderStroke(1.dp, dropdownBorderColor),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onGloballyPositioned { paidBandAnchorBounds = it.boundsInWindow() }
                                    .clickable { paidBandDropdownExpanded = true }
                                    .testTag("payment_reminders_paid_band_dropdown")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = "Paid %",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = paidSummary,
                                            fontSize = 12.sp,
                                            fontWeight = if (selectedPaidBands.isNotEmpty()) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selectedPaidBands.isNotEmpty()) paymentAccent else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    ThemedIcon(
                                        imageVector = if (paidBandDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            MorphPopupMenu(
                                expanded = paidBandDropdownExpanded,
                                anchorBounds = paidBandAnchorBounds,
                                onDismissRequest = { paidBandDropdownExpanded = false },
                                modifier = Modifier.testTag("payment_reminders_paid_band_menu")
                            ) {
                                paidBandOptions.forEach { band ->
                                    val isChecked = band in selectedPaidBands
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Checkbox(
                                                    checked = isChecked,
                                                    onCheckedChange = null,
                                                    colors = CheckboxDefaults.colors(
                                                        checkedColor = paymentAccent
                                                    )
                                                )
                                                Text(
                                                    text = "$band%",
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedPaidBands = if (isChecked) {
                                                selectedPaidBands - band
                                            } else {
                                                selectedPaidBands + band
                                            }
                                        },
                                        modifier = Modifier.testTag("paid_band_item_$band")
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (pendingItems.isEmpty()) {
                        if (isInitialLoading) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(4) {
                                    SkeletonCard(isDark = isDark, lineCount = 3, hasActionRow = true)
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    ThemedIcon(
                                        imageVector = Icons.Default.HourglassTop,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = if (searchQuery.isBlank() && selectedServiceTypes.isEmpty() && selectedPaidBands.isEmpty()) "No pending payments found" else "No matching pending records",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        BrandedPullToRefreshBox(
                            isRefreshing = isRefreshing,
                            onRefresh = {
                                if (isRefreshing) return@BrandedPullToRefreshBox
                                isRefreshing = true
                                scope.launch {
                                    try {
                                        delay(400)
                                    } catch (_: Exception) {
                                    } finally {
                                        isRefreshing = false
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(pendingItems, key = { "${it.source}_${it.id}" }) { item ->
                                    PendingPaymentRow(
                                        item = item,
                                        numberFormat = numberFormat,
                                        isDark = isDark,
                                        accentColor = paymentAccent,
                                        onOpenReceipt = { selectedReceiptItem = item },
                                        onPrintReceipt = {
                                            PdfReceiptManager.printReceipt(
                                                context = context,
                                                item = item,
                                                cropRecords = cropRecords,
                                                gardenEntries = gardenEntries
                                            )
                                        },
                                        onSendWhatsApp = { openWhatsAppReminder(item) },
                                        onCall = { makePhoneCall(item.contactNumber) }
                                    )
                                }
                                item {
                                    Spacer(modifier = Modifier.height(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
}

@Composable
fun PendingPaymentRow(
    item: PendingPaymentItem,
    numberFormat: NumberFormat,
    isDark: Boolean,
    accentColor: Color = Color(0xFFF59E0B),
    onOpenReceipt: () -> Unit,
    onPrintReceipt: () -> Unit,
    onSendWhatsApp: () -> Unit,
    onCall: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassCardBackground(
                cornerRadius = 16.dp,
                accentColor = accentColor,
                isDark = isDark,
            )
            .testTag("pending_payment_row_${item.serialNumber}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        border = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Row 1: Header (Farmer Name + Service Badge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.farmerName.ifBlank { "Unknown Farmer" },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Serial #${item.serialNumber}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Service tag chip - Theme-Aware Colors
                val isGardenOrPlants = item.source == "GARDEN" ||
                        item.serviceType.contains("Plant", ignoreCase = true) ||
                        item.serviceType.contains("Rootstock", ignoreCase = true)

                val chipBg = if (isGardenOrPlants) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isDark) 0.35f else 0.55f)
                } else {
                    if (isDark) Color(0xFF0C4A6E).copy(alpha = 0.40f) else Color(0xFFE0F2FE)
                }
                val chipText = if (isGardenOrPlants) {
                    MaterialTheme.colorScheme.primary
                } else {
                    if (isDark) Color(0xFF7DD3FC) else Color(0xFF0369A1)
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = chipBg
                ) {
                    Text(
                        text = item.serviceType,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = chipText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 2: Outstanding Amount & Contact Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Amount Due",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = numberFormat.format(item.amountDue),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFEF5350) else Color(0xFFD32F2F)
                    )
                }

                if (item.contactNumber.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.15f else 0.08f))
                            .clickable { onCall() }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        ThemedIcon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Call",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = item.contactNumber,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Row 3: Actions (Print PDF Receipt, WhatsApp Reminder)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Receipt PDF / Print Action Button
                OutlinedButton(
                    onClick = onOpenReceipt,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isDark) MaterialTheme.colorScheme.outline
                        else MaterialTheme.colorScheme.outlineVariant
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("receipt_pdf_button_${item.serialNumber}")
                ) {
                    ThemedIcon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = "PDF Receipt",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "PDF Receipt",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (item.contactNumber.isBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "No number",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                } else {
                    Button(
                        onClick = onSendWhatsApp,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF25D366)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(38.dp)
                            .testTag("send_whatsapp_reminder_${item.serialNumber}")
                    ) {
                        ThemedIcon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "WhatsApp",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
