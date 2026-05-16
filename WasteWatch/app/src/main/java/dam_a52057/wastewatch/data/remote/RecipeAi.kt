package dam_a52057.wastewatch.data.remote

data class RecipeAiResponse(
    val recipes: List<RecipeAi>
)

data class RecipeAi(
    val title: String,
    val description: String,
    val ingredients: List<String>,
    val instructions: List<String>,
    val preparationTime: String,
    val difficulty: String
)
