package dam_a52057.wastewatch.data.model

data class PartyGroup(
    val id: String = "",
    val name: String = "",
    val inviteCode: String = "",
    val members: List<String> = emptyList(),
    val createdBy: String = ""
)
