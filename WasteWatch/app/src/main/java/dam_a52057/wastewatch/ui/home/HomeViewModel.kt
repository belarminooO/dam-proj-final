package dam_a52057.wastewatch.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dam_a52057.wastewatch.data.local.entity.InventoryItemEntity
import dam_a52057.wastewatch.data.local.entity.InventoryItemWithProduct
import dam_a52057.wastewatch.data.local.entity.ShoppingItemEntity
import dam_a52057.wastewatch.data.repository.InventoryRepository
import dam_a52057.wastewatch.data.repository.ShoppingRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class HomeUiState(
    val expiresTodayCount: Int = 0,
    val urgentCount: Int = 0,
    val totalCount: Int = 0,
    val top5Items: List<InventoryItemWithProduct> = emptyList(),
    // Diálogo de reposição
    val showShoppingDialog: Boolean = false,
    val itemToAddToShopping: InventoryItemWithProduct? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val inventoryRepository: InventoryRepository,
    private val shoppingRepository: ShoppingRepository
) : ViewModel() {

    init {
        inventoryRepository.startSync()
    }

    private val _shoppingDialogState = MutableStateFlow<InventoryItemWithProduct?>(null)

    val uiState: StateFlow<HomeUiState> = run {
        val now = LocalDate.now()
        val zoneId = ZoneId.systemDefault()
        
        val todayStart = now.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val tomorrowStart = now.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val in3DaysStart = now.plusDays(3).atStartOfDay(zoneId).toInstant().toEpochMilli()

        combine(
            inventoryRepository.getCountInDateRange(todayStart, tomorrowStart),
            inventoryRepository.getUrgentCount(in3DaysStart),
            inventoryRepository.getTotalActiveCount(),
            inventoryRepository.getTop5UrgentItemsWithProduct(),
            _shoppingDialogState
        ) { today, urgent, total, top5, dialogItem ->
            HomeUiState(
                expiresTodayCount = today,
                urgentCount = urgent,
                totalCount = total,
                top5Items = top5,
                showShoppingDialog = dialogItem != null,
                itemToAddToShopping = dialogItem
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HomeUiState()
        )
    }

    fun consumeItem(itemWithProduct: InventoryItemWithProduct) {
        viewModelScope.launch {
            if (itemWithProduct.item.quantity <= 1) {
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

    override fun onCleared() {
        super.onCleared()
        inventoryRepository.stopSync()
    }
}