package dam_a52057.wastewatch.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dam_a52057.wastewatch.data.local.entity.ShoppingItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ShoppingItemEntity): Long

    @Update
    suspend fun update(item: ShoppingItemEntity)

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

    @Query("SELECT * FROM shopping_items WHERE id = :id")
    suspend fun getById(id: Int): ShoppingItemEntity?

    @Query("SELECT * FROM shopping_items WHERE remoteId = :remoteId")
    suspend fun getByRemoteId(remoteId: String): ShoppingItemEntity?

    @Query("DELETE FROM shopping_items WHERE remoteId = :remoteId")
    suspend fun deleteByRemoteId(remoteId: String)

    @Query("UPDATE shopping_items SET remoteId = :remoteId, householdId = :householdId WHERE id = :id")
    suspend fun updateRemoteId(id: Int, remoteId: String, householdId: String)
}
