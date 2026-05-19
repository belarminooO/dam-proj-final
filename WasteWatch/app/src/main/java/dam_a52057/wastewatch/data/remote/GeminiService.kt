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
            Podes também incluir ingredientes básicos de despensa indispensáveis (ex: azeite, sal, água, alho, pimenta, açúcar, farinha) mesmo que não estejam listados.
            
            IMPORTANTE para a lista de "ingredients":
            Cada ingrediente da lista deve obrigatoriamente especificar a quantidade sugerida em parênteses e terminar com "[Stock]" se for dos ingredientes fornecidos acima, ou com "[Básico]" se for um ingrediente essencial de despensa extra que adicionaste.
            Exemplo de formato exigido para a lista de "ingredients":
            [
              "Peito de Frango (2 unidades) [Stock]",
              "Arroz Agulha (1 chávena) [Stock]",
              "Azeite (2 colheres de sopa) [Básico]",
              "Sal (1 pitada) [Básico]"
            ]
            
            O teu output deve ser APENAS um objeto JSON válido com a seguinte estrutura:
            {
              "recipes": [
                {
                  "title": "Nome da Receita",
                  "description": "Uma breve descrição",
                  "ingredients": ["ingrediente formatado 1", "ingrediente formatado 2"],
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

    suspend fun getMealSuggestions(ingredients: List<String>, mealType: String): List<RecipeAi> {
        val mealTypeDesc = when(mealType.uppercase()) {
            "MATABIXO" -> "Matabixo (Pequeno-almoço / refeição leve matinal)"
            "ALMOCO" -> "Almoço (Refeição principal e substancial do meio do dia)"
            "JANTAR" -> "Jantar (Refeição da noite, prato principal mas preferencialmente mais leve)"
            else -> mealType
        }
        val prompt = """
            Sou um assistente de cozinha inteligente doméstico e quero evitar o desperdício alimentar.
            Tenho os seguintes ingredientes disponíveis em stock: ${ingredients.joinToString(", ")}.
            Sugere exatamente 3 opções de receitas deliciosas adequadas especificamente para a refeição **$mealTypeDesc**.
            Prioriza o uso de todos ou da maioria dos ingredientes listados para reduzir o desperdício alimentar.
            Podes também incluir ingredientes básicos de despensa indispensáveis (ex: azeite, sal, água, alho, pimenta, açúcar, farinha) mesmo que não estejam listados no stock.
            
            IMPORTANTE para a lista de "ingredients":
            Cada ingrediente da lista deve obrigatoriamente especificar a quantidade sugerida em parênteses e terminar com "[Stock]" se for dos ingredientes fornecidos acima, ou com "[Básico]" se for um ingrediente essencial de despensa extra que adicionaste.
            Exemplo de formato exigido para a lista de "ingredients":
            [
              "Peito de Frango (2 unidades) [Stock]",
              "Arroz Agulha (1 chávena) [Stock]",
              "Azeite (2 colheres de sopa) [Básico]",
              "Sal (1 pitada) [Básico]"
            ]
            
            O teu output deve ser APENAS um objeto JSON válido com a seguinte estrutura:
            {
              "recipes": [
                {
                  "title": "Nome da Receita",
                  "description": "Uma breve descrição",
                  "ingredients": ["ingrediente formatado 1", "ingrediente formatado 2"],
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
