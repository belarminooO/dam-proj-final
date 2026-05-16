package dam_a52057.wastewatch.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dam_a52057.wastewatch.data.remote.ProductData
import dam_a52057.wastewatch.data.remote.ProductRemoteDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScannerUiState(
    val isLoading: Boolean = false,
    val scannedBarcode: String? = null,
    val productData: ProductData? = null,
    val error: String? = null,
    val navigateToAddProduct: Boolean = false
)

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val remoteDataSource: ProductRemoteDataSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState

    fun onBarcodeScanned(barcode: String) {
        if (_uiState.value.isLoading || _uiState.value.scannedBarcode == barcode) return
        _uiState.value = _uiState.value.copy(isLoading = true, scannedBarcode = barcode, error = null)
        viewModelScope.launch {
            val result = remoteDataSource.fetchProductByBarcode(barcode)
            result.fold(
                onSuccess = { product ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        productData = product,
                        navigateToAddProduct = true
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message,
                        navigateToAddProduct = true // navega mesmo sem dados (modo manual)
                    )
                }
            )
        }
    }

    fun onNavigationHandled() {
        _uiState.value = _uiState.value.copy(
            navigateToAddProduct = false,
            scannedBarcode = null
        )
    }

    fun resetError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
