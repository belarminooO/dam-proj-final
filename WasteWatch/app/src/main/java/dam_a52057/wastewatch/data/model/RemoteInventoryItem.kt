package dam_a52057.wastewatch.data.model

import com.google.firebase.firestore.PropertyName

data class RemoteInventoryItem(
    val id: String = "",
    val productId: Int = 0,
    val productName: String = "",
    val productBrand: String = "",
    val productImageUrl: String = "",
    val expiryDate: Long = 0,
    val quantity: Int = 1,
    val storageLocation: String = "Despensa",
    @get:PropertyName("isConsumed") @set:PropertyName("isConsumed")
    var isConsumed: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)
