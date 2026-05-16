package dam_a52057.wastewatch.ui.home

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
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class HomeUiState(
    val expiresTodayCount: Int = 0,
    val urgentCount: Int = 0,
    val totalCount: Int = 0,
    val top5UrgentItems: List<InventoryItemEntity> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val inventoryRepository: InventoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    private fun loadHomeData() {
        viewModelScope.launch {
            try {
                val todayEnd = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1) - 1
                val urgentThreshold = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(3)

                combine(
                    inventoryRepository.getAllActiveItems(),
                    inventoryRepository.getTotalActiveCount()
                ) { allItems, total ->
                    val expiresToday = allItems.count { it.expiryDate <= todayEnd }
                    val urgent = allItems.count { it.expiryDate <= urgentThreshold }
                    val top5 = allItems
                        .sortedBy { it.expiryDate }
                        .take(5)

                    HomeUiState(
                        expiresTodayCount = expiresToday,
                        urgentCount = urgent,
                        totalCount = total,
                        top5UrgentItems = top5,
                        isLoading = false
                    )
                }.collect { state ->
                    _uiState.value = state
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
}
