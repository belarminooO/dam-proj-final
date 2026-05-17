package dam_a52057.wastewatch.data.model

import com.google.firebase.firestore.PropertyName

data class RemoteShoppingItem(
    val id: String = "",
    val name: String = "",
    val quantity: Int = 1,
    @get:PropertyName("isPurchased") @set:PropertyName("isPurchased")
    var isPurchased: Boolean = false,
    val lastUpdated: Long = 0
)
