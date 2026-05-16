package dam_a52057.wastewatch.data.repository

import dam_a52057.wastewatch.data.local.dao.ShoppingItemDao
import dam_a52057.wastewatch.data.local.entity.ShoppingItemEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShoppingRepository @Inject constructor(
    private val shoppingItemDao: ShoppingItemDao
) {
    fun getAllItems(): Flow<List<ShoppingItemEntity>> =
        shoppingItemDao.getAllItems()

    suspend fun addItem(item: ShoppingItemEntity): Long =
        shoppingItemDao.insert(item)

    suspend fun deleteItem(id: Int) =
        shoppingItemDao.deleteById(id)

    suspend fun markAsPurchased(id: Int) =
        shoppingItemDao.markAsPurchased(id)

    suspend fun clearAll() =
        shoppingItemDao.clearAll()

    suspend fun clearPurchased() =
        shoppingItemDao.clearPurchased()
}
