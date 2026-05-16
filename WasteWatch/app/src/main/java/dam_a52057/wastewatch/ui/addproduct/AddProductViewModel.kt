package dam_a52057.wastewatch.ui.addproduct

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dam_a52057.wastewatch.data.local.entity.CategoryEntity
import dam_a52057.wastewatch.data.local.entity.InventoryItemEntity
import dam_a52057.wastewatch.data.local.entity.ProductEntity
import dam_a52057.wastewatch.data.repository.InventoryRepository
import dam_a52057.wastewatch.data.repository.ProductRepository
import dam_a52057.wastewatch.data.local.dao.CategoryDao
import dam_a52057.wastewatch.data.remote.ProductRemoteDataSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddProductUiState(
    val name: String = "",
    val brand: String = "",
    val barcode: String = "",
    val quantity: Int = 1,
    val storageLocation: String = "Despensa",
    val expiryDateMillis: Long? = null,
    val selectedCategoryId: Int? = null,
    val categories: List<CategoryEntity> = emptyList(),
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AddProductViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val inventoryRepository: InventoryRepository,
    private val categoryDao: CategoryDao,
    private val remoteDataSource: ProductRemoteDataSource
) : ViewModel() {

    private var searchJob: Job? = null

    private val _uiState = MutableStateFlow(AddProductUiState())
    val uiState: StateFlow<AddProductUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            categoryDao.getAllCategories().collect { cats ->
                _uiState.value = _uiState.value.copy(categories = cats)
            }
        }
    }

    fun onNameChanged(value: String) { _uiState.value = _uiState.value.copy(name = value) }
    fun onBrandChanged(value: String) { _uiState.value = _uiState.value.copy(brand = value) }
    
    fun onBarcodeChanged(value: String) { 
        _uiState.value = _uiState.value.copy(barcode = value)
        
        // Se o código tiver comprimento padrão (8 ou 13), tenta procurar
        if (value.length == 8 || value.length == 13) {
            fetchProductDetails(value)
        }
    }

    private fun fetchProductDetails(barcode: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500) // Pequeno delay para evitar chamadas enquanto escreve
            val result = remoteDataSource.fetchProductByBarcode(barcode)
            result.onSuccess { productData ->
                _uiState.value = _uiState.value.copy(
                    name = productData.name ?: _uiState.value.name,
                    brand = productData.brands ?: _uiState.value.brand
                )
            }
        }
    }
    fun onQuantityChanged(value: Int) { _uiState.value = _uiState.value.copy(quantity = value) }
    fun onLocationChanged(value: String) { _uiState.value = _uiState.value.copy(storageLocation = value) }
    fun onExpiryDateChanged(millis: Long) { _uiState.value = _uiState.value.copy(expiryDateMillis = millis) }
    fun onCategoryChanged(id: Int?) { _uiState.value = _uiState.value.copy(selectedCategoryId = id) }

    fun prefillFromBarcode(barcode: String) {
        _uiState.value = _uiState.value.copy(barcode = barcode)
    }

    fun saveProduct() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.value = state.copy(error = "O nome do produto é obrigatório")
            return
        }
        if (state.expiryDateMillis == null) {
            _uiState.value = state.copy(error = "A data de validade é obrigatória")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            try {
                val productId = productRepository.addProduct(
                    ProductEntity(
                        name = state.name,
                        brand = state.brand.ifBlank { null },
                        barcode = state.barcode.ifBlank { null },
                        categoryId = state.selectedCategoryId
                    )
                )
                inventoryRepository.addItem(
                    InventoryItemEntity(
                        productId = productId.toInt(),
                        expiryDate = state.expiryDateMillis,
                        quantity = state.quantity,
                        storageLocation = state.storageLocation
                    )
                )
                _uiState.value = _uiState.value.copy(isSaving = false, isSaved = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSaving = false, error = e.message)
            }
        }
    }
}
