package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recycle_bin")
data class RecycleBinEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val itemType: String, // "BOOKING", "CONTACT"
    val title: String,
    val subtitle: String,
    val jsonPayload: String,
    val deletedAt: Long = System.currentTimeMillis()
)
