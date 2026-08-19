package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "garden_planning_entries")
data class GardenPlanningEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val serialNumber: String = "",
    val farmerName: String = "",
    val farmerAddress: String = "",
    val contactNumber: String = "",
    val totalKanalArea: Double = 0.0,
    val plantsPerKanal: Int = 0,
    val costPerPlant: Double = 0.0,
    val plantVariety: String = "",
    val rootStock: String = "",
    val saplingAge: String = "1 Year",
    val plantOrigin: String = "Local Plants",
    val totalCost: Double = 0.0,
    val amountPaid: Double = 0.0,
    val remainingBalance: Double = 0.0,
    val paymentStatus: String = "Pending",
    val bookingDate: String = "",
    val expectedDelivery: String = "",
    val notes: String = "",
    val installmentHistoryJson: String = "",
    val isReceived: Boolean = false,
    val receivedDate: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
