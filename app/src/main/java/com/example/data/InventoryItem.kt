package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inventory_items")
data class InventoryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemName: String = "",
    val category: String = "", // Local Plants, Imported Plants, Imported Rootstock
    val variety: String = "", // M9T337, MM111, Geneva G-41, Geneva G-11, Geneva G-214, Geneva G-969, Geneva G-35, Geneva G-979, Geneva G-890
    val sku: String = "",
    val initialQuantity: Int = 0,
    val currentQuantity: Int = 0,
    val unitPrice: Double = 0.0,
    val supplierName: String = "",
    val supplierContact: String = "",
    val lowStockThreshold: Int = 5,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getItemsSold(): Int {
        return (initialQuantity - currentQuantity).coerceAtLeast(0)
    }

    fun isLowStock(): Boolean {
        return currentQuantity <= lowStockThreshold
    }

    fun isOutOfStock(): Boolean {
        return currentQuantity <= 0
    }
}
