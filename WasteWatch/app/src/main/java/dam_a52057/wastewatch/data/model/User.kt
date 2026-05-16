package dam_a52057.wastewatch.data.model

data class User(
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val householdId: String? = null,
    val groupIds: List<String> = emptyList()
)
