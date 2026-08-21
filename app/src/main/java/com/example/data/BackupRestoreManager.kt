package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RestoreSummary(
    val cropRecordsCount: Int = 0,
    val workersCount: Int = 0,
    val attendanceRecordsCount: Int = 0,
    val advancePaymentsCount: Int = 0,
    val userBookingsCount: Int = 0,
    val userAttendanceCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
) {
    val totalCount: Int
        get() = cropRecordsCount + workersCount + attendanceRecordsCount + advancePaymentsCount + userBookingsCount + userAttendanceCount
}

class BackupRestoreManager {

    private val db get() = com.example.util.SafeFirebase.db
    private val auth get() = com.example.util.SafeFirebase.auth

    companion object {
        private const val TAG = "BackupRestoreManager"
        const val COLLECTION_CROP_RECORDS = "crop_records"
        const val COLLECTION_WORKERS = "workers"
        const val COLLECTION_ATTENDANCE = "attendance_records"
        const val COLLECTION_ADVANCE = "advance_payments"
        const val COLLECTION_USERS = "users"
        const val COLLECTION_BOOKINGS = "bookings"
        const val COLLECTION_USER_ATTENDANCE = "attendance"
    }

    private fun getCollectionRef(collectionName: String): CollectionReference? {
        val uid = auth?.currentUser?.uid
        val firestore = db
        if (uid.isNullOrEmpty() || firestore == null) {
            return null
        }
        return firestore.collection(COLLECTION_USERS).document(uid).collection(collectionName)
    }

    /**
     * Exports current Firestore data (and Room local database data) to a formatted JSON File.
     */
    suspend fun exportDataToJson(context: Context, database: AppDatabase): Result<File> = withContext(Dispatchers.IO) {
        try {
            val uid = auth?.currentUser?.uid

            // 1. Fetch Crop Records from Firestore (or Local Room DB fallback)
            val cropRecordsJson = JSONArray()
            val firestoreCrops = try {
                getCollectionRef(COLLECTION_CROP_RECORDS)?.get()?.await()
            } catch (e: Exception) {
                null
            }

            val cropRecordList = mutableListOf<CropRecord>()
            if (firestoreCrops != null && !firestoreCrops.isEmpty) {
                for (doc in firestoreCrops.documents) {
                    val id = doc.getLong("id") ?: continue
                    cropRecordList.add(
                        CropRecord(
                            id = id,
                            serialNumber = doc.getString("serialNumber") ?: "",
                            farmerName = doc.getString("farmerName") ?: "",
                            farmerAddress = doc.getString("farmerAddress") ?: "",
                            contactNumber = doc.getString("contactNumber") ?: "",
                            serviceType = doc.getString("serviceType") ?: "Local Plants",
                            plantVariety = doc.getString("plantVariety") ?: "",
                            rootstock = doc.getString("rootstock") ?: "",
                            feathers = doc.getString("feathers") ?: (doc.getLong("feathers")?.let { if (it > 0) it.toString() else "" } ?: ""),
                            quantity = (doc.getLong("quantity") ?: 1L).toInt(),
                            landAreaAcres = doc.getDouble("landAreaAcres") ?: 1.0,
                            soilType = doc.getString("soilType") ?: "Loamy",
                            healthStage = doc.getString("healthStage") ?: "Active Sapling",
                            location = doc.getString("location") ?: "",
                            notes = doc.getString("notes") ?: "",
                            amountPaid = doc.getDouble("amountPaid") ?: 0.0,
                            paymentStatus = doc.getString("paymentStatus") ?: "Pending",
                            bookingDate = doc.getString("bookingDate") ?: "",
                            expectedDelivery = doc.getString("expectedDelivery") ?: "",
                            paymentProofUri = doc.getString("paymentProofUri") ?: "",
                            paymentHistoryJson = doc.getString("paymentHistoryJson") ?: "",
                            varietyLinesJson = doc.getString("varietyLinesJson") ?: "",
                            timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                        )
                    )
                }
            } else {
                // Fallback to local Room database
                val localCrops = database.cropRecordDao().getAllRecordsList()
                cropRecordList.addAll(localCrops)
            }

            cropRecordList.forEach { record ->
                val obj = JSONObject()
                obj.put("id", record.id)
                obj.put("serialNumber", record.serialNumber)
                obj.put("farmerName", record.farmerName)
                obj.put("farmerAddress", record.farmerAddress)
                obj.put("contactNumber", record.contactNumber)
                obj.put("serviceType", record.serviceType)
                obj.put("plantVariety", record.plantVariety)
                obj.put("rootstock", record.rootstock)
                obj.put("feathers", record.feathers)
                obj.put("quantity", record.quantity)
                obj.put("landAreaAcres", record.landAreaAcres)
                obj.put("soilType", record.soilType)
                obj.put("healthStage", record.healthStage)
                obj.put("location", record.location)
                obj.put("notes", record.notes)
                obj.put("amountPaid", record.amountPaid)
                obj.put("paymentStatus", record.paymentStatus)
                obj.put("bookingDate", record.bookingDate)
                obj.put("expectedDelivery", record.expectedDelivery)
                obj.put("paymentProofUri", record.paymentProofUri)
                obj.put("paymentHistoryJson", record.paymentHistoryJson)
                obj.put("varietyLinesJson", record.varietyLinesJson)
                obj.put("timestamp", record.timestamp)
                cropRecordsJson.put(obj)
            }

            // 2. Fetch Workers
            val workersJson = JSONArray()
            val firestoreWorkers = try {
                getCollectionRef(COLLECTION_WORKERS)?.get()?.await()
            } catch (e: Exception) {
                null
            }

            val workerList = mutableListOf<Worker>()
            if (firestoreWorkers != null && !firestoreWorkers.isEmpty) {
                for (doc in firestoreWorkers.documents) {
                    val wId = doc.getLong("workerId") ?: continue
                    workerList.add(
                        Worker(
                            workerId = wId,
                            name = doc.getString("name") ?: "",
                            phoneNumber = doc.getString("phoneNumber") ?: "",
                            dailyRate = doc.getDouble("dailyRate") ?: 0.0,
                            advancePaid = doc.getDouble("advancePaid") ?: 0.0,
                            isActive = doc.getBoolean("isActive") ?: true
                        )
                    )
                }
            } else {
                val localWorkers = database.attendanceDao().getAllWorkersSync()
                workerList.addAll(localWorkers)
            }

            workerList.forEach { worker ->
                val obj = JSONObject()
                obj.put("workerId", worker.workerId)
                obj.put("name", worker.name)
                obj.put("phoneNumber", worker.phoneNumber)
                obj.put("dailyRate", worker.dailyRate)
                obj.put("advancePaid", worker.advancePaid)
                obj.put("isActive", worker.isActive)
                workersJson.put(obj)
            }

            // 3. Fetch Attendance Records
            val attendanceJson = JSONArray()
            val firestoreAttendance = try {
                getCollectionRef(COLLECTION_ATTENDANCE)?.get()?.await()
            } catch (e: Exception) {
                null
            }

            val attendanceList = mutableListOf<AttendanceRecord>()
            if (firestoreAttendance != null && !firestoreAttendance.isEmpty) {
                for (doc in firestoreAttendance.documents) {
                    val wId = doc.getLong("workerId") ?: continue
                    val date = doc.getString("date") ?: continue
                    val statusStr = doc.getString("status") ?: "PRESENT"
                    val status = try { AttendanceStatus.valueOf(statusStr) } catch (e: Exception) { AttendanceStatus.PRESENT }
                    attendanceList.add(
                        AttendanceRecord(
                            id = doc.getLong("id") ?: 0L,
                            workerId = wId,
                            date = date,
                            status = status,
                            markedAt = doc.getLong("markedAt") ?: System.currentTimeMillis()
                        )
                    )
                }
            } else {
                val localAttendance = database.attendanceDao().getAllAttendanceRecordsSync()
                attendanceList.addAll(localAttendance)
            }

            attendanceList.forEach { record ->
                val obj = JSONObject()
                obj.put("id", record.id)
                obj.put("workerId", record.workerId)
                obj.put("date", record.date)
                obj.put("status", record.status.name)
                obj.put("markedAt", record.markedAt)
                attendanceJson.put(obj)
            }

            // 4. Fetch Advance Payments
            val paymentsJson = JSONArray()
            val firestorePayments = try {
                getCollectionRef(COLLECTION_ADVANCE)?.get()?.await()
            } catch (e: Exception) {
                null
            }

            val paymentsList = mutableListOf<AdvancePayment>()
            if (firestorePayments != null && !firestorePayments.isEmpty) {
                for (doc in firestorePayments.documents) {
                    val pId = doc.getLong("paymentId") ?: continue
                    val wId = doc.getLong("workerId") ?: continue
                    paymentsList.add(
                        AdvancePayment(
                            paymentId = pId,
                            workerId = wId,
                            amount = doc.getDouble("amount") ?: 0.0,
                            date = doc.getString("date") ?: "",
                            note = doc.getString("note") ?: doc.getString("notes") ?: "",
                            timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                        )
                    )
                }
            } else {
                val localPayments = database.attendanceDao().getAllAdvancePaymentsSync()
                paymentsList.addAll(localPayments)
            }

            paymentsList.forEach { payment ->
                val obj = JSONObject()
                obj.put("paymentId", payment.paymentId)
                obj.put("workerId", payment.workerId)
                obj.put("amount", payment.amount)
                obj.put("date", payment.date)
                obj.put("note", payment.note)
                obj.put("timestamp", payment.timestamp)
                paymentsJson.put(obj)
            }

            // 5. Fetch Inventory Items
            val inventoryJson = JSONArray()
            val localInventory = database.inventoryDao().getAllItemsSync()
            localInventory.forEach { item ->
                val obj = JSONObject()
                obj.put("id", item.id)
                obj.put("itemName", item.itemName)
                obj.put("category", item.category)
                obj.put("variety", item.variety)
                obj.put("sku", item.sku)
                obj.put("initialQuantity", item.initialQuantity)
                obj.put("currentQuantity", item.currentQuantity)
                obj.put("unitPrice", item.unitPrice)
                obj.put("supplierName", item.supplierName)
                obj.put("supplierContact", item.supplierContact)
                obj.put("lowStockThreshold", item.lowStockThreshold)
                obj.put("createdAt", item.createdAt)
                inventoryJson.put(obj)
            }

            // 6. Fetch User Bookings & User Attendance (if user is authenticated)
            val userBookingsJson = JSONArray()
            val userAttendanceJson = JSONArray()
            val firestoreDb = db
            if (!uid.isNullOrEmpty() && firestoreDb != null) {
                try {
                    val bookingsSnap = firestoreDb.collection(COLLECTION_USERS)
                        .document(uid)
                        .collection(COLLECTION_BOOKINGS)
                        .get().await()

                    for (doc in bookingsSnap.documents) {
                        val obj = JSONObject()
                        obj.put("id", doc.id)
                        obj.put("type", doc.getString("type") ?: "")
                        obj.put("itemName", doc.getString("itemName") ?: "")
                        obj.put("variety", doc.getString("variety") ?: "")
                        obj.put("season", doc.getString("season") ?: "")
                        obj.put("farmerName", doc.getString("farmerName") ?: "")
                        obj.put("quantity", (doc.getLong("quantity") ?: 0L).toInt())
                        obj.put("bookingDate", doc.getString("bookingDate") ?: "")
                        obj.put("notes", doc.getString("notes") ?: "")
                        obj.put("createdAt", doc.getLong("createdAt") ?: System.currentTimeMillis())
                        userBookingsJson.put(obj)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching user bookings for backup: ${e.message}")
                }

                try {
                    val attendanceSnap = firestoreDb.collection(COLLECTION_USERS)
                        .document(uid)
                        .collection(COLLECTION_USER_ATTENDANCE)
                        .get().await()

                    for (doc in attendanceSnap.documents) {
                        val obj = JSONObject()
                        obj.put("id", doc.id)
                        obj.put("workerName", doc.getString("workerName") ?: "")
                        obj.put("date", doc.getString("date") ?: "")
                        obj.put("status", doc.getString("status") ?: "")
                        obj.put("notes", doc.getString("notes") ?: "")
                        obj.put("createdAt", doc.getLong("createdAt") ?: System.currentTimeMillis())
                        userAttendanceJson.put(obj)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching user attendance for backup: ${e.message}")
                }
            }

            // Combine into root JSON document
            val rootJson = JSONObject()

            val metadata = JSONObject()
            val nowStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            metadata.put("backupVersion", 1)
            metadata.put("exportedAt", nowStr)
            metadata.put("timestamp", System.currentTimeMillis())
            metadata.put("app", "AgriCrop Orchard Manager")

            val counts = JSONObject()
            counts.put("crop_records", cropRecordsJson.length())
            counts.put("workers", workersJson.length())
            counts.put("attendance_records", attendanceJson.length())
            counts.put("advance_payments", paymentsJson.length())
            counts.put("inventory_items", inventoryJson.length())
            counts.put("user_bookings", userBookingsJson.length())
            counts.put("user_attendance", userAttendanceJson.length())
            metadata.put("counts", counts)

            rootJson.put("backupMetadata", metadata)

            val dataObj = JSONObject()
            dataObj.put("crop_records", cropRecordsJson)
            dataObj.put("workers", workersJson)
            dataObj.put("attendance_records", attendanceJson)
            dataObj.put("advance_payments", paymentsJson)
            dataObj.put("inventory_items", inventoryJson)
            dataObj.put("user_bookings", userBookingsJson)
            dataObj.put("user_attendance", userAttendanceJson)

            rootJson.put("data", dataObj)

            // Write to local file
            val timeStampFile = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "BaagBaan_Boi_Backup_$timeStampFile.json"
            val backupFile = File(context.cacheDir, fileName)

            backupFile.writeText(rootJson.toString(2))
            Log.d(TAG, "Exported backup file successfully to ${backupFile.absolutePath}")

            Result.success(backupFile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export data to JSON: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Restores data from a JSON string into Firestore and local Room database.
     */
    suspend fun restoreDataFromJson(
        jsonString: String,
        database: AppDatabase
    ): Result<RestoreSummary> = withContext(Dispatchers.IO) {
        try {
            val rootObj = JSONObject(jsonString)
            val dataObj = if (rootObj.has("data")) {
                rootObj.getJSONObject("data")
            } else {
                rootObj // if user provided plain data json
            }

            val uid = auth?.currentUser?.uid

            var restoredCrops = 0
            var restoredWorkers = 0
            var restoredAttendance = 0
            var restoredPayments = 0
            var restoredUserBookings = 0
            var restoredUserAttendance = 0

            val cropRef = getCollectionRef(COLLECTION_CROP_RECORDS)
            val workerRef = getCollectionRef(COLLECTION_WORKERS)
            val attendanceRef = getCollectionRef(COLLECTION_ATTENDANCE)
            val advanceRef = getCollectionRef(COLLECTION_ADVANCE)

            // 1. Restore Crop Records
            if (dataObj.has("crop_records")) {
                val array = dataObj.getJSONArray("crop_records")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.optLong("id", System.currentTimeMillis() + i)
                    val record = CropRecord(
                        id = id,
                        serialNumber = obj.optString("serialNumber", "CRP-$id"),
                        farmerName = obj.optString("farmerName", ""),
                        farmerAddress = obj.optString("farmerAddress", ""),
                        contactNumber = obj.optString("contactNumber", ""),
                        serviceType = obj.optString("serviceType", "Local Plants"),
                        plantVariety = obj.optString("plantVariety", ""),
                        rootstock = obj.optString("rootstock", ""),
                        feathers = obj.optString("feathers", ""),
                        quantity = obj.optInt("quantity", 1),
                        landAreaAcres = obj.optDouble("landAreaAcres", 1.0),
                        soilType = obj.optString("soilType", "Loamy"),
                        healthStage = obj.optString("healthStage", "Active Sapling"),
                        location = obj.optString("location", ""),
                        notes = obj.optString("notes", ""),
                        amountPaid = obj.optDouble("amountPaid", 0.0),
                        paymentStatus = obj.optString("paymentStatus", "Pending"),
                        bookingDate = obj.optString("bookingDate", ""),
                        expectedDelivery = obj.optString("expectedDelivery", ""),
                        paymentProofUri = obj.optString("paymentProofUri", ""),
                        paymentHistoryJson = obj.optString("paymentHistoryJson", ""),
                        varietyLinesJson = obj.optString("varietyLinesJson", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )

                    // Insert into Room DB
                    database.cropRecordDao().insertRecord(record)

                    // Sync to Firestore
                    if (cropRef != null) {
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
                                "feathers" to record.feathers,
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
                                "varietyLinesJson" to record.varietyLinesJson,
                                "timestamp" to record.timestamp
                            )
                            cropRef
                                .document(record.id.toString())
                                .set(recordMap, SetOptions.merge())
                                .await()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error syncing restored crop record to Firestore: ${e.message}")
                        }
                    }

                    restoredCrops++
                }
            }

            // 2. Restore Workers
            if (dataObj.has("workers")) {
                val array = dataObj.getJSONArray("workers")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val wId = obj.optLong("workerId", System.currentTimeMillis() + i)
                    val worker = Worker(
                        workerId = wId,
                        name = obj.optString("name", "Worker $wId"),
                        phoneNumber = obj.optString("phoneNumber", ""),
                        dailyRate = obj.optDouble("dailyRate", 0.0),
                        advancePaid = obj.optDouble("advancePaid", 0.0),
                        isActive = obj.optBoolean("isActive", true)
                    )

                    database.attendanceDao().insertWorker(worker)

                    if (workerRef != null) {
                        try {
                            val workerMap = hashMapOf(
                                "workerId" to worker.workerId,
                                "name" to worker.name,
                                "phoneNumber" to worker.phoneNumber,
                                "dailyRate" to worker.dailyRate,
                                "advancePaid" to worker.advancePaid,
                                "isActive" to worker.isActive
                            )
                            workerRef
                                .document(worker.workerId.toString())
                                .set(workerMap, SetOptions.merge())
                                .await()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error syncing restored worker to Firestore: ${e.message}")
                        }
                    }

                    restoredWorkers++
                }
            }

            // 3. Restore Attendance Records
            if (dataObj.has("attendance_records")) {
                val array = dataObj.getJSONArray("attendance_records")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val wId = obj.optLong("workerId", 0L)
                    val date = obj.optString("date", "")
                    if (wId > 0 && date.isNotEmpty()) {
                        val statusStr = obj.optString("status", "PRESENT")
                        val status = try { AttendanceStatus.valueOf(statusStr) } catch (e: Exception) { AttendanceStatus.PRESENT }
                        val record = AttendanceRecord(
                            id = obj.optLong("id", 0L),
                            workerId = wId,
                            date = date,
                            status = status,
                            markedAt = obj.optLong("markedAt", System.currentTimeMillis())
                        )

                        database.attendanceDao().insertOrUpdateAttendance(record)

                        if (attendanceRef != null) {
                            try {
                                val attendanceMap = hashMapOf(
                                    "id" to record.id,
                                    "workerId" to record.workerId,
                                    "date" to record.date,
                                    "status" to record.status.name,
                                    "markedAt" to record.markedAt
                                )
                                attendanceRef
                                    .document("${wId}_$date")
                                    .set(attendanceMap, SetOptions.merge())
                                    .await()
                            } catch (e: Exception) {
                                Log.e(TAG, "Error syncing restored attendance to Firestore: ${e.message}")
                            }
                        }

                        restoredAttendance++
                    }
                }
            }

            // 4. Restore Advance Payments
            if (dataObj.has("advance_payments")) {
                val array = dataObj.getJSONArray("advance_payments")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val pId = obj.optLong("paymentId", System.currentTimeMillis() + i)
                    val wId = obj.optLong("workerId", 0L)
                    if (wId > 0) {
                        val payment = AdvancePayment(
                            paymentId = pId,
                            workerId = wId,
                            amount = obj.optDouble("amount", 0.0),
                            date = obj.optString("date", ""),
                            note = obj.optString("note", obj.optString("notes", "")),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        )

                        database.attendanceDao().insertAdvancePayment(payment)

                        if (advanceRef != null) {
                            try {
                                val paymentMap = hashMapOf(
                                    "paymentId" to payment.paymentId,
                                    "workerId" to payment.workerId,
                                    "amount" to payment.amount,
                                    "date" to payment.date,
                                    "note" to payment.note,
                                    "timestamp" to payment.timestamp
                                )
                                advanceRef
                                    .document(pId.toString())
                                    .set(paymentMap, SetOptions.merge())
                                    .await()
                            } catch (e: Exception) {
                                Log.e(TAG, "Error syncing restored advance payment to Firestore: ${e.message}")
                            }
                        }

                        restoredPayments++
                    }
                }
            }

            // 5. Restore User Bookings (if authenticated)
            val firestoreRestoreDb = db
            if (!uid.isNullOrEmpty() && firestoreRestoreDb != null && dataObj.has("user_bookings")) {
                val array = dataObj.getJSONArray("user_bookings")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val docId = obj.optString("id", "").ifEmpty { "restored_booking_$i" }
                    val map = hashMapOf(
                        "id" to docId,
                        "type" to obj.optString("type", ""),
                        "itemName" to obj.optString("itemName", ""),
                        "variety" to obj.optString("variety", ""),
                        "season" to obj.optString("season", ""),
                        "farmerName" to obj.optString("farmerName", ""),
                        "quantity" to obj.optInt("quantity", 1),
                        "bookingDate" to obj.optString("bookingDate", ""),
                        "notes" to obj.optString("notes", ""),
                        "createdAt" to obj.optLong("createdAt", System.currentTimeMillis())
                    )

                    try {
                        firestoreRestoreDb.collection(COLLECTION_USERS)
                            .document(uid)
                            .collection(COLLECTION_BOOKINGS)
                            .document(docId)
                            .set(map, SetOptions.merge())
                            .await()
                        restoredUserBookings++
                    } catch (e: Exception) {
                        Log.e(TAG, "Error syncing restored user booking to Firestore: ${e.message}")
                    }
                }
            }

            // 6. Restore Inventory Items (if present)
            if (dataObj.has("inventory_items")) {
                val array = dataObj.getJSONArray("inventory_items")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val item = InventoryItem(
                        id = obj.optLong("id", 0L),
                        itemName = obj.optString("itemName", ""),
                        category = obj.optString("category", "Local Plants"),
                        variety = obj.optString("variety", ""),
                        sku = obj.optString("sku", ""),
                        initialQuantity = obj.optInt("initialQuantity", 0),
                        currentQuantity = obj.optInt("currentQuantity", obj.optInt("initialQuantity", 0)),
                        unitPrice = obj.optDouble("unitPrice", 0.0),
                        supplierName = obj.optString("supplierName", ""),
                        supplierContact = obj.optString("supplierContact", ""),
                        lowStockThreshold = obj.optInt("lowStockThreshold", 10),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                    database.inventoryDao().insertItem(item)
                }
            }

            // Recalculate and ensure Current Stock is 100% synchronized with confirmed bookings
            try {
                InventoryStockManager.recalculateAllStock(database)
            } catch (e: Exception) {
                Log.e(TAG, "Error synchronizing inventory after restore: ${e.message}")
            }

            // 7. Restore User Attendance (if authenticated)
            if (!uid.isNullOrEmpty() && firestoreRestoreDb != null && dataObj.has("user_attendance")) {
                val array = dataObj.getJSONArray("user_attendance")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val docId = obj.optString("id", "").ifEmpty { "restored_user_att_$i" }
                    val map = hashMapOf(
                        "id" to docId,
                        "workerName" to obj.optString("workerName", ""),
                        "date" to obj.optString("date", ""),
                        "status" to obj.optString("status", ""),
                        "notes" to obj.optString("notes", ""),
                        "createdAt" to obj.optLong("createdAt", System.currentTimeMillis())
                    )

                    try {
                        firestoreRestoreDb.collection(COLLECTION_USERS)
                            .document(uid)
                            .collection(COLLECTION_USER_ATTENDANCE)
                            .document(docId)
                            .set(map, SetOptions.merge())
                            .await()
                        restoredUserAttendance++
                    } catch (e: Exception) {
                        Log.e(TAG, "Error syncing restored user attendance to Firestore: ${e.message}")
                    }
                }
            }

            val summary = RestoreSummary(
                cropRecordsCount = restoredCrops,
                workersCount = restoredWorkers,
                attendanceRecordsCount = restoredAttendance,
                advancePaymentsCount = restoredPayments,
                userBookingsCount = restoredUserBookings,
                userAttendanceCount = restoredUserAttendance
            )

            Log.d(TAG, "Successfully restored $summary from JSON")
            Result.success(summary)
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring data from JSON: ${e.message}", e)
            Result.failure(e)
        }
    }
}
