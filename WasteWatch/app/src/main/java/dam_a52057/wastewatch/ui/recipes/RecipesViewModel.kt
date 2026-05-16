package dam_a52057.wastewatch.ui.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dam_a52057.wastewatch.data.remote.GeminiService
import dam_a52057.wastewatch.data.remote.RecipeAi
import dam_a52057.wastewatch.data.repository.InventoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecipesUiState(
    val recipes: List<RecipeAi> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class RecipesViewModel @Inject constructor(
    private val inventoryRepository: InventoryRepository,
    private val geminiService: GeminiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecipesUiState())
    val uiState: StateFlow<RecipesUiState> = _uiState

    fun generateRecipes() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Obter todos os itens ativos do inventário
                val items = inventoryRepository.getAllActiveItemsWithProduct().first()
                if (items.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "O teu inventário está vazio. Adiciona produtos para obteres sugestões!"
                    )
                    return@launch
                }

                // Extrair nomes dos produtos
                val ingredients = items.map { it.product.name }.distinct()
                
                // Chamar o Gemini
                val suggestions = geminiService.getRecipesForIngredients(ingredients)
                
                if (suggestions.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Não foi possível gerar receitas de momento. Verifica a tua chave de API."
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        recipes = suggestions,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Erro inesperado: ${e.message}"
                )
            }
        }
    }
}
