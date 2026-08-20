package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.AppDatabase
import com.example.data.FirestoreSyncManager
import com.example.data.InventoryItem
import com.example.data.InventoryStockManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

data class DbgMatchedBookingDiagnostic(
    val id: Long,
    val serialNumber: String,
    val farmerName: String,
    val plantVariety: String,
    val rootstock: String,
    val serviceType: String,
    val quantity: Int,
    val paymentStatus: String,
    val matchedItemName: String,
    val matchedSku: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryManagementDialog(
    onDismissRequest: () -> Unit,
    db: AppDatabase,
    isDark: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val firestoreSyncManager = remember { FirestoreSyncManager() }

    val allItems by db.inventoryDao().getAllItems().collectAsState(initial = emptyList())

    var isInitialLoading by remember { mutableStateOf(true) }
    LaunchedEffect(allItems) {
        if (allItems.isNotEmpty()) {
            isInitialLoading = false
        } else {
            kotlinx.coroutines.delay(300)
            isInitialLoading = false
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("All") }

    var showAddEditModal by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<InventoryItem?>(null) }
    var itemToDelete by remember { mutableStateOf<InventoryItem?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }

    var showRecalculateConfirmDialog by remember { mutableStateOf(false) }
    var isRecalculating by remember { mutableStateOf(false) }
    var dbgDiagnostics by remember { mutableStateOf<List<DbgMatchedBookingDiagnostic>>(emptyList()) }
    var dbgItemFound by remember { mutableStateOf<InventoryItem?>(null) }
    var isLoadingDiagnostics by remember { mutableStateOf(false) }

    LaunchedEffect(showRecalculateConfirmDialog) {
        if (showRecalculateConfirmDialog) {
            isLoadingDiagnostics = true
            withContext(Dispatchers.IO) {
                try {
                    val allBookings = db.cropRecordDao().getAllRecordsList()
                    val allInvItems = db.inventoryDao().getAllItemsSync()
                    val targetDbgItem = allInvItems.firstOrNull {
                        it.sku.contains("DBG", ignoreCase = true) ||
                        it.itemName.contains("DBG", ignoreCase = true) ||
                        it.variety.contains("DBG", ignoreCase = true)
                    }
                    dbgItemFound = targetDbgItem

                    val matchedList = mutableListOf<DbgMatchedBookingDiagnostic>()
                    allBookings.forEach { booking ->
                        if (InventoryStockManager.isStockDeductible(booking)) {
                            val matched = InventoryStockManager.findMatchingInventoryItem(db.inventoryDao(), booking)
                            if (matched != null && (
                                matched.id == targetDbgItem?.id ||
                                matched.sku.contains("DBG", ignoreCase = true) ||
                                matched.itemName.contains("DBG", ignoreCase = true) ||
                                matched.variety.contains("DBG", ignoreCase = true)
                            )) {
                                matchedList.add(
                                    DbgMatchedBookingDiagnostic(
                                        id = booking.id,
                                        serialNumber = booking.serialNumber,
                                        farmerName = booking.farmerName,
                                        plantVariety = booking.plantVariety,
                                        rootstock = booking.rootstock,
                                        serviceType = booking.serviceType,
                                        quantity = booking.quantity,
                                        paymentStatus = booking.paymentStatus,
                                        matchedItemName = matched.itemName,
                                        matchedSku = matched.sku
                                    )
                                )
                            }
                        }
                    }
                    dbgDiagnostics = matchedList

                    android.util.Log.d("DBG_Diagnostic", "=== DBG Matched Bookings (${matchedList.size} records, total qty = ${matchedList.sumOf { it.quantity }}) ===")
                    matchedList.forEach { b ->
                        android.util.Log.d("DBG_Diagnostic", "Booking ID: ${b.id} | Serial: ${b.serialNumber} | Farmer: '${b.farmerName}' | Variety: '${b.plantVariety}' | Rootstock: '${b.rootstock}' | Service: '${b.serviceType}' | Qty: ${b.quantity} | Status: '${b.paymentStatus}' -> Matched: '${b.matchedItemName}' [SKU: ${b.matchedSku}]")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("DBG_Diagnostic", "Error computing DBG diagnostics: ${e.message}", e)
                } finally {
                    isLoadingDiagnostics = false
                }
            }
        }
    }

    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }

    val filteredItems = remember(allItems, searchQuery, selectedCategoryFilter) {
        allItems.filter { item ->
            val matchesCategory = when (selectedCategoryFilter) {
                "All" -> true
                "Low Stock" -> item.isLowStock()
                else -> item.category.equals(selectedCategoryFilter, ignoreCase = true)
            }

            val query = searchQuery.trim().lowercase()
            val matchesSearch = query.isEmpty() ||
                    item.itemName.lowercase().contains(query) ||
                    item.variety.lowercase().contains(query) ||
                    item.sku.lowercase().contains(query) ||
                    item.supplierName.lowercase().contains(query)

            matchesCategory && matchesSearch
        }
    }

    val totalStock = remember(allItems) { allItems.sumOf { it.currentQuantity } }
    val lowStockCount = remember(allItems) { allItems.count { it.isLowStock() } }
    val totalSold = remember(allItems) { allItems.sumOf { it.getItemsSold() } }

    val bgSurface = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val cardBg = if (isDark) Color(0xFF1E293B) else Color.White
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            shape = RoundedCornerShape(24.dp),
            color = bgSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Inventory2,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Inventory Management",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            Text(
                                text = "${allItems.size} item(s) in catalog",
                                fontSize = 12.sp,
                                color = textSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.testTag("close_inventory_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Dashboard Summary Cards Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Card 1: Total Stock
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = null,
                                    tint = Color(0xFF3B82F6),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Total Stock",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textSecondary
                                )
                            }
                            Text(
                                text = "$totalStock",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF3B82F6)
                            )
                        }
                    }

                    // Card 2: Low Stock Alerts
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (lowStockCount > 0) {
                                if (isDark) Color(0xFF451A03) else Color(0xFFFEF3C7)
                            } else cardBg
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (lowStockCount > 0) Color(0xFFF59E0B) else (if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (lowStockCount > 0) Color(0xFFD97706) else textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Low Stock Alerts",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (lowStockCount > 0) Color(0xFFB45309) else textSecondary
                                )
                            }
                            Text(
                                text = "$lowStockCount",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (lowStockCount > 0) Color(0xFFD97706) else textPrimary
                            )
                        }
                    }

                    // Card 3: Items Sold
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Items Sold",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textSecondary
                                )
                            }
                            Text(
                                text = "$totalSold",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF10B981)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Search Bar & Action Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search inventory...") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = textSecondary)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search", tint = textSecondary)
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("inventory_search_input"),
                        shape = RoundedCornerShape(16.dp)
                    )

                    OutlinedButton(
                        onClick = {
                            showRecalculateConfirmDialog = true
                        },
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isDark) Color(0xFF1E293B) else Color.White
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.testTag("recalculate_stock_button")
                    ) {
                        Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Recalculate Stock", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = textPrimary)
                    }

                    Button(
                        onClick = {
                            itemToEdit = null
                            showAddEditModal = true
                        },
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                        modifier = Modifier.testTag("add_new_inventory_item_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Item", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Category Filter Chips
                val filterChips = listOf("All", "Low Stock", "Local Plants", "Imported Plants", "Imported Rootstock", "Garden Planning")
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filterChips) { filterName ->
                        val isSelected = selectedCategoryFilter == filterName
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategoryFilter = filterName },
                            label = {
                                Text(
                                    text = filterName + if (filterName == "Low Stock" && lowStockCount > 0) " ($lowStockCount)" else "",
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White,
                                containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                                labelColor = if (isDark) Color.White else Color(0xFF334155)
                            ),
                            modifier = Modifier.testTag("inventory_filter_chip_${filterName.lowercase().replace(" ", "_")}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Inventory List, Skeleton Loading, or Empty State
                if (filteredItems.isEmpty()) {
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
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Inventory,
                                    contentDescription = null,
                                    modifier = Modifier.size(56.dp),
                                    tint = if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1)
                                )
                                Text(
                                    text = if (allItems.isEmpty()) "No inventory items yet" else "No matching items found",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textSecondary
                                )
                                Text(
                                    text = if (allItems.isEmpty()) "Click '+ Add Item' to build your inventory stock catalog." else "Try adjusting your search query or selected filter.",
                                    fontSize = 13.sp,
                                    color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
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
                            items(filteredItems, key = { it.id }) { item ->
                                InventoryItemCard(
                                    item = item,
                                    isDark = isDark,
                                    onEdit = {
                                        itemToEdit = item
                                        showAddEditModal = true
                                    },
                                    onDelete = {
                                        itemToDelete = item
                                    },
                                    onQuantityAdjust = { delta ->
                                        scope.launch(Dispatchers.IO) {
                                            val oldQty = item.currentQuantity
                                            val newQty = (item.currentQuantity + delta).coerceAtLeast(0)
                                            val updated = item.copy(currentQuantity = newQty)
                                            db.inventoryDao().updateItem(updated)
                                            firestoreSyncManager.saveInventoryItem(updated)
                                            com.example.data.InventoryStockManager.checkAndNotifyLowStock(context, oldQty, updated)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Dialog for Add / Edit
    if (showAddEditModal) {
        AddEditInventoryItemModal(
            itemToEdit = itemToEdit,
            onDismissRequest = { showAddEditModal = false },
            onSaveSuccess = { showAddEditModal = false },
            db = db,
            isDark = isDark
        )
    }

    // Modal Confirmation Dialog for Delete
    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = {
                Text(
                    text = "Delete Inventory Item",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text("Are you sure you want to delete '${itemToDelete?.itemName}'? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = itemToDelete
                        itemToDelete = null
                        if (target != null) {
                            scope.launch(Dispatchers.IO) {
                                db.inventoryDao().deleteItem(target)
                                firestoreSyncManager.deleteInventoryItem(target.id)
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Item deleted", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Delete Permanently", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal Confirmation Dialog for Recalculate Stock
    if (showRecalculateConfirmDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isRecalculating) showRecalculateConfirmDialog = false
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Recalculate All Stock",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "This will recalculate all stock levels based on total bookings, overwriting any manual adjustments. Continue?",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Diagnostic Section for DBG item
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.BugReport,
                                    contentDescription = null,
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "DBG Diagnostic Report",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isDark) Color(0xFFFCD34D) else Color(0xFFB45309)
                                )
                            }

                            if (isLoadingDiagnostics) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(vertical = 8.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Text("Analyzing DBG booking matches...", fontSize = 12.sp, color = textSecondary)
                                }
                            } else {
                                val target = dbgItemFound
                                if (target != null) {
                                    Text(
                                        text = "Target Item: ${target.itemName} (SKU: ${target.sku}, Cat: ${target.category})\nInitial: ${target.initialQuantity} | Current: ${target.currentQuantity}",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = textPrimary
                                    )
                                } else {
                                    Text(
                                        text = "Target Item: No item with 'DBG' found in catalog.",
                                        fontSize = 11.5.sp,
                                        color = textSecondary
                                    )
                                }

                                val totalMatchedQty = dbgDiagnostics.sumOf { it.quantity }
                                Text(
                                    text = "Matched Deductible Bookings: ${dbgDiagnostics.size} (Total Qty: $totalMatchedQty)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (totalMatchedQty > 0) Color(0xFFEF4444) else Color(0xFF10B981)
                                )

                                if (dbgDiagnostics.isNotEmpty()) {
                                    HorizontalDivider(
                                        color = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1),
                                        thickness = 0.5.dp
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        dbgDiagnostics.forEachIndexed { index, b ->
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(
                                                        color = if (isDark) Color(0xFF0F172A) else Color.White,
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .border(
                                                        1.dp,
                                                        if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                                                        RoundedCornerShape(8.dp)
                                                    )
                                                    .padding(8.dp)
                                            ) {
                                                Text(
                                                    text = "${index + 1}. Serial: ${b.serialNumber.ifEmpty { "ID #${b.id}" }}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.5.sp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = "Farmer: ${b.farmerName}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = textPrimary
                                                )
                                                Text(
                                                    text = "Variety: '${b.plantVariety}' | Rootstock: '${b.rootstock}'",
                                                    fontSize = 11.sp,
                                                    color = textSecondary
                                                )
                                                Text(
                                                    text = "Service: ${b.serviceType} | Qty: ${b.quantity} (Status: ${b.paymentStatus})",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = textPrimary
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "No bookings currently matched to DBG by findMatchingInventoryItem.",
                                        fontSize = 11.5.sp,
                                        color = textSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isRecalculating = true
                        scope.launch(Dispatchers.IO) {
                            try {
                                com.example.data.InventoryStockManager.recalculateAllStock(
                                    database = db,
                                    firestoreSyncManager = firestoreSyncManager,
                                    context = context
                                )
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Stock recalculation completed successfully", Toast.LENGTH_LONG).show()
                                    isRecalculating = false
                                    showRecalculateConfirmDialog = false
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Error recalculating stock: ${e.message}", Toast.LENGTH_LONG).show()
                                    isRecalculating = false
                                }
                            }
                        }
                    },
                    enabled = !isRecalculating,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (isRecalculating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Recalculating...")
                    } else {
                        Text("Recalculate Now", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRecalculateConfirmDialog = false },
                    enabled = !isRecalculating
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun InventoryItemCard(
    item: InventoryItem,
    isDark: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onQuantityAdjust: (delta: Int) -> Unit
) {
    val isLow = item.isLowStock()
    val isOut = item.isOutOfStock()

    val badgeBg = when {
        isOut -> Color(0xFFEF4444).copy(alpha = 0.15f)
        isLow -> Color(0xFFF59E0B).copy(alpha = 0.15f)
        else -> Color(0xFF10B981).copy(alpha = 0.15f)
    }
    val badgeText = when {
        isOut -> Color(0xFFDC2626)
        isLow -> Color(0xFFD97706)
        else -> Color(0xFF059669)
    }
    val statusLabel = when {
        isOut -> "OUT OF STOCK"
        isLow -> "LOW STOCK"
        else -> "IN STOCK"
    }

    val cardBg = if (isDark) Color(0xFF1E293B) else Color.White
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isLow || isOut) 1.5.dp else 1.dp,
            color = if (isOut) Color(0xFFEF4444) else if (isLow) Color(0xFFF59E0B) else (if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Row: Category Badge + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = item.category.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    if (item.variety.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                        ) {
                            Text(
                                text = item.variety,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeBg
                ) {
                    Text(
                        text = statusLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = badgeText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Item Name & SKU
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.itemName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    modifier = Modifier.weight(1f)
                )

                if (item.sku.isNotBlank()) {
                    Text(
                        text = "SKU: ${item.sku}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = textSecondary
                    )
                }
            }

            // Quantity & Price Info Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Current / Initial Stock",
                        fontSize = 11.sp,
                        color = textSecondary
                    )
                    Text(
                        text = "${item.currentQuantity} / ${item.initialQuantity} units",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeText
                    )
                }

                if (item.unitPrice > 0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Unit Price",
                            fontSize = 11.sp,
                            color = textSecondary
                        )
                        Text(
                            text = "₹${item.unitPrice}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                    }
                }
            }

            // Supplier Info if available
            if (item.supplierName.isNotBlank()) {
                Text(
                    text = "Supplier: ${item.supplierName}" + if (item.supplierContact.isNotBlank()) " (${item.supplierContact})" else "",
                    fontSize = 12.sp,
                    color = textSecondary
                )
            }

            // Action Buttons Row: Quick Adjustments, Edit, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stock Adjuster Buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Adjust Stock:", fontSize = 12.sp, color = textSecondary)
                    Spacer(modifier = Modifier.width(6.dp))

                    FilledIconButton(
                        onClick = { onQuantityAdjust(-1) },
                        modifier = Modifier
                            .size(30.dp)
                            .testTag("decrease_stock_button_${item.id}"),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                        )
                    ) {
                        Text("-", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textPrimary)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    FilledIconButton(
                        onClick = { onQuantityAdjust(1) },
                        modifier = Modifier
                            .size(30.dp)
                            .testTag("increase_stock_button_${item.id}"),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        )
                    ) {
                        Text("+", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }

                // Edit & Delete Action Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.testTag("edit_inventory_button_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Item",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_inventory_button_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Item",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
