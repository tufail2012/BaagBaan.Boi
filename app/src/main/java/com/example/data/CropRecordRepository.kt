package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class CropRecordRepository(
    private val dao: CropRecordDao,
    private val farmerContactDao: FarmerContactDao? = null,
    private val recycleBinDao: RecycleBinDao? = null,
    private val inventoryDao: InventoryDao? = null,
    private val firestoreSyncManager: FirestoreSyncManager = FirestoreSyncManager()
) {
    val allRecords: Flow<List<CropRecord>> = dao.getAllRecords()
    val recordCount: Flow<Int> = dao.getRecordCount()

    fun getRecordsByService(serviceType: String): Flow<List<CropRecord>> {
        return dao.getRecordsByService(serviceType)
    }

    fun searchRecords(query: String): Flow<List<CropRecord>> {
        return dao.searchRecords(query)
    }

    fun getRecordById(id: Long): Flow<CropRecord?> {
        return dao.getRecordById(id)
    }

    suspend fun insert(record: CropRecord): Long {
        val insertedId = dao.insertRecord(record)
        val recordToSync = if (record.id == 0L) record.copy(id = insertedId) else record
        firestoreSyncManager.saveCropRecord(recordToSync)
        syncFarmerContactOnSave(recordToSync)
        adjustInventoryOnBookingSave(recordToSync)
        return insertedId
    }

    suspend fun update(record: CropRecord, oldRecord: CropRecord? = null) {
        val previousRecord = oldRecord ?: kotlinx.coroutines.withTimeoutOrNull(1000) {
            dao.getRecordById(record.id).firstOrNull()
        }

        dao.updateRecord(record)
        firestoreSyncManager.saveCropRecord(record)
        syncFarmerContactOnSave(record)

        if (previousRecord != null) {
            adjustInventoryOnBookingDelete(previousRecord)
        }
        adjustInventoryOnBookingSave(record)
    }

    suspend fun delete(record: CropRecord) {
        dao.deleteRecord(record)
        firestoreSyncManager.deleteCropRecord(record.id)
        syncFarmerContactOnDelete(record)
        adjustInventoryOnBookingDelete(record)

        if (recycleBinDao != null) {
            val jsonPayload = RecycleBinConverter.cropRecordToJson(record)
            val binItem = RecycleBinEntity(
                itemType = "BOOKING",
                title = "Booking #${record.serialNumber} - ${record.farmerName.ifBlank { "Farmer" }}",
                subtitle = "${record.serviceType} • Qty: ${record.quantity} • ${record.plantVariety.ifBlank { "Standard" }}",
                jsonPayload = jsonPayload,
                deletedAt = System.currentTimeMillis()
            )
            val insertedId = recycleBinDao.insert(binItem)
            firestoreSyncManager.saveRecycleBinItem(binItem.copy(id = insertedId))
        }
    }

    private suspend fun adjustInventoryOnBookingSave(record: CropRecord) {
        if (inventoryDao == null) return
        val validCategories = listOf("Local Plants", "Imported Plants", "Imported Rootstock", "Garden Planning")
        val categoryMatch = validCategories.firstOrNull { record.serviceType.contains(it, ignoreCase = true) }
        if (categoryMatch != null) {
            val varietyToMatch = if (record.rootstock.isNotBlank()) record.rootstock else record.plantVariety
            val item = inventoryDao.findMatchingItem(categoryMatch, varietyToMatch)
            if (item != null) {
                inventoryDao.decrementQuantity(item.id, record.quantity)
                val updated = inventoryDao.getItemById(item.id)
                if (updated != null) {
                    firestoreSyncManager.saveInventoryItem(updated)
                }
            }
        }
    }

    private suspend fun adjustInventoryOnBookingDelete(record: CropRecord) {
        if (inventoryDao == null) return
        val validCategories = listOf("Local Plants", "Imported Plants", "Imported Rootstock", "Garden Planning")
        val categoryMatch = validCategories.firstOrNull { record.serviceType.contains(it, ignoreCase = true) }
        if (categoryMatch != null) {
            val varietyToMatch = if (record.rootstock.isNotBlank()) record.rootstock else record.plantVariety
            val item = inventoryDao.findMatchingItem(categoryMatch, varietyToMatch)
            if (item != null) {
                inventoryDao.incrementQuantity(item.id, record.quantity)
                val updated = inventoryDao.getItemById(item.id)
                if (updated != null) {
                    firestoreSyncManager.saveInventoryItem(updated)
                }
            }
        }
    }

    private suspend fun syncFarmerContactOnSave(record: CropRecord) {
        if (farmerContactDao != null && (record.farmerName.isNotBlank() || record.contactNumber.isNotBlank())) {
            val existing = farmerContactDao.getContactByPhoneOrName(record.contactNumber, record.farmerName)
            if (existing == null) {
                farmerContactDao.insertContact(
                    FarmerContact(
                        name = record.farmerName.ifBlank { "Farmer" },
                        phone = record.contactNumber,
                        address = record.farmerAddress,
                        category = "Farmer"
                    )
                )
            } else {
                val updated = existing.copy(
                    name = if (existing.name.isBlank()) record.farmerName else existing.name,
                    phone = if (existing.phone.isBlank()) record.contactNumber else existing.phone,
                    address = if (existing.address.isBlank()) record.farmerAddress else existing.address
                )
                farmerContactDao.updateContact(updated)
            }
        }
    }

    private suspend fun syncFarmerContactOnDelete(record: CropRecord) {
        if (farmerContactDao != null && (record.contactNumber.isNotBlank() || record.farmerName.isNotBlank())) {
            val remainingCount = dao.countRecordsByFarmer(record.contactNumber, record.farmerName)
            if (remainingCount == 0) {
                farmerContactDao.deleteContactByPhoneOrName(record.contactNumber, record.farmerName)
            }
        }
    }

    suspend fun deleteAll() {
        dao.deleteAll()
    }
}
