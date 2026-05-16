package dam_a52057.wastewatch.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import dam_a52057.wastewatch.data.local.dao.CategoryDao
import dam_a52057.wastewatch.data.local.dao.InventoryItemDao
import dam_a52057.wastewatch.data.local.dao.ProductDao
import dam_a52057.wastewatch.data.local.dao.ShoppingItemDao
import dam_a52057.wastewatch.data.local.entity.CategoryEntity
import dam_a52057.wastewatch.data.local.entity.InventoryItemEntity
import dam_a52057.wastewatch.data.local.entity.ProductEntity
import dam_a52057.wastewatch.data.local.entity.ShoppingItemEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        CategoryEntity::class,
        ProductEntity::class,
        InventoryItemEntity::class,
        ShoppingItemEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun inventoryItemDao(): InventoryItemDao
    abstract fun shoppingItemDao(): ShoppingItemDao

    companion object {
        private val PREPOPULATE_CATEGORIES = listOf(
            CategoryEntity(name = "Laticínios"),
            CategoryEntity(name = "Carne"),
            CategoryEntity(name = "Vegetais"),
            CategoryEntity(name = "Frutas"),
            CategoryEntity(name = "Padaria"),
            CategoryEntity(name = "Bebidas"),
            CategoryEntity(name = "Congelados"),
            CategoryEntity(name = "Outros")
        )

        fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                "wastewatch.db"
            )
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed categories on first creation
                        CoroutineScope(Dispatchers.IO).launch {
                            // Seeded via Hilt after build — see AppModule
                        }
                    }
                })
                .build()
        }
    }
}
