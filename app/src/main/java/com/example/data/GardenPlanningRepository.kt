package com.example.data

import kotlinx.coroutines.flow.Flow

class GardenPlanningRepository(
    private val dao: GardenPlanningDao,
    private val farmerContactDao: FarmerContactDao? = null,
    private val recycleBinDao: RecycleBinDao? = null,
    private val firestoreSyncManager: FirestoreSyncManager = FirestoreSyncManager()
) {
    val allEntries: Flow<List<GardenPlanningEntry>> = dao.getAllEntries()

    fun searchEntries(query: String): Flow<List<GardenPlanningEntry>> {
        return dao.searchEntries(query)
    }

    fun getEntryById(id: Long): Flow<GardenPlanningEntry?> {
        return dao.getEntryById(id)
    }

    suspend fun getEntryByIdSync(id: Long): GardenPlanningEntry? {
        return dao.getEntryByIdSync(id)
    }

    suspend fun getAllEntriesList(): List<GardenPlanningEntry> {
        return dao.getAllEntriesList()
    }

    suspend fun insert(entry: GardenPlanningEntry): Long {
        val insertedId = dao.insertEntry(entry)
        val entryToSync = if (entry.id == 0L) entry.copy(id = insertedId) else entry
        firestoreSyncManager.saveGardenPlanningEntry(entryToSync)
        syncFarmerContactOnSave(entryToSync)
        return insertedId
    }

    suspend fun update(entry: GardenPlanningEntry) {
        dao.updateEntry(entry)
        firestoreSyncManager.saveGardenPlanningEntry(entry)
        syncFarmerContactOnSave(entry)
    }

    suspend fun delete(entry: GardenPlanningEntry) {
        dao.deleteEntry(entry)
        firestoreSyncManager.deleteGardenPlanningEntry(entry.id)

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

    private suspend fun syncFarmerContactOnSave(entry: GardenPlanningEntry) {
        if (farmerContactDao != null && (entry.farmerName.isNotBlank() || entry.contactNumber.isNotBlank())) {
            val existing = farmerContactDao.getContactByPhoneOrName(entry.contactNumber, entry.farmerName)
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
                    name = if (existing.name.isBlank()) entry.farmerName else existing.name,
                    phone = if (existing.phone.isBlank()) entry.contactNumber else existing.phone,
                    address = if (existing.address.isBlank()) entry.farmerAddress else existing.address
                )
                farmerContactDao.updateContact(updated)
            }
        }
    }
}
