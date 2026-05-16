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
import java.util.Calendar
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
            inventoryRepository.getAllActiveItems().collectLatest { items ->
                val cal = Calendar.getInstance()

                // início de hoje (00:00:00)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val todayStart = cal.timeInMillis

                // início de amanhã
                cal.add(Calendar.DAY_OF_YEAR, 1)
                val tomorrowStart = cal.timeInMillis

                // início daqui a 3 dias
                cal.add(Calendar.DAY_OF_YEAR, 2) // já avançou 1, falta +2 = 3 total
                val in3DaysStart = cal.timeInMillis

                val active = items.filter { !it.isConsumed }

                _uiState.value = HomeUiState(
                    expiresTodayCount = active.count { it.expiryDate in todayStart until tomorrowStart },
                    urgentCount = active.count { it.expiryDate < in3DaysStart },
                    totalCount = active.size,
                    top5Items = active.sortedBy { it.expiryDate }.take(5)
                )
            }
        }
    }
}