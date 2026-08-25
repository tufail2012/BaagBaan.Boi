package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CropRecordDao {
    @Query("SELECT * FROM crop_records ORDER BY timestamp DESC, id DESC")
    fun getAllRecords(): Flow<List<CropRecord>>

    @Query("SELECT * FROM crop_records ORDER BY timestamp DESC, id DESC")
    suspend fun getAllRecordsList(): List<CropRecord>

    @Query("SELECT * FROM crop_records WHERE serviceType = :serviceType ORDER BY timestamp DESC, id DESC")
    fun getRecordsByService(serviceType: String): Flow<List<CropRecord>>

    @Query("""
        SELECT * FROM crop_records 
        WHERE farmerName LIKE '%' || :query || '%' 
           OR farmerAddress LIKE '%' || :query || '%' 
           OR contactNumber LIKE '%' || :query || '%' 
           OR plantVariety LIKE '%' || :query || '%'
           OR serialNumber LIKE '%' || :query || '%'
        ORDER BY timestamp DESC, id DESC
    """)
    fun searchRecords(query: String): Flow<List<CropRecord>>

    @Query("SELECT * FROM crop_records WHERE id = :id")
    fun getRecordById(id: Long): Flow<CropRecord?>

    @Query("SELECT COUNT(*) FROM crop_records WHERE (:phone != '' AND contactNumber = :phone) OR (:phone = '' AND :name != '' AND farmerName = :name AND (contactNumber = '' OR contactNumber IS NULL))")
    suspend fun countRecordsByFarmer(phone: String, name: String): Int

    @Query("SELECT * FROM crop_records WHERE (:phone != '' AND contactNumber = :phone) OR (:phone = '' AND :name != '' AND farmerName = :name AND (contactNumber = '' OR contactNumber IS NULL)) ORDER BY timestamp DESC")
    suspend fun getRecordsByFarmer(phone: String, name: String): List<CropRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: CropRecord): Long

    @Update
    suspend fun updateRecord(record: CropRecord)

    @Delete
    suspend fun deleteRecord(record: CropRecord)

    @Query("DELETE FROM crop_records")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM crop_records")
    fun getRecordCount(): Flow<Int>
}
