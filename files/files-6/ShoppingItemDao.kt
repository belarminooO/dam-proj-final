package dam_a52057.wastewatch.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dam_a52057.wastewatch.data.local.entity.ShoppingItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ShoppingItemEntity): Long

    @Query("DELETE FROM shopping_items WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM shopping_items")
    suspend fun clearAll()

    @Query("SELECT * FROM shopping_items ORDER BY isPurchased ASC, id DESC")
    fun getAllItems(): Flow<List<ShoppingItemEntity>>

    @Query("UPDATE shopping_items SET isPurchased = 1 WHERE id = :id")
    suspend fun markAsPurchased(id: Int)

    @Query("DELETE FROM shopping_items WHERE isPurchased = 1")
    suspend fun clearPurchased()
}
