package dam_a52057.wastewatch.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dam_a52057.wastewatch.data.model.User
import dam_a52057.wastewatch.data.repository.AuthRepository
import dam_a52057.wastewatch.data.repository.SocialRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SocialHubUiState(
    val user: User? = null,
    val householdMembers: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val inviteCode: String? = null,
    val error: String? = null,
    val isLoggedOut: Boolean = false
)

@HiltViewModel
class SocialHubViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val socialRepository: SocialRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SocialHubUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadUserData()
    }

    private fun loadUserData() {
        val currentUser = authRepository.currentUser ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = authRepository.getUserData(currentUser.uid)
            result.onSuccess { user ->
                if (user == null) {
                    // Criar documento se não existir (para users antigos)
                    val newUser = User(uid = currentUser.uid, email = currentUser.email ?: "", name = "Utilizador")
                    viewModelScope.launch {
                        authRepository.saveUserDocument(newUser)
                        _uiState.value = _uiState.value.copy(user = newUser, isLoading = false)
                    }
                } else {
                    _uiState.value = _uiState.value.copy(user = user, isLoading = false)
                    user.householdId?.let { loadHouseholdMembers(it) }
                }
            }.onFailure {
                _uiState.value = _uiState.value.copy(error = it.message, isLoading = false)
            }
        }
    }

    private fun loadHouseholdMembers(householdId: String) {
        viewModelScope.launch {
            val result = socialRepository.getHouseholdMembers(householdId)
            result.onSuccess { members ->
                _uiState.value = _uiState.value.copy(householdMembers = members)
            }
        }
    }

    fun createHousehold(name: String) {
        val userId = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = socialRepository.createHousehold(name, userId)
            result.onSuccess { code ->
                _uiState.value = _uiState.value.copy(inviteCode = code, isLoading = false)
                loadUserData()
            }.onFailure {
                _uiState.value = _uiState.value.copy(error = it.message, isLoading = false)
            }
        }
    }

    fun joinHousehold(code: String) {
        val userId = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = socialRepository.joinHousehold(code, userId)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(isLoading = false, error = null)
                loadUserData()
            }.onFailure {
                _uiState.value = _uiState.value.copy(error = it.message, isLoading = false)
            }
        }
    }

    fun logout() {
        authRepository.logout()
        _uiState.value = _uiState.value.copy(isLoggedOut = true)
    }
}
