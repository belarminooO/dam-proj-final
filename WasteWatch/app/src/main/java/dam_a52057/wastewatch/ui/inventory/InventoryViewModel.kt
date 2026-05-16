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
    val isLoading: Boolean = false,
    val error: String? = null,
    val filterCategory: String? = null,
    val selectedLocation: String? = null,
    val searchQuery: String = ""
)

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val inventoryRepository: InventoryRepository,
    private val shoppingRepository: ShoppingRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedLocation = MutableStateFlow<String?>(null)

    val uiState: StateFlow<InventoryUiState> = combine(
        inventoryRepository.getAllActiveItemsWithProduct(),
        _searchQuery,
        _selectedLocation
    ) { items, query, location ->
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

        InventoryUiState(
            items = filtered,
            searchQuery = query,
            selectedLocation = location,
            isLoading = false
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

    fun consumeItem(item: InventoryItemEntity, addToShoppingList: Boolean = false, productName: String? = null) {
        viewModelScope.launch {
            if (addToShoppingList && productName != null) {
                inventoryRepository.updateItem(item.copy(isConsumed = true))
                shoppingRepository.addItem(ShoppingItemEntity(name = productName))
            } else {
                inventoryRepository.consumeItem(item)
            }
        }
    }

    fun deleteItem(item: InventoryItemEntity) {
        viewModelScope.launch {
            inventoryRepository.deleteItem(item)
        }
    }
}
