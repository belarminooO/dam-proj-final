package dam_a52057.wastewatch.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
    onNavigateToItem: (Int) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "WasteWatch",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Gerencie o seu inventario e evite desperdicio",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                    onConsume = {},
                    onDelete = {}
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