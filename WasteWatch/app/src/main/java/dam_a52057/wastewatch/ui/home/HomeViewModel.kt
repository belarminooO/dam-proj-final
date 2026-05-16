package dam_a52057.wastewatch.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dam_a52057.wastewatch.data.local.entity.InventoryItemEntity
import dam_a52057.wastewatch.data.local.entity.InventoryItemWithProduct
import dam_a52057.wastewatch.data.repository.InventoryRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class HomeUiState(
    val expiresTodayCount: Int = 0,
    val urgentCount: Int = 0,
    val totalCount: Int = 0,
    val top5Items: List<InventoryItemWithProduct> = emptyList()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val inventoryRepository: InventoryRepository
) : ViewModel() {

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
            inventoryRepository.getTop5UrgentItemsWithProduct()
        ) { today, urgent, total, top5 ->
            HomeUiState(
                expiresTodayCount = today,
                urgentCount = urgent,
                totalCount = total,
                top5Items = top5
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HomeUiState()
        )
    }

    fun consumeItem(item: InventoryItemEntity) {
        viewModelScope.launch {
            inventoryRepository.consumeItem(item)
        }
    }

    fun deleteItem(item: InventoryItemEntity) {
        viewModelScope.launch {
            inventoryRepository.deleteItem(item)
        }
    }
}