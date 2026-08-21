package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "crop_records")
data class CropRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val serialNumber: String,
    val farmerName: String,
    val farmerAddress: String,
    val contactNumber: String,
    val serviceType: String, // e.g., "Local Plants", "Imported", "Rootstocks", "Site Visit", "Pruning"
    val plantVariety: String, // e.g., "Apple - Red Delicious", "Gala", "Cherry", "Wheat"
    val rootstock: String = "", // e.g., "M9", "MM106", "Seedling"
    val feathers: String = "", // Branches/shoots e.g. "3", "3F", "5A", "2-3", "3+"
    val quantity: Int = 1,
    val landAreaAcres: Double = 1.0,
    val soilType: String = "Loamy",
    val healthStage: String = "Active Sapling",
    val location: String = "",
    val notes: String = "",
    val amountPaid: Double = 0.0,
    val paymentStatus: String = "Pending", // "Pending", "Advance Paid", "Fully Paid"
    val bookingDate: String = "",
    val expectedDelivery: String = "",
    val paymentProofUri: String = "",
    val paymentHistoryJson: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

fun CropRecord.calculateTotalAmount(): Double {
    val base = quantity * landAreaAcres
    val isRootstock = serviceType.equals("Rootstocks", ignoreCase = true) || serviceType.contains("Rootstock", ignoreCase = true) || serviceType.equals("Imported", ignoreCase = true)
    val graftMatch = if (isRootstock) Regex("Grafting Charges:\\s*₹?\\s*([0-9.]+)").find(notes) else null
    val graftAmount = graftMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
    return base + graftAmount
}

fun CropRecord.calculateRemainingBalance(): Double {
    return maxOf(0.0, calculateTotalAmount() - amountPaid)
}

fun CropRecord.isPaymentCleared(): Boolean {
    return paymentStatus.equals("Fully Paid", ignoreCase = true) || calculateRemainingBalance() <= 0.01
}

