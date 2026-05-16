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

            // Search bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                label = { Text("Pesquisar produtos...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

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
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.items, key = { it.item.id }) { item ->
                        InventoryItemCard(
                            itemWithProduct = item,
                            onClick = { onNavigateToDetail(item.item.id) },
                            onConsume = { viewModel.consumeItem(item.item) },
                            onDelete = { viewModel.deleteItem(item.item) }
                        )
                    }
                }
            }
        }
    }
}
