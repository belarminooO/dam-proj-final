package dam_a52057.wastewatch.data.remote

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton
import dam_a52057.wastewatch.BuildConfig

@Singleton
class GeminiService @Inject constructor() {

    // API Key is now securely stored in local.properties and accessed via BuildConfig
    private val apiKey = BuildConfig.GEMINI_API_KEY

    private val model = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = apiKey,
        generationConfig = generationConfig {
            responseMimeType = "application/json"
        }
    )

    private val gson = Gson()

    suspend fun getRecipesForIngredients(ingredients: List<String>): List<RecipeAi> {
        val prompt = """
            Sou um assistente de cozinha que ajuda a evitar o desperdício alimentar.
            Tenho os seguintes ingredientes disponíveis: ${ingredients.joinToString(", ")}.
            Sugere 3 receitas deliciosas que utilizem estes ingredientes. 
            Prioriza o uso de todos ou da maioria dos ingredientes listados.
            
            O teu output deve ser APENAS um objeto JSON válido com a seguinte estrutura:
            {
              "recipes": [
                {
                  "title": "Nome da Receita",
                  "description": "Uma breve descrição",
                  "ingredients": ["ingrediente 1", "ingrediente 2"],
                  "instructions": ["passo 1", "passo 2"],
                  "preparationTime": "X min",
                  "difficulty": "Fácil/Média/Difícil"
                }
              ]
            }
        """.trimIndent()

        return try {
            val response = model.generateContent(
                content {
                    text(prompt)
                }
            )
            val json = response.text ?: return emptyList()
            val recipeResponse = gson.fromJson(json, RecipeAiResponse::class.java)
            recipeResponse.recipes
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
