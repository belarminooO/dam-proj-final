package dam_a52057.wastewatch.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dam_a52057.wastewatch.data.local.entity.InventoryItemEntity
import dam_a52057.wastewatch.data.repository.InventoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class HomeUiState(
    val expiresTodayCount: Int = 0,
    val urgentCount: Int = 0,
    val totalCount: Int = 0,
    val top5Items: List<InventoryItemEntity> = emptyList()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val inventoryRepository: InventoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        viewModelScope.launch {
            inventoryRepository.getAllItems().collectLatest { items ->
                val today = LocalDate.now()
                val todayMillis = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val tomorrowMillis = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val in3DaysMillis = today.plusDays(3).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

                val active = items.filter { !it.isConsumed }
                val expiresToday = active.count { it.expiryDate in todayMillis until tomorrowMillis }
                val urgent = active.count { it.expiryDate <= in3DaysMillis }
                val top5 = active.sortedBy { it.expiryDate }.take(5)

                _uiState.value = HomeUiState(
                    expiresTodayCount = expiresToday,
                    urgentCount = urgent,
                    totalCount = active.size,
                    top5Items = top5
                )
            }
        }
    }
}
