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
    val varietyLinesJson: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class VarietyLine(
    val variety: String,
    val quantity: Int,
    val unitPrice: Double,
    val rootstock: String = "",
    val feathers: String = ""
)

fun parseVarietyLines(json: String): List<VarietyLine> {
    if (json.isBlank()) return emptyList()
    return try {
        val array = org.json.JSONArray(json)
        val list = mutableListOf<VarietyLine>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val variety = obj.optString("variety", "")
            val quantity = obj.optInt("quantity", 0)
            val unitPrice = obj.optDouble("unitPrice", 0.0)
            val rootstock = obj.optString("rootstock", "")
            val feathers = obj.optString("feathers", "")
            if (variety.isNotBlank() || quantity > 0) {
                list.add(
                    VarietyLine(
                        variety = variety,
                        quantity = quantity,
                        unitPrice = unitPrice,
                        rootstock = rootstock,
                        feathers = feathers
                    )
                )
            }
        }
        list
    } catch (e: Exception) {
        emptyList()
    }
}

fun formatVarietyLinesJson(lines: List<VarietyLine>): String {
    if (lines.isEmpty()) return ""
    val array = org.json.JSONArray()
    for (line in lines) {
        val obj = org.json.JSONObject()
        obj.put("variety", line.variety)
        obj.put("quantity", line.quantity)
        obj.put("unitPrice", line.unitPrice)
        obj.put("rootstock", line.rootstock)
        obj.put("feathers", line.feathers)
        array.put(obj)
    }
    return array.toString()
}

fun serializeVarietyLines(lines: List<VarietyLine>): String = formatVarietyLinesJson(lines)

fun calculateTotalAmountMultiVariety(lines: List<VarietyLine>): Double {
    return lines.sumOf { it.quantity * it.unitPrice }
}

fun CropRecord.calculateTotalAmount(): Double {
    val base = quantity * landAreaAcres
    val isRootstock = serviceType.equals("Rootstocks", ignoreCase = true) || serviceType.contains("Rootstock", ignoreCase = true) || serviceType.equals("Imported", ignoreCase = true)
    val graftMatch = if (isRootstock) Regex("Grafting Charges:\\s*₹?\\s*([0-9.]+)").find(notes) else null
    val graftAmount = graftMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
    return base + graftAmount
}

fun CropRecord.calculateTotalAmountMultiVariety(): Double {
    val lines = parseVarietyLines(varietyLinesJson)
    if (lines.isEmpty()) return calculateTotalAmount() // exact existing behavior, untouched
    val base = lines.sumOf { it.quantity * it.unitPrice }
    val isRootstock = serviceType.equals("Rootstocks", ignoreCase = true) || serviceType.contains("Rootstock", ignoreCase = true) || serviceType.equals("Imported", ignoreCase = true)
    val graftMatch = if (isRootstock) Regex("Grafting Charges:\\s*₹?\\s*([0-9.]+)").find(notes) else null
    val graftAmount = graftMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
    return base + graftAmount
}

fun CropRecord.calculateRemainingBalance(): Double {
    return maxOf(0.0, calculateTotalAmountMultiVariety() - amountPaid)
}

fun CropRecord.isPaymentCleared(): Boolean {
    return paymentStatus.equals("Fully Paid", ignoreCase = true) || calculateRemainingBalance() <= 0.01
}

