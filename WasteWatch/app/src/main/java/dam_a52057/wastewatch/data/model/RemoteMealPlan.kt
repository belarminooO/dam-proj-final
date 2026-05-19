package dam_a52057.wastewatch.data.model

import com.google.firebase.firestore.PropertyName

data class RemoteMealPlan(
    val id: String = "",
    val weekStartDate: Long = 0,
    val dayOfWeek: String = "",
    val mealType: String = "",
    val recipeName: String = "",
    val ingredients: String = "",
    val instructions: String? = null,
    @get:PropertyName("isDone") @set:PropertyName("isDone")
    var isDone: Boolean = false,
    val lastUpdated: Long = 0
)
