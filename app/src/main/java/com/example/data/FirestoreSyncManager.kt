package com.example.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

enum class SyncState {
    SYNCED,
    SYNCING,
    OFFLINE
}

class FirestoreSyncManager {

    private val db get() = com.example.util.SafeFirebase.db
    private val auth get() = com.example.util.SafeFirebase.auth

    companion object {
        private const val TAG = "FirestoreSyncManager"
        const val COLLECTION_USERS = "users"
        const val COLLECTION_CROP_RECORDS = "crop_records"
        const val COLLECTION_WORKERS = "workers"
        const val COLLECTION_ATTENDANCE = "attendance_records"
        const val COLLECTION_ADVANCE = "advance_payments"
        const val COLLECTION_RECYCLE_BIN = "recycle_bin"
        const val COLLECTION_CONTACTS = "farmer_contacts"
        const val COLLECTION_INVENTORY = "inventory"
        const val COLLECTION_GARDEN_PLANNING = "garden_planning_entries"

        private val _syncState = MutableStateFlow(SyncState.SYNCED)
        val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

        private val _lastSyncedTime = MutableStateFlow(System.currentTimeMillis())
        val lastSyncedTime: StateFlow<Long> = _lastSyncedTime.asStateFlow()

        fun updateSyncState(state: SyncState) {
            _syncState.value = state
            if (state == SyncState.SYNCED) {
                _lastSyncedTime.value = System.currentTimeMillis()
            }
        }
    }

    private fun getCurrentUid(): String? {
        return auth?.currentUser?.uid
    }

    private fun getUserCollection(collectionName: String): CollectionReference? {
        val uid = getCurrentUid()
        val firestore = db
        if (uid.isNullOrEmpty() || firestore == null) {
            Log.w(TAG, "Unauthenticated access attempt or Firestore unavailable for collection $collectionName")
            return null
        }
        return firestore.collection(COLLECTION_USERS).document(uid).collection(collectionName)
    }

    // =========================================================================
    // 1. CROP & BOOKING RECORDS
    // =========================================================================

    suspend fun saveCropRecord(record: CropRecord) {
        updateSyncState(SyncState.SYNCING)
        val collectionRef = getUserCollection(COLLECTION_CROP_RECORDS)
        if (collectionRef == null) {
            Log.w(TAG, "Skipping saveCropRecord: User is not authenticated")
            updateSyncState(SyncState.OFFLINE)
            return
        }

        try {
            val recordMap = hashMapOf(
                "id" to record.id,
                "serialNumber" to record.serialNumber,
                "farmerName" to record.farmerName,
                "farmerAddress" to record.farmerAddress,
                "contactNumber" to record.contactNumber,
                "serviceType" to record.serviceType,
                "plantVariety" to record.plantVariety,
                "rootstock" to record.rootstock,
                "quantity" to record.quantity,
                "landAreaAcres" to record.landAreaAcres,
                "soilType" to record.soilType,
                "healthStage" to record.healthStage,
                "location" to record.location,
                "notes" to record.notes,
                "amountPaid" to record.amountPaid,
                "paymentStatus" to record.paymentStatus,
                "bookingDate" to record.bookingDate,
                "expectedDelivery" to record.expectedDelivery,
                "paymentProofUri" to record.paymentProofUri,
                "paymentHistoryJson" to record.paymentHistoryJson,
                "timestamp" to record.timestamp
            )

            collectionRef
                .document(record.id.toString())
                .set(recordMap, SetOptions.merge())
                .await()

            updateSyncState(SyncState.SYNCED)
            Log.d(TAG, "Successfully synced Crop Record ${record.id} to Firestore")
        } catch (e: Exception) {
            updateSyncState(SyncState.OFFLINE)
            Log.e(TAG, "Error syncing Crop Record ${record.id} to Firestore: ${e.message}")
            throw e
        }
    }

    suspend fun deleteCropRecord(recordId: Long) {
        updateSyncState(SyncState.SYNCING)
        val collectionRef = getUserCollection(COLLECTION_CROP_RECORDS)
        if (collectionRef == null) {
            Log.w(TAG, "Skipping deleteCropRecord: User is not authenticated")
            updateSyncState(SyncState.OFFLINE)
            return
        }

        try {
            collectionRef
                .document(recordId.toString())
                .delete()
                .await()
            updateSyncState(SyncState.SYNCED)
            Log.d(TAG, "Successfully deleted Crop Record $recordId from Firestore")
        } catch (e: Exception) {
            updateSyncState(SyncState.OFFLINE)
            Log.e(TAG, "Error deleting Crop Record $recordId from Firestore: ${e.message}")
        }
    }

    // =========================================================================
    // 1B. GARDEN PLANNING ENTRIES
    // =========================================================================

    suspend fun saveGardenPlanningEntry(entry: GardenPlanningEntry) {
        updateSyncState(SyncState.SYNCING)
        val collectionRef = getUserCollection(COLLECTION_GARDEN_PLANNING)
        if (collectionRef == null) {
            Log.w(TAG, "Skipping saveGardenPlanningEntry: User is not authenticated")
            updateSyncState(SyncState.OFFLINE)
            return
        }

        try {
            val entryMap = hashMapOf(
                "id" to entry.id,
                "serialNumber" to entry.serialNumber,
                "farmerName" to entry.farmerName,
                "farmerAddress" to entry.farmerAddress,
                "contactNumber" to entry.contactNumber,
                "totalKanalArea" to entry.totalKanalArea,
                "plantsPerKanal" to entry.plantsPerKanal,
                "costPerPlant" to entry.costPerPlant,
                "totalCost" to entry.totalCost,
                "amountPaid" to entry.amountPaid,
                "remainingBalance" to entry.remainingBalance,
                "paymentStatus" to entry.paymentStatus,
                "bookingDate" to entry.bookingDate,
                "expectedDelivery" to entry.expectedDelivery,
                "notes" to entry.notes,
                "installmentHistoryJson" to entry.installmentHistoryJson,
                "timestamp" to entry.timestamp
            )

            collectionRef
                .document(entry.id.toString())
                .set(entryMap, SetOptions.merge())
                .await()

            updateSyncState(SyncState.SYNCED)
            Log.d(TAG, "Successfully synced Garden Planning Entry ${entry.id} to Firestore")
        } catch (e: Exception) {
            updateSyncState(SyncState.OFFLINE)
            Log.e(TAG, "Error syncing Garden Planning Entry ${entry.id} to Firestore: ${e.message}")
            throw e
        }
    }

    suspend fun deleteGardenPlanningEntry(entryId: Long) {
        updateSyncState(SyncState.SYNCING)
        val collectionRef = getUserCollection(COLLECTION_GARDEN_PLANNING)
        if (collectionRef == null) {
            Log.w(TAG, "Skipping deleteGardenPlanningEntry: User is not authenticated")
            updateSyncState(SyncState.OFFLINE)
            return
        }

        try {
            collectionRef
                .document(entryId.toString())
                .delete()
                .await()

            updateSyncState(SyncState.SYNCED)
            Log.d(TAG, "Successfully deleted Garden Planning Entry $entryId from Firestore")
        } catch (e: Exception) {
            updateSyncState(SyncState.OFFLINE)
            Log.e(TAG, "Error deleting Garden Planning Entry $entryId from Firestore: ${e.message}")
        }
    }

    // =========================================================================
    // 2. WORKERS
    // =========================================================================

    suspend fun saveWorker(worker: Worker) {
        updateSyncState(SyncState.SYNCING)
        val collectionRef = getUserCollection(COLLECTION_WORKERS)
        if (collectionRef == null) {
            Log.w(TAG, "Skipping saveWorker: User is not authenticated")
            updateSyncState(SyncState.OFFLINE)
            return
        }

        try {
            val workerMap = hashMapOf(
                "workerId" to worker.workerId,
                "name" to worker.name,
                "phoneNumber" to worker.phoneNumber,
                "dailyRate" to worker.dailyRate,
                "advancePaid" to worker.advancePaid,
                "isActive" to worker.isActive
            )

            collectionRef
                .document(worker.workerId.toString())
                .set(workerMap, SetOptions.merge())
                .await()

            updateSyncState(SyncState.SYNCED)
            Log.d(TAG, "Successfully synced Worker ${worker.workerId} to Firestore")
        } catch (e: Exception) {
            updateSyncState(SyncState.OFFLINE)
            Log.e(TAG, "Error syncing Worker ${worker.workerId} to Firestore: ${e.message}")
        }
    }

    // =========================================================================
    // 3. DAILY WORKER ATTENDANCE DETAILS
    // =========================================================================

    suspend fun saveAttendanceRecord(record: AttendanceRecord) {
        updateSyncState(SyncState.SYNCING)
        val collectionRef = getUserCollection(COLLECTION_ATTENDANCE)
        if (collectionRef == null) {
            Log.w(TAG, "Skipping saveAttendanceRecord: User is not authenticated")
            updateSyncState(SyncState.OFFLINE)
            return
        }

        val docId = "${record.workerId}_${record.date}"
        try {
            val attendanceMap = hashMapOf(
                "id" to record.id,
                "workerId" to record.workerId,
                "date" to record.date,
                "status" to record.status.name,
                "markedAt" to record.markedAt
            )

            collectionRef
                .document(docId)
                .set(attendanceMap, SetOptions.merge())
                .await()

            updateSyncState(SyncState.SYNCED)
            Log.d(TAG, "Successfully synced Attendance $docId to Firestore")
        } catch (e: Exception) {
            updateSyncState(SyncState.OFFLINE)
            Log.e(TAG, "Error syncing Attendance $docId to Firestore: ${e.message}")
        }
    }

    suspend fun saveAttendanceList(records: List<AttendanceRecord>) {
        records.forEach { record ->
            saveAttendanceRecord(record)
        }
    }

    // =========================================================================
    // 4. ADVANCE PAYMENTS
    // =========================================================================

    suspend fun saveAdvancePayment(payment: AdvancePayment) {
        updateSyncState(SyncState.SYNCING)
        val collectionRef = getUserCollection(COLLECTION_ADVANCE)
        if (collectionRef == null) {
            Log.w(TAG, "Skipping saveAdvancePayment: User is not authenticated")
            updateSyncState(SyncState.OFFLINE)
            return
        }

        try {
            val paymentMap = hashMapOf(
                "paymentId" to payment.paymentId,
                "workerId" to payment.workerId,
                "amount" to payment.amount,
                "date" to payment.date,
                "note" to payment.note,
                "timestamp" to payment.timestamp
            )

            collectionRef
                .document(payment.paymentId.toString())
                .set(paymentMap, SetOptions.merge())
                .await()

            updateSyncState(SyncState.SYNCED)
            Log.d(TAG, "Successfully synced Advance Payment ${payment.paymentId} to Firestore")
        } catch (e: Exception) {
            updateSyncState(SyncState.OFFLINE)
            Log.e(TAG, "Error syncing Advance Payment ${payment.paymentId} to Firestore: ${e.message}")
        }
    }

    // =========================================================================
    // 5. FULL SYNC FROM FIRESTORE TO LOCAL ROOM DATABASE
    // =========================================================================

    suspend fun syncFromCloudToLocal(
        cropDao: CropRecordDao, 
        attendanceDao: AttendanceDao,
        gardenPlanningDao: GardenPlanningDao? = null
    ) {
        updateSyncState(SyncState.SYNCING)
        val uid = getCurrentUid()
        val firestore = db
        if (uid.isNullOrEmpty() || firestore == null) {
            Log.w(TAG, "Cannot sync from Cloud: User is not authenticated or Firestore unavailable")
            updateSyncState(SyncState.OFFLINE)
            return
        }

        try {
            // Sync Garden Planning Entries if DAO provided
            if (gardenPlanningDao != null) {
                try {
                    val localGardenEntries = gardenPlanningDao.getAllEntriesList()
                    val localGardenMap = localGardenEntries.associateBy { it.id }

                    val userCollectionGarden = firestore.collection(COLLECTION_USERS).document(uid).collection(COLLECTION_GARDEN_PLANNING)
                    val gardenSnapshot = userCollectionGarden.get().await()
                    val cloudGardenIds = mutableSetOf<Long>()

                    for (doc in gardenSnapshot.documents) {
                        val id = doc.getLong("id") ?: continue
                        cloudGardenIds.add(id)
                        val serialNumber = doc.getString("serialNumber") ?: ""
                        val farmerName = doc.getString("farmerName") ?: ""
                        val farmerAddress = doc.getString("farmerAddress") ?: ""
                        val contactNumber = doc.getString("contactNumber") ?: ""
                        val totalKanalArea = doc.getDouble("totalKanalArea") ?: 0.0
                        val plantsPerKanal = (doc.getLong("plantsPerKanal") ?: 0L).toInt()
                        val costPerPlant = doc.getDouble("costPerPlant") ?: 0.0
                        val totalCost = doc.getDouble("totalCost") ?: (totalKanalArea * plantsPerKanal * costPerPlant)
                        val amountPaid = doc.getDouble("amountPaid") ?: 0.0
                        val remainingBalance = doc.getDouble("remainingBalance") ?: (totalCost - amountPaid).coerceAtLeast(0.0)
                        val paymentStatus = doc.getString("paymentStatus") ?: "Pending"
                        val bookingDate = doc.getString("bookingDate") ?: ""
                        val expectedDelivery = doc.getString("expectedDelivery") ?: ""
                        val notes = doc.getString("notes") ?: ""
                        val installmentHistoryJson = doc.getString("installmentHistoryJson") ?: ""
                        val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()

                        val cloudEntry = GardenPlanningEntry(
                            id = id,
                            serialNumber = serialNumber,
                            farmerName = farmerName,
                            farmerAddress = farmerAddress,
                            contactNumber = contactNumber,
                            totalKanalArea = totalKanalArea,
                            plantsPerKanal = plantsPerKanal,
                            costPerPlant = costPerPlant,
                            totalCost = totalCost,
                            amountPaid = amountPaid,
                            remainingBalance = remainingBalance,
                            paymentStatus = paymentStatus,
                            bookingDate = bookingDate,
                            expectedDelivery = expectedDelivery,
                            notes = notes,
                            installmentHistoryJson = installmentHistoryJson,
                            timestamp = timestamp
                        )

                        val localEntry = localGardenMap[id]
                        if (localEntry != null && localEntry.timestamp > timestamp) {
                            saveGardenPlanningEntry(localEntry)
                        } else {
                            gardenPlanningDao.insertEntry(cloudEntry)
                        }
                    }

                    for (localEntry in localGardenEntries) {
                        if (localEntry.id !in cloudGardenIds && localEntry.id != 0L) {
                            saveGardenPlanningEntry(localEntry)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error syncing Garden Planning entries from Firestore: ${e.message}")
                }
            }
            // Sync Crop Records with timestamp-aware bidirectional reconciliation
            val localCropRecords = cropDao.getAllRecordsList()
            val localCropMap = localCropRecords.associateBy { it.id }

            val userCollectionCrops = firestore.collection(COLLECTION_USERS).document(uid).collection(COLLECTION_CROP_RECORDS)
            val cropSnapshot = userCollectionCrops.get().await()
            val cloudCropIds = mutableSetOf<Long>()

            for (doc in cropSnapshot.documents) {
                val id = doc.getLong("id") ?: continue
                cloudCropIds.add(id)
                val serialNumber = doc.getString("serialNumber") ?: ""
                val farmerName = doc.getString("farmerName") ?: ""
                val farmerAddress = doc.getString("farmerAddress") ?: ""
                val contactNumber = doc.getString("contactNumber") ?: ""
                val serviceType = doc.getString("serviceType") ?: "Local Plants"
                val plantVariety = doc.getString("plantVariety") ?: ""
                val rootstock = doc.getString("rootstock") ?: ""
                val quantity = (doc.getLong("quantity") ?: 1L).toInt()
                val landAreaAcres = doc.getDouble("landAreaAcres") ?: 1.0
                val soilType = doc.getString("soilType") ?: "Loamy"
                val healthStage = doc.getString("healthStage") ?: "Active Sapling"
                val location = doc.getString("location") ?: ""
                val notes = doc.getString("notes") ?: ""
                val amountPaid = doc.getDouble("amountPaid") ?: 0.0
                val paymentStatus = doc.getString("paymentStatus") ?: "Pending"
                val bookingDate = doc.getString("bookingDate") ?: ""
                val expectedDelivery = doc.getString("expectedDelivery") ?: ""
                val paymentProofUri = doc.getString("paymentProofUri") ?: ""
                val paymentHistoryJson = doc.getString("paymentHistoryJson") ?: ""
                val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()

                val cloudRecord = CropRecord(
                    id = id,
                    serialNumber = serialNumber,
                    farmerName = farmerName,
                    farmerAddress = farmerAddress,
                    contactNumber = contactNumber,
                    serviceType = serviceType,
                    plantVariety = plantVariety,
                    rootstock = rootstock,
                    quantity = quantity,
                    landAreaAcres = landAreaAcres,
                    soilType = soilType,
                    healthStage = healthStage,
                    location = location,
                    notes = notes,
                    amountPaid = amountPaid,
                    paymentStatus = paymentStatus,
                    bookingDate = bookingDate,
                    expectedDelivery = expectedDelivery,
                    paymentProofUri = paymentProofUri,
                    paymentHistoryJson = paymentHistoryJson,
                    timestamp = timestamp
                )

                val localRecord = localCropMap[id]
                if (localRecord != null && localRecord.timestamp > timestamp) {
                    // Local record has newer edits (such as newly added payment installments)
                    // Push local record to Cloud Firestore
                    saveCropRecord(localRecord)
                } else {
                    cropDao.insertRecord(cloudRecord)
                }
            }

            // Push any newly created local records that do not exist on Cloud yet
            for (localRecord in localCropRecords) {
                if (localRecord.id !in cloudCropIds && localRecord.id != 0L) {
                    saveCropRecord(localRecord)
                }
            }

            // Sync Workers
            val userCollectionWorkers = firestore.collection(COLLECTION_USERS).document(uid).collection(COLLECTION_WORKERS)
            val workerSnapshot = userCollectionWorkers.get().await()

            for (doc in workerSnapshot.documents) {
                val workerId = doc.getLong("workerId") ?: continue
                val name = doc.getString("name") ?: ""
                val phoneNumber = doc.getString("phoneNumber") ?: ""
                val dailyRate = doc.getDouble("dailyRate") ?: 0.0
                val advancePaid = doc.getDouble("advancePaid") ?: 0.0
                val isActive = doc.getBoolean("isActive") ?: true

                val worker = Worker(
                    workerId = workerId,
                    name = name,
                    phoneNumber = phoneNumber,
                    dailyRate = dailyRate,
                    advancePaid = advancePaid,
                    isActive = isActive
                )
                attendanceDao.insertWorker(worker)
            }

            // Sync Attendance Records
            val userCollectionAttendance = firestore.collection(COLLECTION_USERS).document(uid).collection(COLLECTION_ATTENDANCE)
            val attendanceSnapshot = userCollectionAttendance.get().await()

            val attendanceList = mutableListOf<AttendanceRecord>()
            for (doc in attendanceSnapshot.documents) {
                val id = doc.getLong("id") ?: 0L
                val workerId = doc.getLong("workerId") ?: continue
                val date = doc.getString("date") ?: continue
                val statusStr = doc.getString("status") ?: "PRESENT"
                val markedAt = doc.getLong("markedAt") ?: System.currentTimeMillis()

                val status = try {
                    AttendanceStatus.valueOf(statusStr)
                } catch (e: Exception) {
                    AttendanceStatus.PRESENT
                }

                attendanceList.add(
                    AttendanceRecord(
                        id = id,
                        workerId = workerId,
                        date = date,
                        status = status,
                        markedAt = markedAt
                    )
                )
            }
            if (attendanceList.isNotEmpty()) {
                attendanceDao.insertOrUpdateAttendanceList(attendanceList)
            }

            // Sync Advance Payments with timestamp-aware reconciliation
            val localAdvancePayments = attendanceDao.getAllAdvancePaymentsSync()
            val localAdvanceMap = localAdvancePayments.associateBy { it.paymentId }

            val userCollectionAdvance = firestore.collection(COLLECTION_USERS).document(uid).collection(COLLECTION_ADVANCE)
            val advanceSnapshot = userCollectionAdvance.get().await()
            val cloudAdvanceIds = mutableSetOf<Long>()

            for (doc in advanceSnapshot.documents) {
                val paymentId = doc.getLong("paymentId") ?: continue
                cloudAdvanceIds.add(paymentId)
                val workerId = doc.getLong("workerId") ?: continue
                val amount = doc.getDouble("amount") ?: 0.0
                val date = doc.getString("date") ?: ""
                val note = doc.getString("note") ?: doc.getString("notes") ?: ""
                val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()

                val cloudPayment = AdvancePayment(
                    paymentId = paymentId,
                    workerId = workerId,
                    amount = amount,
                    date = date,
                    note = note,
                    timestamp = timestamp
                )

                val localPayment = localAdvanceMap[paymentId]
                if (localPayment != null && localPayment.timestamp > timestamp) {
                    saveAdvancePayment(localPayment)
                } else {
                    attendanceDao.insertAdvancePayment(cloudPayment)
                }
            }

            for (localPayment in localAdvancePayments) {
                if (localPayment.paymentId !in cloudAdvanceIds && localPayment.paymentId != 0L) {
                    saveAdvancePayment(localPayment)
                }
            }

            updateSyncState(SyncState.SYNCED)
            Log.d(TAG, "Full Cloud Firestore to Local Room DB sync completed successfully for user $uid")
            com.example.widget.PendingPaymentsWidgetUpdater.triggerUpdate()
        } catch (e: Exception) {
            updateSyncState(SyncState.OFFLINE)
            Log.e(TAG, "Error performing Cloud sync: ${e.message}")
        }
    }

    suspend fun saveRecycleBinItem(item: RecycleBinEntity) {
        val collectionRef = getUserCollection(COLLECTION_RECYCLE_BIN) ?: return
        try {
            val itemMap = hashMapOf(
                "id" to item.id,
                "itemType" to item.itemType,
                "title" to item.title,
                "subtitle" to item.subtitle,
                "jsonPayload" to item.jsonPayload,
                "deletedAt" to item.deletedAt
            )
            collectionRef.document(item.id.toString()).set(itemMap, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving recycle bin item: ${e.message}")
        }
    }

    suspend fun deleteRecycleBinItem(itemId: Long) {
        val collectionRef = getUserCollection(COLLECTION_RECYCLE_BIN) ?: return
        try {
            collectionRef.document(itemId.toString()).delete().await()
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting recycle bin item: ${e.message}")
        }
    }

    suspend fun deleteFarmerContact(contactId: Long) {
        val collectionRef = getUserCollection(COLLECTION_CONTACTS) ?: return
        try {
            collectionRef.document(contactId.toString()).delete().await()
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting contact from Firestore: ${e.message}")
        }
    }

    suspend fun saveInventoryItem(item: InventoryItem) {
        updateSyncState(SyncState.SYNCING)
        val collectionRef = getUserCollection(COLLECTION_INVENTORY) ?: run {
            updateSyncState(SyncState.OFFLINE)
            return
        }
        try {
            val itemMap = hashMapOf(
                "id" to item.id,
                "itemName" to item.itemName,
                "category" to item.category,
                "variety" to item.variety,
                "sku" to item.sku,
                "initialQuantity" to item.initialQuantity,
                "currentQuantity" to item.currentQuantity,
                "unitPrice" to item.unitPrice,
                "supplierName" to item.supplierName,
                "supplierContact" to item.supplierContact,
                "lowStockThreshold" to item.lowStockThreshold,
                "createdAt" to item.createdAt
            )
            collectionRef.document(item.id.toString()).set(itemMap, SetOptions.merge()).await()
            updateSyncState(SyncState.SYNCED)
        } catch (e: Exception) {
            updateSyncState(SyncState.OFFLINE)
            Log.e(TAG, "Error saving inventory item to Firestore: ${e.message}")
        }
    }

    suspend fun deleteInventoryItem(itemId: Long) {
        val collectionRef = getUserCollection(COLLECTION_INVENTORY) ?: return
        try {
            collectionRef.document(itemId.toString()).delete().await()
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting inventory item from Firestore: ${e.message}")
        }
    }

    suspend fun syncInventoryFromCloudToLocal(inventoryDao: InventoryDao) {
        val collectionRef = getUserCollection(COLLECTION_INVENTORY) ?: return
        try {
            val snapshot = collectionRef.get().await()
            for (doc in snapshot.documents) {
                val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: continue
                val itemName = doc.getString("itemName") ?: ""
                val category = doc.getString("category") ?: ""
                val variety = doc.getString("variety") ?: ""
                val sku = doc.getString("sku") ?: ""
                val initialQuantity = doc.getLong("initialQuantity")?.toInt() ?: 0
                val currentQuantity = doc.getLong("currentQuantity")?.toInt() ?: 0
                val unitPrice = doc.getDouble("unitPrice") ?: 0.0
                val supplierName = doc.getString("supplierName") ?: ""
                val supplierContact = doc.getString("supplierContact") ?: ""
                val lowStockThreshold = doc.getLong("lowStockThreshold")?.toInt() ?: 5
                val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()

                val item = InventoryItem(
                    id = id,
                    itemName = itemName,
                    category = category,
                    variety = variety,
                    sku = sku,
                    initialQuantity = initialQuantity,
                    currentQuantity = currentQuantity,
                    unitPrice = unitPrice,
                    supplierName = supplierName,
                    supplierContact = supplierContact,
                    lowStockThreshold = lowStockThreshold,
                    createdAt = createdAt
                )
                inventoryDao.insertItem(item)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing inventory from Firestore: ${e.message}")
        }
    }

    suspend fun deleteAdvancePayment(paymentId: Long) {
        updateSyncState(SyncState.SYNCING)
        val collectionRef = getUserCollection(COLLECTION_ADVANCE) ?: return
        try {
            collectionRef.document(paymentId.toString()).delete().await()
            updateSyncState(SyncState.SYNCED)
        } catch (e: Exception) {
            updateSyncState(SyncState.OFFLINE)
            Log.e(TAG, "Error deleting advance payment $paymentId: ${e.message}")
        }
    }

    suspend fun deleteWorker(workerId: Long) {
        updateSyncState(SyncState.SYNCING)
        val collectionRef = getUserCollection(COLLECTION_WORKERS) ?: return
        try {
            collectionRef.document(workerId.toString()).delete().await()
            updateSyncState(SyncState.SYNCED)
        } catch (e: Exception) {
            updateSyncState(SyncState.OFFLINE)
            Log.e(TAG, "Error deleting worker $workerId: ${e.message}")
        }
    }
}
