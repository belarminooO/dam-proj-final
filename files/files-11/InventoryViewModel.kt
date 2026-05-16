package dam_a52057.wastewatch.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dam_a52057.wastewatch.data.local.entity.InventoryItemEntity
import dam_a52057.wastewatch.data.repository.InventoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InventoryUiState(
    val items: List<InventoryItemEntity> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val searchQuery: String = "",
    val selectedLocation: String? = null
)

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val inventoryRepository: InventoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryUiState())
    val uiState: StateFlow<InventoryUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _selectedLocation = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            combine(
                inventoryRepository.getAllActiveItems(),
                _searchQuery,
                _selectedLocation
            ) { items, query, location ->
                val filtered = items
                    .filter { item ->
                        query.isBlank() || item.productId.toString().contains(query, ignoreCase = true)
                    }
                    .filter { item ->
                        location == null || item.storageLocation == location
                    }
                InventoryUiState(
                    items = filtered,
                    isLoading = false,
                    searchQuery = query,
                    selectedLocation = location
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onLocationFilterChanged(location: String?) {
        _selectedLocation.value = location
    }

    fun consumeItem(id: Int) {
        viewModelScope.launch {
            inventoryRepository.markAsConsumed(id)
        }
    }

    fun deleteItem(id: Int) {
        viewModelScope.launch {
            inventoryRepository.deleteItem(id)
        }
    }
}
