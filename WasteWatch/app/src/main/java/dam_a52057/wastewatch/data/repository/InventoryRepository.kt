package dam_a52057.wastewatch.data.repository

import dam_a52057.wastewatch.data.local.dao.InventoryItemDao
import dam_a52057.wastewatch.data.local.dao.ProductDao
import dam_a52057.wastewatch.data.local.entity.InventoryItemEntity
import dam_a52057.wastewatch.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InventoryRepository @Inject constructor(
    private val inventoryItemDao: InventoryItemDao,
    private val productDao: ProductDao
) {
    fun getAllActiveItems(): Flow<List<InventoryItemEntity>> =
        inventoryItemDao.getAllActiveItems()

    fun getItemsExpiringWithinDays(days: Int): Flow<List<InventoryItemEntity>> {
        val threshold = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(days.toLong())
        return inventoryItemDao.getItemsExpiringBefore(threshold)
    }

    fun getItemsByLocation(location: String): Flow<List<InventoryItemEntity>> =
        inventoryItemDao.getItemsByLocation(location)

    fun getTotalActiveCount(): Flow<Int> =
        inventoryItemDao.getTotalActiveCount()

    fun getUrgentCount(days: Int = 3): Flow<Int> {
        val threshold = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(days.toLong())
        return inventoryItemDao.getUrgentCount(threshold)
    }

    suspend fun addItem(item: InventoryItemEntity): Long =
        inventoryItemDao.insert(item)

    suspend fun updateItem(item: InventoryItemEntity) =
        inventoryItemDao.update(item)

    suspend fun deleteItem(id: Int) =
        inventoryItemDao.deleteById(id)

    suspend fun markAsConsumed(id: Int) =
        inventoryItemDao.markAsConsumed(id)

    suspend fun getItemById(id: Int): InventoryItemEntity? =
        inventoryItemDao.getById(id)
}
