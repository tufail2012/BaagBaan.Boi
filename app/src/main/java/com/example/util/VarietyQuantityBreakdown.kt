package com.example.util

import com.example.data.CropRecord
import com.example.data.GardenPlanningEntry
import com.example.data.parseVarietyLines

data class VarietyQuantityRow(
    val variety: String,
    val quantity: Int
)

fun varietyQuantityRowsForRecords(records: List<CropRecord>): List<VarietyQuantityRow> {
    val pairs = ArrayList<Pair<String, Int>>(records.size)
    for (record in records) {
        val lines = parseVarietyLines(record.varietyLinesJson)
        if (lines.isNotEmpty()) {
            for (line in lines) {
                pairs.add(line.variety to line.effectiveQuantity)
            }
        } else {
            pairs.add(record.plantVariety to record.quantity)
        }
    }
    return groupAndSortVarietyQuantities(pairs)
}

fun varietyQuantityRowsForGarden(entries: List<GardenPlanningEntry>): List<VarietyQuantityRow> {
    val pairs = ArrayList<Pair<String, Int>>(entries.size)
    for (entry in entries) {
        val lines = parseVarietyLines(entry.varietyLinesJson)
        if (lines.isNotEmpty()) {
            for (line in lines) {
                pairs.add(line.variety to line.quantity)
            }
        } else if (entry.totalKanalArea > 0 && entry.plantsPerKanal > 0) {
            pairs.add(entry.plantVariety to (entry.totalKanalArea * entry.plantsPerKanal).toInt())
        } else {
            pairs.add(entry.plantVariety to 0)
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
