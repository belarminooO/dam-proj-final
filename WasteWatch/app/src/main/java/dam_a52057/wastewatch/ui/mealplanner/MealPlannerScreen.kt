package dam_a52057.wastewatch.ui.mealplanner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import dam_a52057.wastewatch.data.local.entity.MealPlanEntity
import dam_a52057.wastewatch.data.remote.RecipeAi
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealPlannerScreen(
    viewModel: MealPlannerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Controlos de diálogos locais
    var showManualAddDialog by remember { mutableStateOf(false) }
    var selectedDayForAdd by remember { mutableStateOf("") }
    var selectedMealTypeForAdd by remember { mutableStateOf("") }
    var manualMealName by remember { mutableStateOf("") }

    var selectedMealForDetails by remember { mutableStateOf<MealPlanEntity?>(null) }

    var showConsumeConfirmDialog by remember { mutableStateOf(false) }
    var selectedPlanToToggle by remember { mutableStateOf<MealPlanEntity?>(null) }

    if (uiState.depletedItemsToPrompt.isNotEmpty()) {
        val selectedForShopping = remember(uiState.depletedItemsToPrompt) {
            mutableStateMapOf<Int, Boolean>().apply {
                uiState.depletedItemsToPrompt.forEach { put(it.item.id, true) }
            }
        }

        AlertDialog(
            onDismissRequest = { viewModel.dismissDepletedPrompt() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Repor Stock 🛒", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Os seguintes produtos esgotaram no vosso stock. Selecione quais deseja adicionar à lista de compras:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp)
                    ) {
                        items(uiState.depletedItemsToPrompt) { itemWithProduct ->
                            val isChecked = selectedForShopping[itemWithProduct.item.id] ?: false
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedForShopping[itemWithProduct.item.id] = !isChecked }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { selectedForShopping[itemWithProduct.item.id] = it == true }
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = itemWithProduct.product.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val itemsToAdd = uiState.depletedItemsToPrompt.filter { selectedForShopping[it.item.id] ?: false }
                        viewModel.addDepletedToShopping(itemsToAdd)
                    }
                ) {
                    Text("Adicionar Selecionados")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissDepletedPrompt() }
                ) {
                    Text("Não, obrigado", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }

    val daysOfWeek = listOf(
        "SEGUNDA" to "Segunda-feira",
        "TERCA" to "Terça-feira",
        "QUARTA" to "Quarta-feira",
        "QUINTA" to "Quinta-feira",
        "SEXTA" to "Sexta-feira",
        "SABADO" to "Sábado",
        "DOMINGO" to "Domingo"
    )

    val mealTypes = listOf(
        "MATABIXO" to "🍳 Matabixo",
        "ALMOCO" to "🍗 Almoço",
        "JANTAR" to "🥗 Jantar"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Plano Alimentar",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 1. Seletor de Tipo de Plano (Casa, Grupo)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Minha Casa
                FilterChip(
                    selected = uiState.planType == PlanType.HOUSEHOLD,
                    onClick = { viewModel.setPlanType(PlanType.HOUSEHOLD) },
                    label = { Text("Minha Casa") },
                    leadingIcon = { Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )

                // Grupos de Festa (se tiver grupos)
                if (uiState.partyGroups.isNotEmpty()) {
                    FilterChip(
                        selected = uiState.planType == PlanType.GROUP,
                        onClick = { viewModel.setPlanType(PlanType.GROUP) },
                        label = { Text("Eventos") },
                        leadingIcon = { Icon(Icons.Default.Celebration, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }

            // Se for do tipo GRUPO, mostramos o seletor com dropdown de qual grupo está ativo
            if (uiState.planType == PlanType.GROUP && uiState.partyGroups.isNotEmpty()) {
                var showGroupDropdown by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    OutlinedButton(
                        onClick = { showGroupDropdown = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = uiState.selectedGroup?.name ?: "Selecionar Evento/Grupo",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }

                    DropdownMenu(
                        expanded = showGroupDropdown,
                        onDismissRequest = { showGroupDropdown = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        uiState.partyGroups.forEach { group ->
                            DropdownMenuItem(
                                text = { Text(group.name) },
                                onClick = {
                                    viewModel.selectGroup(group.id)
                                    showGroupDropdown = false
                                },
                                leadingIcon = { Icon(Icons.Default.Celebration, contentDescription = null) }
                            )
                        }
                    }
                }
            }

            if (uiState.householdId == null && uiState.planType == PlanType.HOUSEHOLD) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Nenhuma Casa Detetada",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Cria ou adere a uma Casa no ecrã Social/Perfil para começares a planear as tuas refeições partilhadas.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                // 2. Navegador de Semanas
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.previousWeek() }) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Semana Anterior")
                        }

                        Text(
                            text = uiState.currentWeekLabel,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        IconButton(onClick = { viewModel.nextWeek() }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Semana Seguinte")
                        }
                    }
                }

                // 3. Grelha de Dias da Semana
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(daysOfWeek) { (dayKey, dayLabel) ->
                        DayCard(
                            dayLabel = dayLabel,
                            dayKey = dayKey,
                            mealTypes = mealTypes,
                            mealPlans = uiState.mealPlans[dayKey] ?: emptyMap(),
                            onAddManual = { mealType ->
                                selectedDayForAdd = dayKey
                                selectedMealTypeForAdd = mealType
                                manualMealName = ""
                                showManualAddDialog = true
                            },
                            onAddGemini = { mealType ->
                                viewModel.loadGeminiSuggestions(dayKey, mealType)
                                selectedDayForAdd = dayKey
                                selectedMealTypeForAdd = mealType
                            },
                            onToggleDone = { plan -> 
                                if (!plan.isDone) {
                                    selectedPlanToToggle = plan
                                    showConsumeConfirmDialog = true
                                } else {
                                    viewModel.toggleMealPlanDone(plan)
                                }
                            },
                            onViewDetails = { plan -> selectedMealForDetails = plan }
                        )
                    }
                }
            }
        }

        // --- DIÁLOGOS DE INTERAÇÃO ---

        // Diálogo para Confirmação de Consumo de Ingredientes
        if (showConsumeConfirmDialog && selectedPlanToToggle != null) {
            val plan = selectedPlanToToggle!!
            
            val gson = Gson()
            val type = object : TypeToken<List<String>>() {}.type
            val recipeIngredients: List<String> = try {
                gson.fromJson(plan.ingredients, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }

            val selectedItemQuantities = remember { mutableStateMapOf<Int, Int>() }
            var searchQuery by remember { mutableStateOf("") }

            LaunchedEffect(plan) {
                selectedItemQuantities.clear()
                if (recipeIngredients.isNotEmpty()) {
                    uiState.activeInventoryItems.forEach { stockItem ->
                        val isMatch = recipeIngredients.any { ing ->
                            // Ignorar ingredientes básicos de despensa na correspondência automática de débito de stock
                            if (ing.contains("[Básico]", ignoreCase = true)) return@any false
                            
                            stockItem.product.name.equals(ing, ignoreCase = true) ||
                            ing.contains(stockItem.product.name, ignoreCase = true) ||
                            stockItem.product.name.contains(ing, ignoreCase = true)
                        }
                        if (isMatch) {
                            selectedItemQuantities[stockItem.item.id] = 1
                        }
                    }
                }
            }

            val filteredInventory = uiState.activeInventoryItems.filter {
                it.product.name.contains(searchQuery, ignoreCase = true) ||
                (it.product.brand?.contains(searchQuery, ignoreCase = true) == true)
            }

            AlertDialog(
                onDismissRequest = { 
                    showConsumeConfirmDialog = false
                    selectedPlanToToggle = null
                },
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.RestaurantMenu, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Dar baixa no Stock? 🍳", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Selecione quais os ingredientes do vosso stock que consumiu para confecionar \"${plan.recipeName}\":",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Pesquisar na despensa...", fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                        )

                        if (filteredInventory.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Nenhum ingrediente ativo no stock.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        } else {
                            Box(modifier = Modifier.heightIn(max = 240.dp)) {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(filteredInventory) { stockItem ->
                                        val quantityToDeduct = selectedItemQuantities[stockItem.item.id]
                                        val isChecked = quantityToDeduct != null
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    if (isChecked) {
                                                        selectedItemQuantities.remove(stockItem.item.id)
                                                    } else {
                                                        selectedItemQuantities[stockItem.item.id] = 1
                                                    }
                                                }
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Checkbox(
                                                    checked = isChecked,
                                                    onCheckedChange = { checked ->
                                                        if (checked == true) {
                                                            selectedItemQuantities[stockItem.item.id] = 1
                                                        } else {
                                                            selectedItemQuantities.remove(stockItem.item.id)
                                                        }
                                                    }
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Column {
                                                    Text(
                                                        text = stockItem.product.name,
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 14.sp
                                                    )
                                                    if (!stockItem.product.brand.isNullOrBlank()) {
                                                        Text(
                                                            text = stockItem.product.brand,
                                                            fontSize = 11.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                            
                                            if (isChecked) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    IconButton(
                                                        onClick = {
                                                            if (quantityToDeduct > 1) {
                                                                selectedItemQuantities[stockItem.item.id] = quantityToDeduct - 1
                                                            } else {
                                                                selectedItemQuantities.remove(stockItem.item.id)
                                                            }
                                                        },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Remove,
                                                            contentDescription = "Diminuir",
                                                            tint = MaterialTheme.colorScheme.error,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                    
                                                    Text(
                                                        text = "$quantityToDeduct",
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.padding(horizontal = 4.dp)
                                                    )
                                                    
                                                    IconButton(
                                                        onClick = {
                                                            if (quantityToDeduct < stockItem.item.quantity) {
                                                                selectedItemQuantities[stockItem.item.id] = quantityToDeduct + 1
                                                            }
                                                        },
                                                        modifier = Modifier.size(28.dp),
                                                        enabled = quantityToDeduct < stockItem.item.quantity
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Add,
                                                            contentDescription = "Aumentar",
                                                            tint = if (quantityToDeduct < stockItem.item.quantity) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                    
                                                    Text(
                                                        text = "/ ${stockItem.item.quantity}",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            } else {
                                                Text(
                                                    text = "Qtd: ${stockItem.item.quantity}",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(end = 8.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (selectedItemQuantities.isNotEmpty()) {
                                viewModel.consumeSelectedInventoryItems(selectedItemQuantities.toMap())
                            }
                            viewModel.toggleMealPlanDone(plan)
                            showConsumeConfirmDialog = false
                            selectedPlanToToggle = null
                        }
                    ) {
                        Text("Sim, Consumir (${selectedItemQuantities.size})")
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(
                            onClick = {
                                viewModel.toggleMealPlanDone(plan)
                                showConsumeConfirmDialog = false
                                selectedPlanToToggle = null
                            }
                        ) {
                            Text("Apenas Concluir")
                        }
                        TextButton(
                            onClick = {
                                showConsumeConfirmDialog = false
                                selectedPlanToToggle = null
                            }
                        ) {
                            Text("Cancelar", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }

        // Diálogo para Adição Manual
        if (showManualAddDialog) {
            AlertDialog(
                onDismissRequest = { showManualAddDialog = false },
                title = { Text("Adicionar Refeição") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Adicionar prato para ${daysOfWeek.find { it.first == selectedDayForAdd }?.second} no ${mealTypes.find { it.first == selectedMealTypeForAdd }?.second}:"
                        )
                        OutlinedTextField(
                            value = manualMealName,
                            onValueChange = { manualMealName = it },
                            label = { Text("Nome da Receita") },
                            placeholder = { Text("Ex: Frango Assado com Salada") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (manualMealName.isNotBlank()) {
                                viewModel.addManualMeal(selectedDayForAdd, selectedMealTypeForAdd, manualMealName)
                                showManualAddDialog = false
                            }
                        }
                    ) {
                        Text("Adicionar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showManualAddDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        // Diálogo de Loading do Gemini AI
        if (uiState.isLoadingGemini) {
            AlertDialog(
                onDismissRequest = { },
                confirmButton = {},
                dismissButton = {},
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(24.dp))
                        Text(
                            "O Gemini está a analisar o vosso stock para sugerir receitas deliciosas e evitar desperdício...",
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            )
        }

        // Diálogo de Erro do Gemini AI
        if (uiState.error != null) {
            AlertDialog(
                onDismissRequest = { viewModel.clearSuggestions() },
                title = { Text("Desperdiçómetro Inteligente") },
                text = { Text(uiState.error!!) },
                confirmButton = {
                    Button(onClick = { viewModel.clearSuggestions() }) {
                        Text("OK")
                    }
                }
            )
        }

        // Diálogo com as Sugestões do Gemini AI
        if (uiState.geminiSuggestions.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = { viewModel.clearSuggestions() },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Sugestões do Gemini AI", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Box(modifier = Modifier.fillMaxHeight(0.65f)) {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(uiState.geminiSuggestions) { recipe ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.selectGeminiSuggestion(
                                                selectedDayForAdd,
                                                selectedMealTypeForAdd,
                                                recipe
                                            )
                                        },
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(recipe.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                                        Spacer(Modifier.height(4.dp))
                                        Text(recipe.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        
                                        Spacer(Modifier.height(8.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Spacer(Modifier.width(4.dp))
                                                Text(recipe.preparationTime, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.StarBorder, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Spacer(Modifier.width(4.dp))
                                                Text(recipe.difficulty, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                        
                                        Spacer(Modifier.height(8.dp))
                                        Text("🛒 Ingredientes da receita:", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                                        recipe.ingredients.take(4).forEach { ing ->
                                            Text("• $ing", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        if (recipe.ingredients.size > 4) {
                                            Text("...e mais ${recipe.ingredients.size - 4} ingredientes", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                        }
                                        
                                        Spacer(Modifier.height(12.dp))
                                        Button(
                                            onClick = {
                                                viewModel.selectGeminiSuggestion(
                                                    selectedDayForAdd,
                                                    selectedMealTypeForAdd,
                                                    recipe
                                                )
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Escolher esta sugestão", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { viewModel.clearSuggestions() }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        // Diálogo para Detalhes do Plano Alimentar
        if (selectedMealForDetails != null) {
            val meal = selectedMealForDetails!!
            
            // Decodificar ingredientes offline
            val gson = Gson()
            val type = object : TypeToken<List<String>>() {}.type
            val ingredientsList: List<String> = try {
                gson.fromJson(meal.ingredients, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }

            AlertDialog(
                onDismissRequest = { selectedMealForDetails = null },
                title = {
                    Text(
                        text = meal.recipeName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                text = {
                    Box(modifier = Modifier.fillMaxHeight(0.6f)) {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (ingredientsList.isNotEmpty()) {
                                item {
                                    Text("📝 Ingredientes Necessários:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                }
                                items(ingredientsList) { ing ->
                                    Text("• $ing", style = MaterialTheme.typography.bodyMedium)
                                }
                                item { Spacer(Modifier.height(8.dp)) }
                            }

                            if (!meal.instructions.isNullOrBlank()) {
                                item {
                                    Text("🍳 Modo de Preparação / Detalhes:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                }
                                item {
                                    Text(meal.instructions, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                viewModel.deleteMealPlan(meal.id)
                                selectedMealForDetails = null
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Eliminar")
                        }
                        Button(onClick = { selectedMealForDetails = null }) {
                            Text("Fechar")
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun DayCard(
    dayLabel: String,
    dayKey: String,
    mealTypes: List<Pair<String, String>>,
    mealPlans: Map<String, MealPlanEntity>,
    onAddManual: (String) -> Unit,
    onAddGemini: (String) -> Unit,
    onToggleDone: (MealPlanEntity) -> Unit,
    onViewDetails: (MealPlanEntity) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Cabeçalho do Dia (Ex: Segunda-feira)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp, 24.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            ),
                            RoundedCornerShape(4.dp)
                        )
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = dayLabel,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(Modifier.height(12.dp))

            // 3 slots de refeições (Matabixo, Almoço, Jantar)
            mealTypes.forEach { (typeKey, typeLabel) ->
                val plan = mealPlans[typeKey]
                MealSlotRow(
                    mealLabel = typeLabel,
                    plan = plan,
                    onAddManual = { onAddManual(typeKey) },
                    onAddGemini = { onAddGemini(typeKey) },
                    onToggleDone = { if (plan != null) onToggleDone(plan) },
                    onViewDetails = { if (plan != null) onViewDetails(plan) }
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun MealSlotRow(
    mealLabel: String,
    plan: MealPlanEntity?,
    onAddManual: () -> Unit,
    onAddGemini: () -> Unit,
    onToggleDone: () -> Unit,
    onViewDetails: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = plan != null) { onViewDetails() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (plan != null) {
                if (plan.isDone) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
            }
        ),
        border = BorderStroke(
            1.dp, 
            if (plan != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Lado Esquerdo: Tipo de Refeição + Título do prato
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mealLabel,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(2.dp))
                if (plan != null) {
                    Text(
                        text = plan.recipeName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (plan.isDone) TextDecoration.LineThrough else null,
                        color = if (plan.isDone) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) 
                                else MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    Text(
                        text = "Sem prato planeado",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }

            // Lado Direito: Ações
            if (plan != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Checkbox de Concluído
                    IconButton(onClick = onToggleDone) {
                        Icon(
                            imageVector = if (plan.isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = "Concluir",
                            tint = if (plan.isDone) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                // Ações de criação
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Botão Manual
                    FilledTonalIconButton(
                        onClick = onAddManual,
                        modifier = Modifier.size(32.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Icon(
                            Icons.Default.Add, 
                            contentDescription = "Manual", 
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Botão Gemini Suggestions
                    FilledIconButton(
                        onClick = onAddGemini,
                        modifier = Modifier.size(32.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome, 
                            contentDescription = "Gemini IA", 
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}
