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
            if (item.isPurchased) {
                // Se já estava comprado, podíamos ter um método para desmarcar,
                // mas para já vamos apenas permitir marcar como comprado.
                // Vou adicionar suporte a desmarcar no repository se necessário.
                shoppingRepository.addItem(item.copy(isPurchased = false))
            } else {
                shoppingRepository.markAsPurchased(item.id)
            }
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
}
