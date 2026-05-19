package dam_a52057.wastewatch.ui.mealplanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dam_a52057.wastewatch.data.local.entity.MealPlanEntity
import dam_a52057.wastewatch.data.model.PartyGroup
import dam_a52057.wastewatch.data.repository.AuthRepository
import dam_a52057.wastewatch.data.repository.InventoryRepository
import dam_a52057.wastewatch.data.repository.MealPlanRepository
import dam_a52057.wastewatch.data.repository.SocialRepository
import dam_a52057.wastewatch.data.remote.GeminiService
import dam_a52057.wastewatch.data.remote.RecipeAi
import dam_a52057.wastewatch.data.repository.ShoppingRepository
import dam_a52057.wastewatch.data.local.entity.ShoppingItemEntity
import dam_a52057.wastewatch.data.local.entity.InventoryItemWithProduct
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

enum class PlanType {
    PERSONAL,
    HOUSEHOLD,
    GROUP
}

data class MealPlannerUiState(
    val planType: PlanType = PlanType.PERSONAL,
    val weekStartDate: Long = 0L,
    val currentWeekLabel: String = "",
    val mealPlans: Map<String, Map<String, MealPlanEntity>> = emptyMap(),
    val householdId: String? = null,
    val partyGroups: List<PartyGroup> = emptyList(),
    val selectedGroup: PartyGroup? = null,
    val isLoading: Boolean = false,
    val isLoadingGemini: Boolean = false,
    val geminiSuggestions: List<RecipeAi> = emptyList(),
    val error: String? = null,
    val activeInventoryItems: List<InventoryItemWithProduct> = emptyList(),
    val depletedItemsToPrompt: List<InventoryItemWithProduct> = emptyList()
)

data class CoreState(
    val planType: PlanType,
    val offset: Int,
    val groupId: String?,
    val user: dam_a52057.wastewatch.data.model.User?
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MealPlannerViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val socialRepository: SocialRepository,
    private val inventoryRepository: InventoryRepository,
    private val mealPlanRepository: MealPlanRepository,
    private val shoppingRepository: ShoppingRepository,
    private val geminiService: GeminiService
) : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val _depletedItemsPrompt = MutableStateFlow<List<InventoryItemWithProduct>>(emptyList())

    private val _planType = MutableStateFlow(PlanType.PERSONAL)
    private val _currentWeekOffset = MutableStateFlow(0)
    private val _selectedGroupId = MutableStateFlow<String?>(null)
    
    private val _isLoadingGemini = MutableStateFlow(false)
    private val _geminiSuggestions = MutableStateFlow<List<RecipeAi>>(emptyList())
    private val _error = MutableStateFlow<String?>(null)

    // Detalhes do utilizador carregados de forma única
    private val userFlow = flow {
        val currentUser = authRepository.currentUser
        if (currentUser != null) {
            val res = authRepository.getUserData(currentUser.uid)
            emit(res.getOrNull())
        } else {
            emit(null)
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    // Lista de grupos de festa
    private val partyGroupsFlow = userFlow.flatMapLatest { user ->
        if (user != null) {
            flow {
                val res = socialRepository.getUserPartyGroups(user.uid)
                emit(res.getOrNull() ?: emptyList())
            }
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Fluxo de planos de refeição com base no tipo selecionado, semana e grupo
    private val mealPlansFlow = combine(
        _planType,
        _currentWeekOffset,
        _selectedGroupId,
        userFlow
    ) { type, offset, groupId, user ->
        val (weekStart, _) = getWeekStartAndLabel(offset)
        when (type) {
            PlanType.PERSONAL -> {
                mealPlanRepository.getPersonalMealPlansForWeek(weekStart)
            }
            PlanType.HOUSEHOLD -> {
                val householdId = user?.householdId
                if (householdId != null) {
                    mealPlanRepository.startHouseholdSync(householdId)
                    mealPlanRepository.getHouseholdMealPlansForWeek(householdId, weekStart)
                } else {
                    flowOf(emptyList())
                }
            }
            PlanType.GROUP -> {
                if (groupId != null) {
                    mealPlanRepository.startGroupSync(groupId)
                    mealPlanRepository.getGroupMealPlansForWeek(groupId, weekStart)
                } else {
                    flowOf(emptyList())
                }
            }
        }
    }.flatMapLatest { it }

    // Combine 1: Core Navigation & User data
    private val coreStateFlow = combine(
        _planType,
        _currentWeekOffset,
        _selectedGroupId,
        userFlow
    ) { type, offset, groupId, user ->
        CoreState(type, offset, groupId, user)
    }

    // Combine 2: Combine base flows with lists (groups, plans)
    private val baseStateFlow = combine(
        coreStateFlow,
        partyGroupsFlow,
        mealPlansFlow
    ) { core, groups, plans ->
        val (weekStart, label) = getWeekStartAndLabel(core.offset)
        val mappedPlans = plans.groupBy { it.dayOfWeek }
            .mapValues { entry -> entry.value.associateBy { it.mealType } }
        val selectedGroup = groups.find { it.id == core.groupId }
        
        MealPlannerUiState(
            planType = core.planType,
            weekStartDate = weekStart,
            currentWeekLabel = label,
            mealPlans = mappedPlans,
            householdId = core.user?.householdId,
            partyGroups = groups,
            selectedGroup = selectedGroup
        )
    }

    private val activeInventoryFlow = inventoryRepository.getAllActiveItemsWithProduct()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Combine 3: Add the Gemini loading & suggestion states, and active stock items
    val uiState: StateFlow<MealPlannerUiState> = combine(
        baseStateFlow,
        _isLoadingGemini,
        _geminiSuggestions,
        _error,
        combine(activeInventoryFlow, _depletedItemsPrompt) { active, depleted -> Pair(active, depleted) }
    ) { base, loadingGemini, suggestions, err, stockAndDepleted ->
        base.copy(
            isLoadingGemini = loadingGemini,
            geminiSuggestions = suggestions,
            error = err,
            activeInventoryItems = stockAndDepleted.first,
            depletedItemsToPrompt = stockAndDepleted.second
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        MealPlannerUiState()
    )

    fun setPlanType(type: PlanType) {
        _planType.value = type
        // Se mudar para grupo e nenhum estiver selecionado, escolhe o primeiro disponível
        if (type == PlanType.GROUP && _selectedGroupId.value == null) {
            val firstGroup = partyGroupsFlow.value.firstOrNull()
            if (firstGroup != null) {
                _selectedGroupId.value = firstGroup.id
            }
        }
    }

    fun selectGroup(groupId: String) {
        _selectedGroupId.value = groupId
    }

    fun nextWeek() {
        _currentWeekOffset.value += 1
    }

    fun previousWeek() {
        _currentWeekOffset.value -= 1
    }

    fun addManualMeal(day: String, mealType: String, recipeName: String) {
        viewModelScope.launch {
            val state = uiState.value
            val item = MealPlanEntity(
                weekStartDate = state.weekStartDate,
                dayOfWeek = day,
                mealType = mealType,
                recipeName = recipeName,
                ingredients = "[]",
                instructions = "Adicionado manualmente.",
                householdId = if (state.planType == PlanType.HOUSEHOLD) state.householdId else null,
                groupId = if (state.planType == PlanType.GROUP) state.selectedGroup?.id else null
            )
            mealPlanRepository.addMealPlan(item)
        }
    }

    fun deleteMealPlan(planId: Int) {
        viewModelScope.launch {
            mealPlanRepository.deleteMealPlan(planId)
        }
    }

    fun toggleMealPlanDone(plan: MealPlanEntity) {
        viewModelScope.launch {
            mealPlanRepository.updateMealPlan(plan.copy(isDone = !plan.isDone))
        }
    }

    fun consumeRecipeIngredients(ingredientsJson: String) {
        viewModelScope.launch {
            try {
                val gson = com.google.gson.Gson()
                val type = object : com.google.gson.reflect.TypeToken<List<String>>() {}.type
                val ingredientsList: List<String> = gson.fromJson(ingredientsJson, type) ?: return@launch
                
                val activeItems = inventoryRepository.getAllActiveItemsWithProduct().first()
                
                ingredientsList.forEach { ingredientName ->
                    val matchingItem = activeItems.find { 
                        it.product.name.equals(ingredientName, ignoreCase = true) ||
                        ingredientName.contains(it.product.name, ignoreCase = true) ||
                        it.product.name.contains(ingredientName, ignoreCase = true)
                    }
                    if (matchingItem != null) {
                        inventoryRepository.consumeItem(matchingItem.item)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun consumeSelectedInventoryItems(deductions: Map<Int, Int>) {
        viewModelScope.launch {
            try {
                val depleted = mutableListOf<InventoryItemWithProduct>()
                deductions.forEach { (itemId, qty) ->
                    val itemWithProduct = uiState.value.activeInventoryItems.find { it.item.id == itemId }
                    if (itemWithProduct != null) {
                        val item = itemWithProduct.item
                        if (item.quantity > qty) {
                            inventoryRepository.updateItem(item.copy(quantity = item.quantity - qty))
                        } else {
                            depleted.add(itemWithProduct)
                            inventoryRepository.markAsConsumed(itemId)
                        }
                    }
                }
                if (depleted.isNotEmpty()) {
                    _depletedItemsPrompt.value = depleted
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addDepletedToShopping(items: List<InventoryItemWithProduct>) {
        viewModelScope.launch {
            items.forEach { itemWithProduct ->
                shoppingRepository.addItem(ShoppingItemEntity(name = itemWithProduct.product.name))
            }
            _depletedItemsPrompt.value = emptyList()
        }
    }

    fun dismissDepletedPrompt() {
        _depletedItemsPrompt.value = emptyList()
    }

    // --- Integração Gemini ---

    fun loadGeminiSuggestions(day: String, mealType: String) {
        viewModelScope.launch {
            _isLoadingGemini.value = true
            _geminiSuggestions.value = emptyList()
            _error.value = null
            try {
                val state = uiState.value
                val ingredients = when (state.planType) {
                    PlanType.PERSONAL, PlanType.HOUSEHOLD -> {
                        // Obter ingredientes locais
                        val activeItems = inventoryRepository.getAllActiveItemsWithProduct().first()
                        activeItems.map { it.product.name }.distinct()
                    }
                    PlanType.GROUP -> {
                        // Combinar ingredientes de todos os agregados de todos os membros do grupo
                        val groupId = state.selectedGroup?.id
                        if (groupId != null) {
                            getGroupCombinedStock(groupId)
                        } else {
                            emptyList()
                        }
                    }
                }
                
                if (ingredients.isEmpty()) {
                    _error.value = "Não foram encontrados ingredientes em stock para sugerir receitas."
                    _isLoadingGemini.value = false
                    return@launch
                }

                val suggestions = geminiService.getMealSuggestions(ingredients, mealType)
                _geminiSuggestions.value = suggestions
            } catch (e: Exception) {
                _error.value = e.message ?: "Erro ao obter sugestões do Gemini."
            } finally {
                _isLoadingGemini.value = false
            }
        }
    }

    fun selectGeminiSuggestion(day: String, mealType: String, recipe: RecipeAi) {
        viewModelScope.launch {
            val state = uiState.value
            val gson = com.google.gson.Gson()
            val ingredientsJson = gson.toJson(recipe.ingredients)
            val instructionsText = recipe.instructions.joinToString("\n")
            
            val item = MealPlanEntity(
                weekStartDate = state.weekStartDate,
                dayOfWeek = day,
                mealType = mealType,
                recipeName = recipe.title,
                ingredients = ingredientsJson,
                instructions = "$instructionsText\n\nTempo: ${recipe.preparationTime} | Dificuldade: ${recipe.difficulty}",
                householdId = if (state.planType == PlanType.HOUSEHOLD) state.householdId else null,
                groupId = if (state.planType == PlanType.GROUP) state.selectedGroup?.id else null
            )
            
            mealPlanRepository.addMealPlan(item)
            _geminiSuggestions.value = emptyList() // Limpar sugestões
        }
    }

    fun clearSuggestions() {
        _geminiSuggestions.value = emptyList()
        _error.value = null
    }

    private suspend fun getGroupCombinedStock(groupId: String): List<String> {
        return try {
            val membersResult = socialRepository.getPartyGroupMembers(groupId)
            val members = membersResult.getOrNull() ?: return emptyList()
            val allIngredients = mutableSetOf<String>()
            
            for (member in members) {
                if (member.householdId != null) {
                    val snapshot = db.collection("households")
                        .document(member.householdId)
                        .collection("inventory")
                        .get()
                        .await()
                    
                    snapshot.forEach { doc ->
                        val name = doc.getString("productName")
                        if (name != null) allIngredients.add(name)
                    }
                }
            }
            allIngredients.toList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun getWeekStartAndLabel(offsetWeeks: Int): Pair<Long, String> {
        val calendar = Calendar.getInstance(Locale("pt", "PT")).apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.WEEK_OF_YEAR, offsetWeeks)
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }
        val mondayTime = calendar.timeInMillis
        val startDay = calendar.get(Calendar.DAY_OF_MONTH)
        val startMonth = calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale("pt", "PT"))
        
        calendar.add(Calendar.DAY_OF_YEAR, 6)
        val endDay = calendar.get(Calendar.DAY_OF_MONTH)
        val endMonth = calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale("pt", "PT"))
        
        val label = "Semana de $startDay $startMonth a $endDay $endMonth"
        return Pair(mondayTime, label)
    }

    override fun onCleared() {
        super.onCleared()
        mealPlanRepository.stopSync()
    }
}
