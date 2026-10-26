package com.example.util

import com.example.data.CropRecord
import com.example.data.GardenPlanningEntry
import com.example.data.parseVarietyLines

import kotlin.math.roundToInt

data class VarietyQuantityRow(
    val variety: String,
    val quantity: Int
)

fun CropRecord.isCancelledBooking(): Boolean =
    isCancelled || paymentStatus.equals("Cancelled", ignoreCase = true)

fun GardenPlanningEntry.isCancelledBooking(): Boolean =
    paymentStatus.equals("Cancelled", ignoreCase = true)

fun CropRecord.countedQuantity(): Int {
    if (isCancelledBooking()) return 0
    val lines = parseVarietyLines(varietyLinesJson)
    return if (lines.isNotEmpty()) {
        lines.sumOf { it.effectiveQuantity }
    } else {
        quantity
    }
}

fun GardenPlanningEntry.countedQuantity(): Int {
    if (isCancelledBooking()) return 0
    val lines = parseVarietyLines(varietyLinesJson)
    return if (lines.isNotEmpty()) {
        lines.sumOf { it.quantity }
    } else if (totalKanalArea > 0 && plantsPerKanal > 0) {
        (totalKanalArea * plantsPerKanal).roundToInt()
    } else {
        0
    }
}

fun varietyQuantityRowsForRecords(records: List<CropRecord>): List<VarietyQuantityRow> {
    val pairs = ArrayList<Pair<String, Int>>(records.size)
    for (record in records) {
        if (record.isCancelledBooking()) continue
        val recordFallback = record.plantVariety.trim().ifEmpty { "No variety" }
        val lines = parseVarietyLines(record.varietyLinesJson)
        if (lines.isNotEmpty()) {
            for (line in lines) {
                val varietyName = line.variety.trim().ifEmpty { recordFallback }
                pairs.add(varietyName to line.effectiveQuantity)
            }
        } else {
            pairs.add(recordFallback to record.quantity)
        }
    }
    return groupAndSortVarietyQuantities(pairs)
}

fun varietyQuantityRowsForGarden(entries: List<GardenPlanningEntry>): List<VarietyQuantityRow> {
    val pairs = ArrayList<Pair<String, Int>>(entries.size)
    for (entry in entries) {
        if (entry.isCancelledBooking()) continue
        val entryFallback = entry.plantVariety.trim().ifEmpty { "No variety" }
        val lines = parseVarietyLines(entry.varietyLinesJson)
        if (lines.isNotEmpty()) {
            for (line in lines) {
                val varietyName = line.variety.trim().ifEmpty { entryFallback }
                pairs.add(varietyName to line.quantity)
            }
        } else {
            val qty = if (entry.totalKanalArea > 0 && entry.plantsPerKanal > 0) {
                (entry.totalKanalArea * entry.plantsPerKanal).roundToInt()
            } else {
                0
            }
            pairs.add(entryFallback to qty)
        }
    }
    return groupAndSortVarietyQuantities(pairs)
}

private fun groupAndSortVarietyQuantities(pairs: List<Pair<String, Int>>): List<VarietyQuantityRow> {
    if (pairs.isEmpty()) return emptyList()
    val grouped = LinkedHashMap<String, Int>()
    for ((rawVariety, qty) in pairs) {
        val normalizedVariety = rawVariety.trim().ifEmpty { "No variety" }
        grouped[normalizedVariety] = (grouped[normalizedVariety] ?: 0) + qty
    }
    return grouped.entries
        .map { (variety, quantity) -> VarietyQuantityRow(variety = variety, quantity = quantity) }
        .sortedWith(compareByDescending<VarietyQuantityRow> { it.quantity }.thenBy { it.variety })
}
