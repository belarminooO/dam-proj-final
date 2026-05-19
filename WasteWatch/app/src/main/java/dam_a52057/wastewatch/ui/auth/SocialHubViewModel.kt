package dam_a52057.wastewatch.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dam_a52057.wastewatch.data.model.User
import dam_a52057.wastewatch.data.model.PartyGroup
import dam_a52057.wastewatch.data.repository.AuthRepository
import dam_a52057.wastewatch.data.repository.SocialRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SocialHubUiState(
    val user: User? = null,
    val householdMembers: List<User> = emptyList(),
    val householdCreatorId: String? = null,
    val partyGroups: List<PartyGroup> = emptyList(),
    val selectedGroupMembers: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val inviteCode: String? = null,
    val createdGroupInviteCode: String? = null,
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
                    val newUser = User(uid = currentUser.uid, email = currentUser.email ?: "", name = "Utilizador")
                    viewModelScope.launch {
                        authRepository.saveUserDocument(newUser)
                        _uiState.value = _uiState.value.copy(user = newUser, isLoading = false)
                    }
                } else {
                    _uiState.value = _uiState.value.copy(user = user)
                    user.householdId?.let { householdId ->
                        loadHouseholdMembers(householdId)
                        loadHouseholdCreator(householdId)
                    } ?: run {
                        _uiState.value = _uiState.value.copy(
                            householdMembers = emptyList(),
                            householdCreatorId = null
                        )
                    }
                    loadUserPartyGroups(currentUser.uid)
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

    private fun loadHouseholdCreator(householdId: String) {
        viewModelScope.launch {
            val result = socialRepository.getHouseholdCreator(householdId)
            result.onSuccess { creator ->
                _uiState.value = _uiState.value.copy(householdCreatorId = creator)
            }
        }
    }

    private fun loadUserPartyGroups(userId: String) {
        viewModelScope.launch {
            val result = socialRepository.getUserPartyGroups(userId)
            result.onSuccess { groups ->
                _uiState.value = _uiState.value.copy(partyGroups = groups, isLoading = false)
            }.onFailure {
                _uiState.value = _uiState.value.copy(error = it.message, isLoading = false)
            }
        }
    }

    fun loadGroupMembers(groupId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(selectedGroupMembers = emptyList())
            val result = socialRepository.getPartyGroupMembers(groupId)
            result.onSuccess { members ->
                _uiState.value = _uiState.value.copy(selectedGroupMembers = members)
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

    fun deleteHousehold() {
        val user = _uiState.value.user ?: return
        val householdId = user.householdId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = socialRepository.deleteHousehold(householdId)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    householdMembers = emptyList(),
                    householdCreatorId = null
                )
                loadUserData()
            }.onFailure {
                _uiState.value = _uiState.value.copy(error = it.message, isLoading = false)
            }
        }
    }

    fun removeMemberFromHousehold(memberId: String) {
        val user = _uiState.value.user ?: return
        val householdId = user.householdId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = socialRepository.removeMemberFromHousehold(householdId, memberId)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(isLoading = false)
                loadUserData()
            }.onFailure {
                _uiState.value = _uiState.value.copy(error = it.message, isLoading = false)
            }
        }
    }

    fun leaveHousehold() {
        val user = _uiState.value.user ?: return
        val householdId = user.householdId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = socialRepository.leaveHousehold(householdId, user.uid)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    householdMembers = emptyList(),
                    householdCreatorId = null
                )
                loadUserData()
            }.onFailure {
                _uiState.value = _uiState.value.copy(error = it.message, isLoading = false)
            }
        }
    }

    fun createPartyGroup(name: String) {
        val userId = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = socialRepository.createPartyGroup(name, userId)
            result.onSuccess { code ->
                _uiState.value = _uiState.value.copy(createdGroupInviteCode = code, isLoading = false)
                loadUserData()
            }.onFailure {
                _uiState.value = _uiState.value.copy(error = it.message, isLoading = false)
            }
        }
    }

    fun joinPartyGroup(code: String) {
        val userId = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = socialRepository.joinPartyGroup(code, userId)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(isLoading = false, error = null)
                loadUserData()
            }.onFailure {
                _uiState.value = _uiState.value.copy(error = it.message, isLoading = false)
            }
        }
    }

    fun deletePartyGroup(groupId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = socialRepository.deletePartyGroup(groupId)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(isLoading = false)
                loadUserData()
            }.onFailure {
                _uiState.value = _uiState.value.copy(error = it.message, isLoading = false)
            }
        }
    }

    fun removeMemberFromPartyGroup(groupId: String, memberId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = socialRepository.removeMemberFromPartyGroup(groupId, memberId)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(isLoading = false)
                loadGroupMembers(groupId)
                loadUserData()
            }.onFailure {
                _uiState.value = _uiState.value.copy(error = it.message, isLoading = false)
            }
        }
    }

    fun leavePartyGroup(groupId: String) {
        val user = _uiState.value.user ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = socialRepository.leavePartyGroup(groupId, user.uid)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(isLoading = false)
                loadUserData()
            }.onFailure {
                _uiState.value = _uiState.value.copy(error = it.message, isLoading = false)
            }
        }
    }

    fun clearGroupInviteCode() {
        _uiState.value = _uiState.value.copy(createdGroupInviteCode = null)
    }

    fun logout() {
        authRepository.logout()
        _uiState.value = _uiState.value.copy(isLoggedOut = true)
    }
}
