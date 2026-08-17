package com.example.data

import java.util.UUID

data class SeasonalTask(
    val id: String = UUID.randomUUID().toString(),
    val category: String = "",
    val title: String = "",
    val notes: String = "",
    val reminderMonth: Int? = null, // 1 = January, 12 = December
    val reminderDay: Int? = null,   // 1..31
    val recurring: Boolean = true,
    val isEnabled: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "category" to category,
            "title" to title,
            "notes" to notes,
            "reminderMonth" to reminderMonth,
            "reminderDay" to reminderDay,
            "recurring" to recurring,
            "isEnabled" to isEnabled,
            "updatedAt" to updatedAt
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): SeasonalTask {
            return SeasonalTask(
                id = (map["id"] as? String) ?: UUID.randomUUID().toString(),
                category = (map["category"] as? String) ?: "",
                title = (map["title"] as? String) ?: "",
                notes = (map["notes"] as? String) ?: "",
                reminderMonth = (map["reminderMonth"] as? Number)?.toInt(),
                reminderDay = (map["reminderDay"] as? Number)?.toInt(),
                recurring = (map["recurring"] as? Boolean) ?: true,
                isEnabled = (map["isEnabled"] as? Boolean) ?: true,
                updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }

        val DEFAULT_SEEDS = listOf(
            SeasonalTask(
                id = "seed_pruning",
                category = "Dormant Pruning",
                title = "Dormant Pruning",
                notes = "Prune deadwood, structure canopy, and optimize sunlight penetration before spring bud break."
            ),
            SeasonalTask(
                id = "seed_grafting",
                category = "Grafting Window",
                title = "Grafting Window",
                notes = "Top working and scion grafting on rootstocks during active sap flow."
            ),
            SeasonalTask(
                id = "seed_spray",
                category = "Spray Schedule",
                title = "Spray Schedule",
                notes = "Horticultural mineral oil (HMO) / Fungicide application for scale, mites, and scab control."
            ),
            SeasonalTask(
                id = "seed_fertilizing",
                category = "Fertilizing",
                title = "Fertilizing",
                notes = "Basal dose application: NPK, compost, and micronutrient soil conditioning."
            ),
            SeasonalTask(
                id = "seed_harvest",
                category = "Harvest",
                title = "Harvest",
                notes = "Peak maturity picking, grading, packing, and cold-chain dispatch window."
            )
        )
    }
}
