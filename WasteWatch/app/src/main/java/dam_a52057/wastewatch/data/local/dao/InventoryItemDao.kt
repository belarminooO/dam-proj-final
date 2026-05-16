package dam_a52057.wastewatch.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dam_a52057.wastewatch.data.local.entity.InventoryItemEntity
import dam_a52057.wastewatch.data.local.entity.InventoryItemWithProduct
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: InventoryItemEntity): Long

    @Update
    suspend fun update(item: InventoryItemEntity)

    @Query("DELETE FROM inventory_items WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("SELECT * FROM inventory_items WHERE isConsumed = 0 ORDER BY expiryDate ASC")
    fun getAllActiveItems(): Flow<List<InventoryItemEntity>>

    @androidx.room.Transaction
    @Query("SELECT * FROM inventory_items WHERE isConsumed = 0 ORDER BY expiryDate ASC")
    fun getAllActiveItemsWithProduct(): Flow<List<InventoryItemWithProduct>>

    @Query("""
        SELECT * FROM inventory_items
        WHERE isConsumed = 0
          AND expiryDate <= :thresholdTimestamp
        ORDER BY expiryDate ASC
    """)
    fun getItemsExpiringBefore(thresholdTimestamp: Long): Flow<List<InventoryItemEntity>>

    @Query("SELECT * FROM inventory_items WHERE isConsumed = 0 AND storageLocation = :location ORDER BY expiryDate ASC")
    fun getItemsByLocation(location: String): Flow<List<InventoryItemEntity>>

    @Query("SELECT COUNT(*) FROM inventory_items WHERE isConsumed = 0")
    fun getTotalActiveCount(): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM inventory_items 
        WHERE isConsumed = 0 
          AND expiryDate >= :startRange 
          AND expiryDate < :endRange
    """)
    fun getCountInDateRange(startRange: Long, endRange: Long): Flow<Int>

    @androidx.room.Transaction
    @Query("SELECT * FROM inventory_items WHERE isConsumed = 0 ORDER BY expiryDate ASC LIMIT 5")
    fun getTop5UrgentItemsWithProduct(): Flow<List<InventoryItemWithProduct>>

    @Query("SELECT * FROM inventory_items WHERE id = :id")
    suspend fun getById(id: Int): InventoryItemEntity?

    @Query("UPDATE inventory_items SET isConsumed = 1 WHERE id = :id")
    suspend fun markAsConsumed(id: Int)
}
