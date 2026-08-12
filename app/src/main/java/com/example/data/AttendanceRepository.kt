package com.example.data

import kotlinx.coroutines.flow.Flow

class AttendanceRepository(
    private val dao: AttendanceDao,
    private val firestoreSyncManager: FirestoreSyncManager = FirestoreSyncManager()
) {

    val activeWorkers: Flow<List<Worker>> = dao.getAllActiveWorkers()
    val allWorkers: Flow<List<Worker>> = dao.getAllWorkers()

    suspend fun insertWorker(worker: Worker): Long {
        val insertedId = dao.insertWorker(worker)
        val workerToSync = if (worker.workerId == 0L) worker.copy(workerId = insertedId) else worker
        firestoreSyncManager.saveWorker(workerToSync)
        return insertedId
    }

    suspend fun updateWorker(worker: Worker) {
        dao.updateWorker(worker)
        firestoreSyncManager.saveWorker(worker)
    }

    suspend fun deactivateWorker(workerId: Long) {
        dao.deactivateWorker(workerId)
        val worker = dao.getWorkerById(workerId)
        if (worker != null) {
            firestoreSyncManager.saveWorker(worker)
        }
    }

    suspend fun insertOrUpdateAttendance(record: AttendanceRecord) {
        dao.insertOrUpdateAttendance(record)
        firestoreSyncManager.saveAttendanceRecord(record)
    }

    suspend fun insertOrUpdateAttendanceList(records: List<AttendanceRecord>) {
        dao.insertOrUpdateAttendanceList(records)
        firestoreSyncManager.saveAttendanceList(records)
    }

    suspend fun deleteAttendanceForWorkerAndDate(workerId: Long, date: String) {
        dao.deleteAttendanceForWorkerAndDate(workerId, date)
    }

    fun getAttendanceForDate(date: String): Flow<List<AttendanceRecord>> {
        return dao.getAttendanceForDate(date)
    }

    fun getAttendanceForWorkerInRange(workerId: Long, startDate: String, endDate: String): Flow<List<AttendanceRecord>> {
        return dao.getAttendanceForWorkerInRange(workerId, startDate, endDate)
    }

    fun getAttendanceInRange(startDate: String, endDate: String): Flow<List<AttendanceRecord>> {
        return dao.getAttendanceInRange(startDate, endDate)
    }

    fun getPresentCount(workerId: Long): Flow<Int> {
        return dao.getPresentCount(workerId)
    }

    fun getAbsentCount(workerId: Long): Flow<Int> {
        return dao.getAbsentCount(workerId)
    }

    fun getPresentCountInRange(workerId: Long, startDate: String, endDate: String): Flow<Int> {
        return dao.getPresentCountInRange(workerId, startDate, endDate)
    }

    fun getAbsentCountInRange(workerId: Long, startDate: String, endDate: String): Flow<Int> {
        return dao.getAbsentCountInRange(workerId, startDate, endDate)
    }

    suspend fun insertAdvancePayment(payment: AdvancePayment): Long {
        val insertedId = dao.insertAdvancePayment(payment)
        val paymentToSync = if (payment.paymentId == 0L) payment.copy(paymentId = insertedId) else payment
        firestoreSyncManager.saveAdvancePayment(paymentToSync)
        return insertedId
    }

    fun getAdvancePaymentsForWorker(workerId: Long): Flow<List<AdvancePayment>> {
        return dao.getAdvancePaymentsForWorker(workerId)
    }

    fun getTotalAdvanceForWorker(workerId: Long): Flow<Double?> {
        return dao.getTotalAdvanceForWorker(workerId)
    }

    suspend fun deleteAdvancePayment(paymentId: Long) {
        dao.deleteAdvancePayment(paymentId)
        firestoreSyncManager.deleteAdvancePayment(paymentId)
    }
}
