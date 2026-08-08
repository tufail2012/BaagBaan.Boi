package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "farmer_contacts")
data class FarmerContact(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phone: String,
    val address: String = "",
    val category: String = "Farmer", // e.g. "Farmer", "Supplier", "Customer", "Worker"
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
