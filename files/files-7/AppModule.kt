package dam_a52057.wastewatch.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dam_a52057.wastewatch.data.local.AppDatabase
import dam_a52057.wastewatch.data.local.dao.CategoryDao
import dam_a52057.wastewatch.data.local.dao.InventoryItemDao
import dam_a52057.wastewatch.data.local.dao.ProductDao
import dam_a52057.wastewatch.data.local.dao.ShoppingItemDao
import dam_a52057.wastewatch.data.local.entity.CategoryEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        val db = AppDatabase.buildDatabase(context)

        // Seed categories if table is empty
        CoroutineScope(Dispatchers.IO).launch {
            val dao = db.categoryDao()
            if (dao.getCount() == 0) {
                dao.insertAll(
                    listOf(
                        CategoryEntity(name = "Laticínios"),
                        CategoryEntity(name = "Carne"),
                        CategoryEntity(name = "Vegetais"),
                        CategoryEntity(name = "Frutas"),
                        CategoryEntity(name = "Padaria"),
                        CategoryEntity(name = "Bebidas"),
                        CategoryEntity(name = "Congelados"),
                        CategoryEntity(name = "Outros")
                    )
                )
            }
        }

        return db
    }

    @Provides
    fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideProductDao(db: AppDatabase): ProductDao = db.productDao()

    @Provides
    fun provideInventoryItemDao(db: AppDatabase): InventoryItemDao = db.inventoryItemDao()

    @Provides
    fun provideShoppingItemDao(db: AppDatabase): ShoppingItemDao = db.shoppingItemDao()
}
