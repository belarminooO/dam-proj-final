package dam_a52057.wastewatch.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dam_a52057.wastewatch.data.local.entity.InventoryItemEntity
import dam_a52057.wastewatch.data.local.entity.InventoryItemWithProduct
import dam_a52057.wastewatch.data.local.entity.ShoppingItemEntity
import dam_a52057.wastewatch.data.repository.InventoryRepository
import dam_a52057.wastewatch.data.repository.ShoppingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InventoryUiState(
    val items: List<InventoryItemWithProduct> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val filterCategory: String? = null,
    val selectedLocation: String? = null,
    val searchQuery: String = "",
    // Diálogo de reposição
    val showShoppingDialog: Boolean = false,
    val itemToAddToShopping: InventoryItemWithProduct? = null
)

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val inventoryRepository: InventoryRepository,
    private val shoppingRepository: ShoppingRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedLocation = MutableStateFlow<String?>(null)
    private val _shoppingDialogState = MutableStateFlow<InventoryItemWithProduct?>(null)

    val uiState: StateFlow<InventoryUiState> = combine(
        inventoryRepository.getAllActiveItemsWithProduct(),
        _searchQuery,
        _selectedLocation,
        _shoppingDialogState
    ) { items, query, location, dialogItem ->
        val filtered = items.filter { itemWithProduct ->
            val matchesQuery = if (query.isBlank()) true else {
                itemWithProduct.product.name.contains(query, ignoreCase = true) ||
                        itemWithProduct.product.barcode?.contains(query, ignoreCase = true) == true
            }
            val matchesLocation = if (location == null) true else {
                itemWithProduct.item.storageLocation == location
            }
            matchesQuery && matchesLocation
        }.sortedBy { it.item.expiryDate }

        val suggestions = if (query.isNotEmpty()) {
            items.map { it.product.name }
                .filter { it.contains(query, ignoreCase = true) && !it.equals(query, ignoreCase = true) }
                .distinct()
                .take(5)
        } else {
            emptyList()
        }

        InventoryUiState(
            items = filtered,
            suggestions = suggestions,
            searchQuery = query,
            selectedLocation = location,
            isLoading = false,
            showShoppingDialog = dialogItem != null,
            itemToAddToShopping = dialogItem
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = InventoryUiState(isLoading = true)
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onLocationFilterChanged(location: String?) {
        _selectedLocation.value = location
    }

    fun consumeItem(itemWithProduct: InventoryItemWithProduct) {
        viewModelScope.launch {
            if (itemWithProduct.item.quantity <= 1) {
                // É o último item, mostrar o diálogo ANTES de consumir de facto na UI
                _shoppingDialogState.value = itemWithProduct
            } else {
                inventoryRepository.consumeItem(itemWithProduct.item)
            }
        }
    }

    fun confirmConsumption(addToShoppingList: Boolean) {
        val itemWithProduct = _shoppingDialogState.value ?: return
        viewModelScope.launch {
            if (addToShoppingList) {
                shoppingRepository.addItem(ShoppingItemEntity(name = itemWithProduct.product.name))
            }
            inventoryRepository.markAsConsumed(itemWithProduct.item.id)
            _shoppingDialogState.value = null
        }
    }

    fun dismissShoppingDialog() {
        _shoppingDialogState.value = null
    }

    fun deleteItem(item: InventoryItemEntity) {
        viewModelScope.launch {
            inventoryRepository.deleteItem(item)
        }
    }
}
