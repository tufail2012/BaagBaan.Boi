package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GardenPlanningDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: GardenPlanningEntry): Long

    @Update
    suspend fun updateEntry(entry: GardenPlanningEntry)

    @Delete
    suspend fun deleteEntry(entry: GardenPlanningEntry)

    @Query("SELECT * FROM garden_planning_entries ORDER BY id DESC")
    fun getAllEntries(): Flow<List<GardenPlanningEntry>>

    @Query("SELECT * FROM garden_planning_entries ORDER BY id DESC")
    suspend fun getAllEntriesList(): List<GardenPlanningEntry>

    @Query("SELECT * FROM garden_planning_entries WHERE id = :id")
    fun getEntryById(id: Long): Flow<GardenPlanningEntry?>

    @Query("SELECT * FROM garden_planning_entries WHERE id = :id")
    suspend fun getEntryByIdSync(id: Long): GardenPlanningEntry?

    @Query("SELECT * FROM garden_planning_entries WHERE farmerName LIKE '%' || :query || '%' OR serialNumber LIKE '%' || :query || '%' OR contactNumber LIKE '%' || :query || '%' ORDER BY id DESC")
    fun searchEntries(query: String): Flow<List<GardenPlanningEntry>>

    @Query("DELETE FROM garden_planning_entries")
    suspend fun deleteAll()
}
