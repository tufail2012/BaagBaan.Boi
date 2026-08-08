package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workers")
data class Worker(
    @PrimaryKey(autoGenerate = true)
    val workerId: Long = 0,
    val name: String,
    val phoneNumber: String = "",
    val dailyRate: Double = 0.0,
    val advancePaid: Double = 0.0,
    val isActive: Boolean = true
)
