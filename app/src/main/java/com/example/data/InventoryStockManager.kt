package com.example.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.notifications.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

sealed class StockValidationResult {
    data class Success(
        val item: InventoryItem?,
        val remainingStock: Int?
    ) : StockValidationResult()

    data class InsufficientStock(
        val item: InventoryItem,
        val availableStock: Int,
        val requestedQty: Int
    ) : StockValidationResult() {
        val errorMessage: String get() = "Insufficient stock. Only $availableStock units are available."
    }
}

object InventoryStockManager {
    private const val TAG = "InventoryStockManager"

    fun normalizeCategory(serviceType: String): String {
        val lower = serviceType.lowercase().trim()
        return when {
            lower.contains("rootstock") -> "Imported Rootstock"
            lower.contains("imported") -> "Imported Plants"
            lower.contains("garden") -> "Garden Planning"
            lower.contains("local") -> "Local Plants"
            else -> serviceType.trim()
        }
    }

    /**
     * Determines whether stock should be deducted for this booking.
     * Non-inventory services ("Site Visit", "Pruning") and non-confirmed statuses
     * ("Draft", "Cancelled", "Canceled", "Failed", "Void") must NOT deduct stock.
     */
    fun isStockDeductible(serviceType: String, paymentStatus: String, notes: String = ""): Boolean {
        val sLower = serviceType.lowercase().trim()
        if (sLower.contains("site visit") || sLower.contains("pruning")) {
            return false
        }
        val pLower = paymentStatus.lowercase().trim()
        if (pLower.contains("cancel") || pLower.contains("draft") || pLower.contains("fail") || pLower.contains("void")) {
            return false
        }
        val nLower = notes.lowercase().trim()
        if (nLower.contains("[status: cancelled]") || nLower.contains("[status: canceled]") || nLower.contains("[status: draft]")) {
            return false
        }
        return true
    }

    fun isStockDeductible(record: CropRecord): Boolean {
        return isStockDeductible(record.serviceType, record.paymentStatus, record.notes)
    }

    /**
     * Finds the matching inventory item using ID, SKU, category, variety, rootstock, or name matching.
     */
    suspend fun findMatchingInventoryItem(
        inventoryDao: InventoryDao,
        serviceType: String,
        plantVariety: String,
        rootstock: String = "",
        notes: String = "",
        explicitId: Long? = null,
        explicitSku: String? = null
    ): InventoryItem? {
        try {
            // 1. Explicit ID
            if (explicitId != null && explicitId > 0L) {
                val byId = inventoryDao.getItemById(explicitId)
                if (byId != null) return byId
            }

            // 2. [InventoryID: X] pattern in notes
            val idInNotes = Regex("""\[InventoryID:\s*(\d+)\]""", RegexOption.IGNORE_CASE)
                .find(notes)?.groupValues?.get(1)?.toLongOrNull()
            if (idInNotes != null && idInNotes > 0L) {
                val byId = inventoryDao.getItemById(idInNotes)
                if (byId != null) return byId
            }

            // 3. Explicit SKU or [SKU: X] in notes
            val skuInNotes = explicitSku?.takeIf { it.isNotBlank() }
                ?: Regex("""\[SKU:\s*([^\]]+)\]""", RegexOption.IGNORE_CASE).find(notes)?.groupValues?.get(1)?.trim()
            if (!skuInNotes.isNullOrBlank()) {
                val bySku = inventoryDao.getItemBySku(skuInNotes)
                if (bySku != null) return bySku
            }

            val category = normalizeCategory(serviceType)
            val allItems = inventoryDao.getAllItemsSync()
            if (allItems.isEmpty()) return null

            val candidateVariety = plantVariety.trim()
            val candidateRootstock = rootstock.trim()

            // 4. Exact SKU match across all items (if user entered a SKU directly in variety field)
            if (candidateVariety.isNotBlank()) {
                val skuMatch = allItems.firstOrNull { it.sku.equals(candidateVariety, ignoreCase = true) }
                if (skuMatch != null) return skuMatch
            }

            // Filter by normalized category if possible
            val categoryItems = allItems.filter {
                normalizeCategory(it.category).equals(category, ignoreCase = true) ||
                it.category.equals(serviceType, ignoreCase = true)
            }
            val pool = if (categoryItems.isNotEmpty()) categoryItems else allItems

            // 5. Match by Variety + Rootstock
            if (candidateVariety.isNotBlank() && candidateRootstock.isNotBlank()) {
                val match = pool.firstOrNull {
                    (it.variety.equals(candidateVariety, ignoreCase = true) || it.itemName.contains(candidateVariety, ignoreCase = true)) &&
                    (it.itemName.contains(candidateRootstock, ignoreCase = true) || it.variety.contains(candidateRootstock, ignoreCase = true))
                }
                if (match != null) return match
            }

            // 6. Match for Rootstocks category (where rootstock name is primary)
            if (category.equals("Imported Rootstock", ignoreCase = true) && candidateRootstock.isNotBlank()) {
                val rootstockMatch = pool.firstOrNull {
                    it.variety.equals(candidateRootstock, ignoreCase = true) ||
                    it.itemName.equals(candidateRootstock, ignoreCase = true) ||
                    it.itemName.contains(candidateRootstock, ignoreCase = true) ||
                    it.variety.contains(candidateRootstock, ignoreCase = true)
                }
                if (rootstockMatch != null) return rootstockMatch
            }

            // 7. Match by Variety
            if (candidateVariety.isNotBlank()) {
                // Exact variety match
                val exactVariety = pool.firstOrNull { it.variety.equals(candidateVariety, ignoreCase = true) }
                if (exactVariety != null) return exactVariety

                // Exact itemName match
                val exactName = pool.firstOrNull { it.itemName.equals(candidateVariety, ignoreCase = true) }
                if (exactName != null) return exactName

                // Substring match in itemName
                val containsName = pool.firstOrNull {
                    it.itemName.contains(candidateVariety, ignoreCase = true) || candidateVariety.contains(it.itemName, ignoreCase = true)
                }
                if (containsName != null) return containsName

                // Substring match in variety
                val containsVariety = pool.firstOrNull {
                    it.variety.isNotBlank() && (it.variety.contains(candidateVariety, ignoreCase = true) || candidateVariety.contains(it.variety, ignoreCase = true))
                }
                if (containsVariety != null) return containsVariety
            }

            // 8. If candidateRootstock is provided, try that
            if (candidateRootstock.isNotBlank()) {
                val matchRootstock = pool.firstOrNull {
                    it.variety.equals(candidateRootstock, ignoreCase = true) ||
                    it.itemName.contains(candidateRootstock, ignoreCase = true)
                }
                if (matchRootstock != null) return matchRootstock
            }

            // 9. Fallback if category has only a single item
            if (categoryItems.size == 1) {
                return categoryItems.first()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error finding matching inventory item: ${e.message}", e)
        }
        return null
    }

    suspend fun findMatchingInventoryItem(inventoryDao: InventoryDao, record: CropRecord): InventoryItem? {
        return findMatchingInventoryItem(
            inventoryDao = inventoryDao,
            serviceType = record.serviceType,
            plantVariety = record.plantVariety,
            rootstock = record.rootstock,
            notes = record.notes
        )
    }

    /**
     * Validates whether sufficient stock exists before creating or updating a booking.
     */
    suspend fun validateStock(
        inventoryDao: InventoryDao,
        record: CropRecord,
        oldRecord: CropRecord? = null
    ): StockValidationResult {
        // If the booking is not deductible (e.g. draft, cancelled, site visit), always pass validation
        if (!isStockDeductible(record)) {
            return StockValidationResult.Success(null, null)
        }

        val item = findMatchingInventoryItem(inventoryDao, record) ?: return StockValidationResult.Success(null, null)

        val isOldDeductible = oldRecord != null && isStockDeductible(oldRecord)
        val oldItem = if (oldRecord != null) findMatchingInventoryItem(inventoryDao, oldRecord) else null

        val effectiveAvailableStock = if (isOldDeductible && oldItem != null && oldItem.id == item.id) {
            // When editing the same item, the previously booked quantity is added back to available stock
            item.currentQuantity + oldRecord.quantity
        } else {
            item.currentQuantity
        }

        return if (record.quantity > effectiveAvailableStock) {
            StockValidationResult.InsufficientStock(
                item = item,
                availableStock = effectiveAvailableStock,
                requestedQty = record.quantity
            )
        } else {
            StockValidationResult.Success(
                item = item,
                remainingStock = effectiveAvailableStock - record.quantity
            )
        }
    }

    /**
     * Centralized alert for low stock or out-of-stock events.
     * Only fires on genuine downward threshold crossings:
     * oldQuantity > item.lowStockThreshold AND item.currentQuantity <= item.lowStockThreshold
     */
    fun checkAndNotifyLowStock(
        context: Context,
        oldQuantity: Int,
        item: InventoryItem
    ) {
        if (oldQuantity > item.lowStockThreshold && item.currentQuantity <= item.lowStockThreshold) {
            val isOutOfStock = item.currentQuantity <= 0
            val title = if (isOutOfStock) "Out of Stock Alert! ⚠️" else "Low Stock Alert! 📦"
            val varietySuffix = if (item.variety.isNotBlank()) " (${item.variety})" else ""
            val message = if (isOutOfStock) {
                "${item.itemName}$varietySuffix is completely out of stock (0 units remaining)."
            } else {
                "${item.itemName}$varietySuffix is running low! Only ${item.currentQuantity} units remaining (Threshold: ${item.lowStockThreshold})."
            }

            val deepLinkUri = Uri.parse("baagbaanboi://inventory")

            NotificationHelper.postSystemNotification(
                context = context,
                title = title,
                message = message,
                channelId = NotificationHelper.CHANNEL_INVENTORY_ID,
                notificationId = (item.id.toInt() + 90000),
                deepLinkUri = deepLinkUri
            )

            // Insert into in-app Notification Center Room database asynchronously
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    db.notificationDao().insertNotification(
                        AppNotification(
                            title = title,
                            message = message,
                            type = "INVENTORY",
                            relatedRecordId = item.id
                        )
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to insert low stock in-app notification: ${e.message}")
                }
            }
        }
    }

    /**
     * Applies inventory stock deduction on booking save (creation).
     */
    suspend fun applyBookingSave(
        inventoryDao: InventoryDao,
        firestoreSyncManager: FirestoreSyncManager,
        record: CropRecord,
        context: Context? = null
    ) {
        if (!isStockDeductible(record)) return

        val item = findMatchingInventoryItem(inventoryDao, record) ?: return
        val oldQty = item.currentQuantity
        val newQty = (item.currentQuantity - record.quantity).coerceAtLeast(0)
        inventoryDao.updateCurrentQuantity(item.id, newQty)

        val updated = inventoryDao.getItemById(item.id)
        if (updated != null) {
            firestoreSyncManager.saveInventoryItem(updated)
            if (context != null) {
                checkAndNotifyLowStock(context, oldQty, updated)
            }
        }
    }

    /**
     * Returns stock to inventory when a booking is deleted or moved to Recycle Bin.
     */
    suspend fun applyBookingDelete(
        inventoryDao: InventoryDao,
        firestoreSyncManager: FirestoreSyncManager,
        record: CropRecord,
        context: Context? = null
    ) {
        if (!isStockDeductible(record)) return

        val item = findMatchingInventoryItem(inventoryDao, record) ?: return
        val oldQty = item.currentQuantity
        val newQty = item.currentQuantity + record.quantity
        inventoryDao.updateCurrentQuantity(item.id, newQty)

        val updated = inventoryDao.getItemById(item.id)
        if (updated != null) {
            firestoreSyncManager.saveInventoryItem(updated)
            if (context != null) {
                checkAndNotifyLowStock(context, oldQty, updated)
            }
        }
    }

    /**
     * Adjusts inventory stock on booking update.
     * Correctly handles quantity changes, item category/variety reassignments,
     * and status transitions (e.g. Active -> Cancelled or Cancelled -> Active).
     */
    suspend fun applyBookingUpdate(
        inventoryDao: InventoryDao,
        firestoreSyncManager: FirestoreSyncManager,
        newRecord: CropRecord,
        oldRecord: CropRecord?,
        context: Context? = null
    ) {
        val wasDeductible = oldRecord != null && isStockDeductible(oldRecord)
        val isNowDeductible = isStockDeductible(newRecord)

        val oldItem = if (oldRecord != null) findMatchingInventoryItem(inventoryDao, oldRecord) else null
        val newItem = findMatchingInventoryItem(inventoryDao, newRecord)

        when {
            // Case 1: Was deductible and is still deductible
            wasDeductible && isNowDeductible -> {
                if (oldItem != null && newItem != null && oldItem.id == newItem.id) {
                    val diff = newRecord.quantity - oldRecord!!.quantity
                    if (diff != 0) {
                        val oldQty = newItem.currentQuantity
                        val newStock = (newItem.currentQuantity - diff).coerceAtLeast(0)
                        inventoryDao.updateCurrentQuantity(newItem.id, newStock)
                        val updated = inventoryDao.getItemById(newItem.id)
                        if (updated != null) {
                            firestoreSyncManager.saveInventoryItem(updated)
                            if (context != null) {
                                checkAndNotifyLowStock(context, oldQty, updated)
                            }
                        }
                    }
                } else {
                    // Changed item entirely: return to old, deduct from new
                    if (oldItem != null) {
                        val oldOldQty = oldItem.currentQuantity
                        val restoredStock = oldItem.currentQuantity + oldRecord!!.quantity
                        inventoryDao.updateCurrentQuantity(oldItem.id, restoredStock)
                        val updatedOld = inventoryDao.getItemById(oldItem.id)
                        if (updatedOld != null) {
                            firestoreSyncManager.saveInventoryItem(updatedOld)
                            if (context != null) {
                                checkAndNotifyLowStock(context, oldOldQty, updatedOld)
                            }
                        }
                    }
                    if (newItem != null) {
                        val oldNewQty = newItem.currentQuantity
                        val deductedStock = (newItem.currentQuantity - newRecord.quantity).coerceAtLeast(0)
                        inventoryDao.updateCurrentQuantity(newItem.id, deductedStock)
                        val updatedNew = inventoryDao.getItemById(newItem.id)
                        if (updatedNew != null) {
                            firestoreSyncManager.saveInventoryItem(updatedNew)
                            if (context != null) {
                                checkAndNotifyLowStock(context, oldNewQty, updatedNew)
                            }
                        }
                    }
                }
            }

            // Case 2: Was deductible, but now NOT deductible (e.g. status changed to Cancelled or Draft)
            wasDeductible && !isNowDeductible -> {
                if (oldItem != null) {
                    val oldOldQty = oldItem.currentQuantity
                    val restoredStock = oldItem.currentQuantity + oldRecord!!.quantity
                    inventoryDao.updateCurrentQuantity(oldItem.id, restoredStock)
                    val updated = inventoryDao.getItemById(oldItem.id)
                    if (updated != null) {
                        firestoreSyncManager.saveInventoryItem(updated)
                        if (context != null) {
                            checkAndNotifyLowStock(context, oldOldQty, updated)
                        }
                    }
                }
            }

            // Case 3: Was NOT deductible, but is now deductible (e.g. status uncanceled back to Active/Confirmed)
            !wasDeductible && isNowDeductible -> {
                if (newItem != null) {
                    val oldNewQty = newItem.currentQuantity
                    val deductedStock = (newItem.currentQuantity - newRecord.quantity).coerceAtLeast(0)
                    inventoryDao.updateCurrentQuantity(newItem.id, deductedStock)
                    val updated = inventoryDao.getItemById(newItem.id)
                    if (updated != null) {
                        firestoreSyncManager.saveInventoryItem(updated)
                        if (context != null) {
                            checkAndNotifyLowStock(context, oldNewQty, updated)
                        }
                    }
                }
            }

            // Case 4: Neither was deductible (e.g. Draft -> Cancelled) -> No stock changes
            else -> { /* No op */ }
        }
    }

    /**
     * Full consistency synchronization: recalculates currentQuantity for all items
     * based on initialQuantity and confirmed bookings.
     */
    suspend fun recalculateAllStock(
        database: AppDatabase,
        firestoreSyncManager: FirestoreSyncManager = FirestoreSyncManager(),
        context: Context? = null
    ) {
        try {
            val inventoryDao = database.inventoryDao()
            val cropDao = database.cropRecordDao()
            val allItems = inventoryDao.getAllItemsSync()
            if (allItems.isEmpty()) return

            val allBookings = cropDao.getAllRecordsList()

            for (item in allItems) {
                val totalBookedQty = allBookings
                    .filter { isStockDeductible(it) }
                    .filter { booking ->
                        val matched = findMatchingInventoryItem(inventoryDao, booking)
                        matched?.id == item.id
                    }
                    .sumOf { it.quantity }

                val calculatedCurrent = (item.initialQuantity - totalBookedQty).coerceAtLeast(0)
                if (item.currentQuantity != calculatedCurrent) {
                    val oldQty = item.currentQuantity
                    inventoryDao.updateCurrentQuantity(item.id, calculatedCurrent)
                    val updated = item.copy(currentQuantity = calculatedCurrent)
                    firestoreSyncManager.saveInventoryItem(updated)
                    if (context != null) {
                        checkAndNotifyLowStock(context, oldQty, updated)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in recalculateAllStock: ${e.message}", e)
        }
    }
}
