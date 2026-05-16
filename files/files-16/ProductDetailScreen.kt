package dam_a52057.wastewatch.ui.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dam_a52057.wastewatch.ui.components.ExpiryBadge
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    itemId: Int,
    onNavigateBack: () -> Unit,
    viewModel: InventoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val item = uiState.items.firstOrNull { it.id == itemId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(item?.productId?.toString() ?: "Detalhe") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        item?.let { viewModel.deleteItem(it) }
                        onNavigateBack()
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                    }
                }
            )
        }
    ) { padding ->
        if (item == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Item nao encontrado")
            }
            return@Scaffold
        }

        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val expiryFormatted = sdf.format(Date(item.expiryDate))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Badge de urgência
            ExpiryBadge(expiryDateMillis = item.expiryDate)

            // Informações
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DetailRow(label = "Data de validade", value = expiryFormatted)
                    DetailRow(label = "Quantidade", value = item.quantity.toString())
                    DetailRow(label = "Local", value = item.storageLocation)
                    if (item.barcode != null) {
                        DetailRow(label = "Codigo de barras", value = item.barcode)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Botao Consumir
            Button(
                onClick = {
                    viewModel.consumeItem(item)
                    onNavigateBack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Marcar como Consumido")
            }

            // Botao Eliminar
            OutlinedButton(
                onClick = {
                    viewModel.deleteItem(item)
                    onNavigateBack()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Eliminar")
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
