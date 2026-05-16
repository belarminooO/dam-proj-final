package dam_a52057.wastewatch.data.repository

import dam_a52057.wastewatch.data.local.dao.ProductDao
import dam_a52057.wastewatch.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepository @Inject constructor(
    private val productDao: ProductDao
) {
    fun getAllProducts(): Flow<List<ProductEntity>> =
        productDao.getAllProducts()

    suspend fun getProductById(id: Int): ProductEntity? =
        productDao.getProductById(id)

    suspend fun getProductByBarcode(barcode: String): ProductEntity? =
        productDao.getProductByBarcode(barcode)

    suspend fun addProduct(product: ProductEntity): Long =
        productDao.insert(product)

    suspend fun deleteProduct(id: Int) =
        productDao.deleteById(id)
}
