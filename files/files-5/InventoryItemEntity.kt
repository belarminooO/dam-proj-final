package dam_a52057.wastewatch.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "inventory_items",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["productId"]),
        Index(value = ["expiryDate"]),
        Index(value = ["isConsumed"])
    ]
)
data class InventoryItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val productId: Int,
    val expiryDate: Long,
    val quantity: Int = 1,
    val storageLocation: String = "Despensa",
    val addedDate: Long = System.currentTimeMillis(),
    val isConsumed: Boolean = false
)
