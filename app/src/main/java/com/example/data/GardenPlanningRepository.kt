package com.example.data

import com.example.util.SerialNumberUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GardenPlanningRepository(
    private val dao: GardenPlanningDao,
    private val farmerContactDao: FarmerContactDao? = null,
    private val recycleBinDao: RecycleBinDao? = null,
    private val firestoreSyncManager: FirestoreSyncManager = FirestoreSyncManager()
) {
    val allEntries: Flow<List<GardenPlanningEntry>> = dao.getAllEntries().map { list ->
        list.sortedWith(SerialNumberUtils.gardenEntryComparator)
    }

    fun searchEntries(query: String): Flow<List<GardenPlanningEntry>> {
        return dao.searchEntries(query).map { list ->
            list.sortedWith(SerialNumberUtils.gardenEntryComparator)
        }
    }

    fun getEntryById(id: Long): Flow<GardenPlanningEntry?> {
        return dao.getEntryById(id)
    }

    suspend fun getEntryByIdSync(id: Long): GardenPlanningEntry? {
        return dao.getEntryByIdSync(id)
    }

    suspend fun getAllEntriesList(): List<GardenPlanningEntry> {
        return dao.getAllEntriesList().sortedWith(SerialNumberUtils.gardenEntryComparator)
    }

    suspend fun insert(entry: GardenPlanningEntry): Long {
        val insertedId = dao.insertEntry(entry)
        val entryToSync = if (entry.id == 0L) entry.copy(id = insertedId) else entry
        firestoreSyncManager.saveGardenPlanningEntry(entryToSync)
        syncFarmerContactOnSave(entryToSync)
        com.example.widget.PendingPaymentsWidgetUpdater.triggerUpdate()
        return insertedId
    }

    suspend fun update(entry: GardenPlanningEntry) {
        dao.updateEntry(entry)
        firestoreSyncManager.saveGardenPlanningEntry(entry)
        syncFarmerContactOnSave(entry)
        com.example.widget.PendingPaymentsWidgetUpdater.triggerUpdate()
    }

    suspend fun delete(entry: GardenPlanningEntry) {
        dao.deleteEntry(entry)
        firestoreSyncManager.deleteGardenPlanningEntry(entry.id)
        com.example.widget.PendingPaymentsWidgetUpdater.triggerUpdate()

        if (recycleBinDao != null) {
            val jsonPayload = RecycleBinConverter.gardenPlanningToJson(entry)
            val binItem = RecycleBinEntity(
                itemType = "GARDEN_PLANNING",
                title = "Garden Plan #${entry.serialNumber} - ${entry.farmerName.ifBlank { "Farmer" }}",
                subtitle = "Area: ${entry.totalKanalArea} Kanals • Cost: ₹${entry.totalCost.toInt()}",
                jsonPayload = jsonPayload,
                deletedAt = System.currentTimeMillis()
            )
            val insertedId = recycleBinDao.insert(binItem)
            firestoreSyncManager.saveRecycleBinItem(binItem.copy(id = insertedId))
        }
    }

    private suspend fun findContact(phone: String, name: String): FarmerContact? {
        if (farmerContactDao == null) return null
        val cleanPhone = phone.filter { it.isDigit() }.takeLast(10)
        if (cleanPhone.isNotEmpty()) {
            val direct = farmerContactDao.getContactByPhone(phone)
            if (direct != null) return direct

            val all = farmerContactDao.getAllContactsSync()
            val match = all.firstOrNull { it.phone.filter { c -> c.isDigit() }.takeLast(10) == cleanPhone }
            if (match != null) return match

            // When phone is present, do NOT fall back to matching by name.
            // Different bookings with identical names but different contact numbers must be separate directory entries.
            return null
        } else if (name.isNotBlank()) {
            return farmerContactDao.getContactByNameWithoutPhone(name.trim())
        }
        return null
    }

    private suspend fun syncFarmerContactOnSave(entry: GardenPlanningEntry) {
        if (farmerContactDao != null && (entry.farmerName.isNotBlank() || entry.contactNumber.isNotBlank())) {
            val existing = findContact(entry.contactNumber, entry.farmerName)
            if (existing == null) {
                farmerContactDao.insertContact(
                    FarmerContact(
                        name = entry.farmerName.ifBlank { "Farmer" },
                        phone = entry.contactNumber,
                        address = entry.farmerAddress,
                        category = "Farmer"
                    )
                )
            } else {
                val updated = existing.copy(
                    name = if (entry.farmerName.isNotBlank()) entry.farmerName else existing.name,
                    phone = if (existing.phone.isBlank() && entry.contactNumber.isNotBlank()) entry.contactNumber else existing.phone,
                    address = if (entry.farmerAddress.isNotBlank()) entry.farmerAddress else existing.address
                )
                farmerContactDao.updateContact(updated)
            }
        }
    }
}
