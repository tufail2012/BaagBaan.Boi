package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.AppDatabase
import com.example.data.FirestoreSyncManager
import com.example.data.InventoryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditInventoryItemModal(
    itemToEdit: InventoryItem? = null,
    onDismissRequest: () -> Unit,
    onSaveSuccess: () -> Unit,
    db: AppDatabase,
    isDark: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val firestoreSyncManager = remember { FirestoreSyncManager() }

    var itemName by remember { mutableStateOf(itemToEdit?.itemName ?: "") }
    var selectedCategory by remember { mutableStateOf(itemToEdit?.category ?: "Local Plants") }
    var variety by remember { mutableStateOf(itemToEdit?.variety ?: "") }
    var sku by remember { mutableStateOf(itemToEdit?.sku ?: "") }
    var initialQuantityStr by remember { mutableStateOf(itemToEdit?.initialQuantity?.toString() ?: "") }
    var currentQuantityStr by remember { mutableStateOf(itemToEdit?.currentQuantity?.toString() ?: "") }
    var unitPriceStr by remember { mutableStateOf(itemToEdit?.unitPrice?.let { if (it == 0.0) "" else it.toString() } ?: "") }
    var supplierName by remember { mutableStateOf(itemToEdit?.supplierName ?: "") }
    var supplierContact by remember { mutableStateOf(itemToEdit?.supplierContact ?: "") }
    var lowStockThresholdStr by remember { mutableStateOf(itemToEdit?.lowStockThreshold?.toString() ?: "5") }

    var isSaving by remember { mutableStateOf(false) }

    var categoryExpanded by remember { mutableStateOf(false) }
    val categories = listOf("Local Plants", "Imported Plants", "Imported Rootstock", "Garden Planning")

    var varietyExpanded by remember { mutableStateOf(false) }
    val rootstockVarieties = listOf(
        "M9T337", "MM111", "Geneva G-41", "Geneva G-11",
        "Geneva G-214", "Geneva G-969", "Geneva G-35", "Geneva G-979", "Geneva G-890"
    )

    val surfaceBg = if (isDark) Color(0xFF1E293B) else Color.White
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.90f),
            shape = RoundedCornerShape(24.dp),
            color = surfaceBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (itemToEdit == null) "Add New Item" else "Edit Inventory Item",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.testTag("close_add_inventory_modal")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = textSecondary
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                )

                // Scrollable Form Fields
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Item Name
                    OutlinedTextField(
                        value = itemName,
                        onValueChange = { itemName = it },
                        label = { Text("Item Name *") },
                        placeholder = { Text("e.g., Red Delicious Grafted Plant") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("inventory_input_item_name"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Category Dropdown
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = !categoryExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedCategory,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .testTag("inventory_category_dropdown"),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat, fontWeight = FontWeight.Medium) },
                                    onClick = {
                                        selectedCategory = cat
                                        categoryExpanded = false
                                        if (cat == "Imported Rootstock" && variety.isBlank()) {
                                            variety = rootstockVarieties.first()
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Variety (Dropdown for Rootstock or TextField for Plants)
                    if (selectedCategory == "Imported Rootstock") {
                        ExposedDropdownMenuBox(
                            expanded = varietyExpanded,
                            onExpandedChange = { varietyExpanded = !varietyExpanded }
                        ) {
                            OutlinedTextField(
                                value = variety,
                                onValueChange = { variety = it },
                                label = { Text("Variety / Rootstock Type *") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = varietyExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                                    .testTag("inventory_variety_dropdown"),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = varietyExpanded,
                                onDismissRequest = { varietyExpanded = false }
                            ) {
                                rootstockVarieties.forEach { v ->
                                    DropdownMenuItem(
                                        text = { Text(v, fontWeight = FontWeight.Medium) },
                                        onClick = {
                                            variety = v
                                            varietyExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = variety,
                            onValueChange = { variety = it },
                            label = { Text("Variety / Breed (Optional)") },
                            placeholder = { Text("e.g., Kala Kullu, Gala, Honeycrisp") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("inventory_input_variety"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // SKU Code
                    OutlinedTextField(
                        value = sku,
                        onValueChange = { sku = it },
                        label = { Text("SKU / Item Code") },
                        placeholder = { Text("e.g., LP-2026-001") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("inventory_input_sku"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Quantities Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = initialQuantityStr,
                            onValueChange = {
                                initialQuantityStr = it.filter { char -> char.isDigit() }
                                if (itemToEdit == null && currentQuantityStr.isEmpty()) {
                                    currentQuantityStr = initialQuantityStr
                                }
                            },
                            label = { Text("Initial Qty *") },
                            placeholder = { Text("100") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("inventory_input_initial_qty"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = currentQuantityStr,
                            onValueChange = { currentQuantityStr = it.filter { char -> char.isDigit() } },
                            label = { Text("Current Stock *") },
                            placeholder = { Text("100") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("inventory_input_current_qty"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Unit Price & Low Stock Threshold Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = unitPriceStr,
                            onValueChange = { unitPriceStr = it.filter { char -> char.isDigit() || char == '.' } },
                            label = { Text("Unit Price (₹)") },
                            placeholder = { Text("250.00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("inventory_input_unit_price"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = lowStockThresholdStr,
                            onValueChange = { lowStockThresholdStr = it.filter { char -> char.isDigit() } },
                            label = { Text("Low Stock Alert Limit") },
                            placeholder = { Text("5") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("inventory_input_low_stock_limit"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Supplier Name
                    OutlinedTextField(
                        value = supplierName,
                        onValueChange = { supplierName = it },
                        label = { Text("Supplier Name (Optional)") },
                        placeholder = { Text("e.g., Green Valley Nursery") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("inventory_input_supplier_name"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Supplier Contact
                    OutlinedTextField(
                        value = supplierContact,
                        onValueChange = { supplierContact = it },
                        label = { Text("Supplier Contact (Optional)") },
                        placeholder = { Text("e.g., +91 9876543210") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("inventory_input_supplier_contact"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Actions: Cancel / Save
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismissRequest,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("cancel_save_inventory_button")
                    ) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = {
                            val trimmedName = itemName.trim()
                            if (trimmedName.isEmpty()) {
                                Toast.makeText(context, "Please enter Item Name", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val initialQty = initialQuantityStr.toIntOrNull() ?: 0
                            val currentQty = currentQuantityStr.toIntOrNull() ?: initialQty
                            val price = unitPriceStr.toDoubleOrNull() ?: 0.0
                            val lowThreshold = lowStockThresholdStr.toIntOrNull() ?: 5

                            isSaving = true
                            scope.launch(Dispatchers.IO) {
                                try {
                                    val item = InventoryItem(
                                        id = itemToEdit?.id ?: 0L,
                                        itemName = trimmedName,
                                        category = selectedCategory,
                                        variety = variety.trim(),
                                        sku = sku.trim(),
                                        initialQuantity = initialQty,
                                        currentQuantity = currentQty,
                                        unitPrice = price,
                                        supplierName = supplierName.trim(),
                                        supplierContact = supplierContact.trim(),
                                        lowStockThreshold = lowThreshold,
                                        createdAt = itemToEdit?.createdAt ?: System.currentTimeMillis()
                                    )

                                    val savedId = db.inventoryDao().insertItem(item)
                                    val finalItem = if (item.id == 0L) item.copy(id = savedId) else item
                                    firestoreSyncManager.saveInventoryItem(finalItem)

                                    withContext(Dispatchers.Main) {
                                        isSaving = false
                                        Toast.makeText(context, if (itemToEdit == null) "Item added to inventory" else "Inventory updated", Toast.LENGTH_SHORT).show()
                                        onSaveSuccess()
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        isSaving = false
                                        Toast.makeText(context, "Failed to save item: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        enabled = !isSaving,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("save_inventory_button")
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        } else {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(if (itemToEdit == null) "Save Item" else "Update Item", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
