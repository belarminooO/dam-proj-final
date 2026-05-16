package dam_a52057.wastewatch.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class InventoryItemWithProduct(
    @Embedded val item: InventoryItemEntity,
    @Relation(
        parentColumn = "productId",
        entityColumn = "id"
    )
    val product: ProductEntity
)
