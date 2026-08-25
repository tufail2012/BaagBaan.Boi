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
    val isReceived: Boolean = false,
    val receivedDate: String = "",
    val isCancelled: Boolean = false,
    val cancelledDate: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class VarietyLine(
    val variety: String = "",
    val rootstock: String = "",
    val feathers: String = "",
    val kanalArea: Double = 0.0,
    val plantsPerKanal: Int = 0,
    val totalPlants: Int = 0,
    val unitPrice: Double = 0.0,
    val quantity: Int = if (totalPlants > 0) totalPlants else 0,
    val kanalAreaStr: String = if (kanalArea > 0) (if (kanalArea % 1.0 == 0.0) kanalArea.toInt().toString() else kanalArea.toString()) else "",
    val plantsPerKanalStr: String = if (plantsPerKanal > 0) plantsPerKanal.toString() else "",
    val totalPlantsStr: String = if (totalPlants > 0) totalPlants.toString() else if (quantity > 0) quantity.toString() else "",
    val unitPriceStr: String = if (unitPrice > 0.0) (if (unitPrice % 1.0 == 0.0) unitPrice.toInt().toString() else unitPrice.toString()) else ""
) {
    val effectiveQuantity: Int get() = if (quantity > 0) quantity else totalPlants

    // Secondary constructor for backward compatibility with 5-arg positional calls: (variety, quantity, unitPrice, rootstock, feathers)
    constructor(
        variety: String,
        quantity: Int,
        unitPrice: Double,
        rootstock: String = "",
        feathers: String = ""
    ) : this(
        variety = variety,
        rootstock = rootstock,
        feathers = feathers,
        kanalArea = 0.0,
        plantsPerKanal = 0,
        totalPlants = quantity,
        unitPrice = unitPrice,
        quantity = quantity,
        totalPlantsStr = if (quantity > 0) quantity.toString() else "",
        unitPriceStr = if (unitPrice > 0.0) (if (unitPrice % 1.0 == 0.0) unitPrice.toInt().toString() else unitPrice.toString()) else ""
    )
}

fun parseVarietyLines(json: String): List<VarietyLine> {
    if (json.isBlank()) return emptyList()
    return try {
        val array = org.json.JSONArray(json)
        val list = mutableListOf<VarietyLine>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val variety = obj.optString("variety", "")
            val rootstock = obj.optString("rootstock", "")
            val feathers = obj.optString("feathers", "")
            val kanalArea = obj.optDouble("kanalArea", 0.0)
            val plantsPerKanal = obj.optInt("plantsPerKanal", 0)
            val rawQty = obj.optInt("quantity", 0)
            val totalPlants = obj.optInt("totalPlants", rawQty)
            val unitPrice = obj.optDouble("unitPrice", 0.0)
            val effectivePlants = if (rawQty > 0) rawQty else if (totalPlants > 0) totalPlants else 0
            val effectiveKanalArea = if (kanalArea > 0.0) kanalArea else if (plantsPerKanal > 0 && effectivePlants > 0) effectivePlants.toDouble() / plantsPerKanal else 0.0
            if (variety.isNotBlank() || effectivePlants > 0) {
                list.add(
                    VarietyLine(
                        variety = variety,
                        rootstock = rootstock,
                        feathers = feathers,
                        kanalArea = effectiveKanalArea,
                        plantsPerKanal = plantsPerKanal,
                        totalPlants = effectivePlants,
                        unitPrice = unitPrice,
                        quantity = effectivePlants,
                        totalPlantsStr = if (effectivePlants > 0) effectivePlants.toString() else "",
                        unitPriceStr = if (unitPrice > 0.0) (if (unitPrice % 1.0 == 0.0) unitPrice.toInt().toString() else unitPrice.toString()) else ""
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
        val plantCount = if (line.quantity > 0) line.quantity else if (line.totalPlants > 0) line.totalPlants else 0
        obj.put("variety", line.variety)
        obj.put("rootstock", line.rootstock)
        obj.put("feathers", line.feathers)
        obj.put("kanalArea", line.kanalArea)
        obj.put("plantsPerKanal", line.plantsPerKanal)
        obj.put("totalPlants", plantCount)
        obj.put("quantity", plantCount)
        obj.put("unitPrice", line.unitPrice)
        array.put(obj)
    }
    return array.toString()
}

fun serializeVarietyLines(lines: List<VarietyLine>): String = formatVarietyLinesJson(lines)

fun calculateTotalAmountMultiVariety(lines: List<VarietyLine>): Double {
    return lines.sumOf { (if (it.quantity > 0) it.quantity else it.totalPlants) * it.unitPrice }
}

fun CropRecord.calculateTotalAmount(): Double {
    if (varietyLinesJson.isNotBlank()) {
        val lines = parseVarietyLines(varietyLinesJson)
        if (lines.isNotEmpty()) {
            return calculateTotalAmountMultiVariety()
        }
    }
    val base = quantity * landAreaAcres
    val isRootstock = serviceType.equals("Rootstocks", ignoreCase = true) || serviceType.contains("Rootstock", ignoreCase = true) || serviceType.equals("Imported", ignoreCase = true)
    val graftMatch = if (isRootstock) Regex("Grafting Charges:\\s*₹?\\s*([0-9.]+)").find(notes) else null
    val graftAmount = graftMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
    return base + graftAmount
}

fun CropRecord.calculateTotalAmountMultiVariety(): Double {
    val lines = parseVarietyLines(varietyLinesJson)
    if (lines.isEmpty()) {
        val base = quantity * landAreaAcres
        val isRootstock = serviceType.equals("Rootstocks", ignoreCase = true) || serviceType.contains("Rootstock", ignoreCase = true) || serviceType.equals("Imported", ignoreCase = true)
        val graftMatch = if (isRootstock) Regex("Grafting Charges:\\s*₹?\\s*([0-9.]+)").find(notes) else null
        val graftAmount = graftMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
        return base + graftAmount
    }
    val base = lines.sumOf { (if (it.quantity > 0) it.quantity else it.totalPlants) * it.unitPrice }
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

