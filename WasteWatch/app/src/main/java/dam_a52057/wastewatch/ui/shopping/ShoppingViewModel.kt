package dam_a52057.wastewatch.ui.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dam_a52057.wastewatch.data.local.entity.ShoppingItemEntity
import dam_a52057.wastewatch.data.repository.ShoppingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShoppingUiState(
    val items: List<ShoppingItemEntity> = emptyList(),
    val newItemName: String = "",
    val isLoading: Boolean = false
)

@HiltViewModel
class ShoppingViewModel @Inject constructor(
    private val shoppingRepository: ShoppingRepository
) : ViewModel() {

    init {
        shoppingRepository.startSync()
    }

    private val _newItemName = MutableStateFlow("")
    
    val uiState: StateFlow<ShoppingUiState> = combine(
        shoppingRepository.getAllItems(),
        _newItemName
    ) { items, name ->
        ShoppingUiState(
            items = items,
            newItemName = name,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ShoppingUiState(isLoading = true)
    )

    fun onNewItemNameChange(name: String) {
        _newItemName.value = name
    }

    fun addItem() {
        val name = _newItemName.value
        if (name.isNotBlank()) {
            viewModelScope.launch {
                shoppingRepository.addItem(ShoppingItemEntity(name = name))
                _newItemName.value = ""
            }
        }
    }

    fun togglePurchased(item: ShoppingItemEntity) {
        viewModelScope.launch {
            shoppingRepository.updateItem(item.copy(isPurchased = !item.isPurchased))
        }
    }

    fun deleteItem(id: Int) {
        viewModelScope.launch {
            shoppingRepository.deleteItem(id)
        }
    }

    fun clearPurchased() {
        viewModelScope.launch {
            shoppingRepository.clearPurchased()
        }
    }

    override fun onCleared() {
        super.onCleared()
        shoppingRepository.stopSync()
    }
}
