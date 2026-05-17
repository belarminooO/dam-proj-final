package dam_a52057.wastewatch.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dam_a52057.wastewatch.ui.components.InventoryItemCard

@Composable
fun HomeScreen(
    onNavigateToInventory: () -> Unit,
    onNavigateToSocial: () -> Unit,
    onNavigateToItem: (Int) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "WasteWatch",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Gerencie o seu inventário e evite desperdício",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onNavigateToSocial) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Perfil e Grupos",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    label = "Expiram Hoje",
                    count = uiState.expiresTodayCount,
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    label = "Urgentes",
                    count = uiState.urgentCount,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    label = "Total",
                    count = uiState.totalCount,
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Top 5 - Itens Mais Urgentes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onNavigateToInventory) {
                    Text("Ver Todos")
                }
            }
        }

        if (uiState.top5Items.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nenhum item no inventario",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(uiState.top5Items) { item ->
                InventoryItemCard(
                    itemWithProduct = item,
                    onClick = { onNavigateToItem(item.item.id) },
                    onConsume = { viewModel.consumeItem(item) },
                    onDelete = { viewModel.deleteItem(item.item) }
                )
            }
        }
    }
}

@Composable
fun SummaryCard(
    label: String,
    count: Int,
    containerColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}