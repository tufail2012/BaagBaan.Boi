package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory_items ORDER BY itemName ASC")
    fun getAllItems(): Flow<List<InventoryItem>>

    @Query("SELECT * FROM inventory_items WHERE id = :id")
    suspend fun getItemById(id: Long): InventoryItem?

    @Query("SELECT * FROM inventory_items WHERE category = :category AND variety = :variety LIMIT 1")
    suspend fun getItemByCategoryAndVariety(category: String, variety: String): InventoryItem?

    @Query("SELECT * FROM inventory_items WHERE category = :category AND (variety = :variety OR itemName = :variety OR itemName = :category) LIMIT 1")
    suspend fun findMatchingItem(category: String, variety: String): InventoryItem?

    @Query("SELECT * FROM inventory_items WHERE category = :category LIMIT 1")
    suspend fun findMatchingItemByCategory(category: String): InventoryItem?

    @Query("SELECT * FROM inventory_items WHERE itemName LIKE '%' || :query || '%' OR sku LIKE '%' || :query || '%' OR variety LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%'")
    fun searchItems(query: String): Flow<List<InventoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: InventoryItem): Long

    @Update
    suspend fun updateItem(item: InventoryItem)

    @Delete
    suspend fun deleteItem(item: InventoryItem)

    @Query("DELETE FROM inventory_items WHERE id = :id")
    suspend fun deleteItemById(id: Long)

    @Query("UPDATE inventory_items SET currentQuantity = MAX(0, currentQuantity - :quantity) WHERE id = :id")
    suspend fun decrementQuantity(id: Long, quantity: Int)

    @Query("UPDATE inventory_items SET currentQuantity = currentQuantity + :quantity WHERE id = :id")
    suspend fun incrementQuantity(id: Long, quantity: Int)

    @Query("DELETE FROM inventory_items")
    suspend fun deleteAll()
}
