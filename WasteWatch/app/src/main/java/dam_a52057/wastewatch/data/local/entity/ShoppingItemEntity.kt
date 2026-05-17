package dam_a52057.wastewatch.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shopping_items")
data class ShoppingItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val quantity: Int = 1,
    val isPurchased: Boolean = false,
    
    // Cloud Sync fields
    val remoteId: String? = null,
    val householdId: String? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)
