package dam_a52057.wastewatch.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dam_a52057.wastewatch.data.local.entity.InventoryItemEntity
import dam_a52057.wastewatch.data.repository.InventoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InventoryUiState(
    val items: List<InventoryItemEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val filterCategory: String? = null,
    val searchQuery: String = ""
)

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val inventoryRepository: InventoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryUiState())
    val uiState: StateFlow<InventoryUiState> = _uiState

    init {
        loadItems()
    }

    private fun loadItems() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            inventoryRepository.getAllItems().collectLatest { items ->
                val active = items.filter { !it.isConsumed }
                val filtered = applyFilters(active)
                _uiState.value = _uiState.value.copy(
                    items = filtered,
                    isLoading = false
                )
            }
        }
    }

    private fun applyFilters(items: List<InventoryItemEntity>): List<InventoryItemEntity> {
        var result = items
        val query = _uiState.value.searchQuery
        if (query.isNotBlank()) {
            result = result.filter { it.barcode?.contains(query, ignoreCase = true) == true }
        }
        return result.sortedBy { it.expiryDate }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        loadItems()
    }

    fun consumeItem(item: InventoryItemEntity) {
        viewModelScope.launch {
            inventoryRepository.updateItem(item.copy(isConsumed = true))
        }
    }

    fun deleteItem(item: InventoryItemEntity) {
        viewModelScope.launch {
            inventoryRepository.deleteItem(item)
        }
    }
}
