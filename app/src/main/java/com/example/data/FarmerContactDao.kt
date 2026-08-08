package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FarmerContactDao {

    @Query("SELECT * FROM farmer_contacts ORDER BY name ASC")
    fun getAllContacts(): Flow<List<FarmerContact>>

    @Query("SELECT * FROM farmer_contacts ORDER BY name ASC")
    suspend fun getAllContactsSync(): List<FarmerContact>

    @Query("SELECT * FROM farmer_contacts WHERE id = :id")
    suspend fun getContactById(id: Long): FarmerContact?

    @Query("SELECT * FROM farmer_contacts WHERE (:phone != '' AND phone = :phone) OR (:name != '' AND name = :name) LIMIT 1")
    suspend fun getContactByPhoneOrName(phone: String, name: String): FarmerContact?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: FarmerContact): Long

    @Update
    suspend fun updateContact(contact: FarmerContact)

    @Delete
    suspend fun deleteContact(contact: FarmerContact)

    @Query("DELETE FROM farmer_contacts WHERE id = :id")
    suspend fun deleteContactById(id: Long)

    @Query("DELETE FROM farmer_contacts WHERE (:phone != '' AND phone = :phone) OR (:name != '' AND name = :name)")
    suspend fun deleteContactByPhoneOrName(phone: String, name: String)
}
