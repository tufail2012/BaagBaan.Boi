package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Upsert
    suspend fun insertOrUpdateAttendance(record: AttendanceRecord)

    @Upsert
    suspend fun insertOrUpdateAttendanceList(records: List<AttendanceRecord>)

    @Query("SELECT * FROM workers WHERE isActive = 1 ORDER BY name ASC")
    fun getAllActiveWorkers(): Flow<List<Worker>>

    @Query("SELECT * FROM workers ORDER BY name ASC")
    fun getAllWorkers(): Flow<List<Worker>>

    @Query("SELECT * FROM workers ORDER BY name ASC")
    suspend fun getAllWorkersSync(): List<Worker>

    @Query("SELECT * FROM attendance_records")
    suspend fun getAllAttendanceRecordsSync(): List<AttendanceRecord>

    @Query("SELECT * FROM advance_payments")
    suspend fun getAllAdvancePaymentsSync(): List<AdvancePayment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorker(worker: Worker): Long

    @Update
    suspend fun updateWorker(worker: Worker)

    @Query("UPDATE workers SET isActive = 0 WHERE workerId = :workerId")
    suspend fun deactivateWorker(workerId: Long)

    @Query("SELECT * FROM workers WHERE workerId = :workerId")
    suspend fun getWorkerById(workerId: Long): Worker?

    @Query("SELECT * FROM attendance_records WHERE date = :date")
    fun getAttendanceForDate(date: String): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE workerId = :workerId AND date >= :startDate AND date <= :endDate")
    fun getAttendanceForWorkerInRange(workerId: Long, startDate: String, endDate: String): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE date >= :startDate AND date <= :endDate")
    fun getAttendanceInRange(startDate: String, endDate: String): Flow<List<AttendanceRecord>>

    @Query("SELECT COUNT(*) FROM attendance_records WHERE workerId = :workerId AND status = 'PRESENT'")
    fun getPresentCount(workerId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM attendance_records WHERE workerId = :workerId AND status = 'ABSENT'")
    fun getAbsentCount(workerId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM attendance_records WHERE workerId = :workerId AND status = 'PRESENT' AND date >= :startDate AND date <= :endDate")
    fun getPresentCountInRange(workerId: Long, startDate: String, endDate: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM attendance_records WHERE workerId = :workerId AND status = 'ABSENT' AND date >= :startDate AND date <= :endDate")
    fun getAbsentCountInRange(workerId: Long, startDate: String, endDate: String): Flow<Int>

    @Query("DELETE FROM attendance_records WHERE workerId = :workerId AND date = :date")
    suspend fun deleteAttendanceForWorkerAndDate(workerId: Long, date: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdvancePayment(payment: AdvancePayment): Long

    @Query("SELECT * FROM advance_payments WHERE workerId = :workerId ORDER BY date DESC, timestamp DESC")
    fun getAdvancePaymentsForWorker(workerId: Long): Flow<List<AdvancePayment>>

    @Query("SELECT SUM(amount) FROM advance_payments WHERE workerId = :workerId")
    fun getTotalAdvanceForWorker(workerId: Long): Flow<Double?>

    @Query("DELETE FROM advance_payments WHERE paymentId = :paymentId")
    suspend fun deleteAdvancePayment(paymentId: Long)
}
