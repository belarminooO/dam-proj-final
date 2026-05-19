package dam_a52057.wastewatch.ui.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dam_a52057.wastewatch.ui.components.InventoryItemCard
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    onNavigateToDetail: (Int) -> Unit,
    onNavigateToAddProduct: () -> Unit,
    viewModel: InventoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val locations = listOf("Frigorífico", "Congelador", "Despensa")
    var locationExpanded by remember { mutableStateOf(false) }

    var showQuickConsumeDialog by remember { mutableStateOf(false) }

    if (uiState.showShoppingDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissShoppingDialog() },
            title = { Text("Repor Stock") },
            text = { Text("Acabaste de consumir o último ${uiState.itemToAddToShopping?.product?.name}. Queres adicioná-lo à lista de compras?") },
            confirmButton = {
                Button(onClick = { viewModel.confirmConsumption(true) }) {
                    Text("Sim, adicionar")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.confirmConsumption(false) }) {
                    Text("Não, apenas consumir")
                }
            }
        )
    }

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

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAddProduct) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar produto")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Inventário",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Search bar with Autocomplete
            var searchExpanded by remember { mutableStateOf(false) }
            
            ExposedDropdownMenuBox(
                expanded = searchExpanded && uiState.suggestions.isNotEmpty(),
                onExpandedChange = { searchExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = {
                        viewModel.onSearchQueryChange(it)
                        searchExpanded = true
                    },
                    label = { Text("Pesquisar produtos...") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    singleLine = true
                )
                
                ExposedDropdownMenu(
                    expanded = searchExpanded && uiState.suggestions.isNotEmpty(),
                    onDismissRequest = { searchExpanded = false }
                ) {
                    uiState.suggestions.forEach { suggestion ->
                        DropdownMenuItem(
                            text = { Text(suggestion) },
                            onClick = {
                                viewModel.onSearchQueryChange(suggestion)
                                searchExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Location filter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExposedDropdownMenuBox(
                    expanded = locationExpanded,
                    onExpandedChange = { locationExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = uiState.selectedLocation ?: "Todos os locais",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Local") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = locationExpanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = locationExpanded,
                        onDismissRequest = { locationExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Todos os locais") },
                            onClick = {
                                viewModel.onLocationFilterChanged(null)
                                locationExpanded = false
                            }
                        )
                        locations.forEach { location ->
                            DropdownMenuItem(
                                text = { Text(location) },
                                onClick = {
                                    viewModel.onLocationFilterChanged(location)
                                    locationExpanded = false
                                }
                            )
                        }
                    }
                }

                // Botão "Dar Baixa" ao lado do filtro de local
                Button(
                    onClick = { showQuickConsumeDialog = true },
                    modifier = Modifier
                        .height(56.dp)
                        .padding(top = 8.dp), // Align perfectly with the OutlinedTextField's box boundary
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = "Dar baixa no stock"
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Dar Baixa", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${uiState.items.size} itens",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.items.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (uiState.searchQuery.isEmpty()) "O teu inventário está vazio" else "Nenhum item corresponde à pesquisa",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (uiState.searchQuery.isEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = onNavigateToAddProduct) {
                            Text("Adicionar Primeiro Produto")
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { viewModel.seedTestData() }
                        ) {
                            Text("Popular Stock de Teste ✨")
                        }
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.items, key = { it.item.id }) { item ->
                        InventoryItemCard(
                            itemWithProduct = item,
                            onClick = { onNavigateToDetail(item.item.id) },
                            onConsume = { viewModel.consumeItem(item) },
                            onDelete = { viewModel.deleteItem(item.item) }
                        )
                    }
                }
            }
        }
    }

    if (showQuickConsumeDialog) {
        val selectedItemQuantities = remember { mutableStateMapOf<Int, Int>() }
        var searchQuery by remember { mutableStateOf("") }

        val filteredInventory = uiState.allActiveItems.filter {
            it.product.name.contains(searchQuery, ignoreCase = true) ||
            (it.product.brand?.contains(searchQuery, ignoreCase = true) == true)
        }

        AlertDialog(
            onDismissRequest = { showQuickConsumeDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.RestaurantMenu,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Dar Baixa no Stock 🍳", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Selecione quais os ingredientes do vosso stock que consumiu:",
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
                        Box(modifier = Modifier.heightIn(max = 200.dp)) {
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
                            viewModel.consumeSelectedItems(selectedItemQuantities.toMap())
                        }
                        showQuickConsumeDialog = false
                    },
                    enabled = selectedItemQuantities.isNotEmpty()
                ) {
                    Text("Consumir (${selectedItemQuantities.size})")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showQuickConsumeDialog = false }
                ) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }
}
