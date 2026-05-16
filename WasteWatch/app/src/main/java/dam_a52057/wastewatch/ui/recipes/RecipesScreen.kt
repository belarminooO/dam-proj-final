package dam_a52057.wastewatch.ui.recipes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dam_a52057.wastewatch.data.remote.RecipeAi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipesScreen(
    viewModel: RecipesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Gerar receitas automaticamente ao abrir o ecrã se a lista estiver vazia
    LaunchedEffect(Unit) {
        if (uiState.recipes.isEmpty() && !uiState.isLoading) {
            viewModel.generateRecipes()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sugestões AI") },
                actions = {
                    IconButton(onClick = { viewModel.generateRecipes() }, enabled = !uiState.isLoading) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Regerar")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                uiState.isLoading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text("O Chef Gemini está a pensar...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                uiState.error != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = uiState.error!!,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.generateRecipes() }) {
                            Text("Tentar Novamente")
                        }
                    }
                }
                uiState.recipes.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Sem receitas de momento.")
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(uiState.recipes) { recipe ->
                            RecipeCard(recipe)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecipeCard(recipe: RecipeAi) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = recipe.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = recipe.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(Modifier.height(8.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(recipe.preparationTime, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.width(16.dp))
                SuggestionChip(
                    onClick = {},
                    label = { Text(recipe.difficulty) },
                    enabled = false
                )
            }
            
            Divider(Modifier.padding(vertical = 12.dp))
            
            Text("Ingredientes:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            recipe.ingredients.forEach { ingredient ->
                Text("• $ingredient", style = MaterialTheme.typography.bodySmall)
            }
            
            Spacer(Modifier.height(12.dp))
            
            Text("Instruções:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            recipe.instructions.forEachIndexed { index, step ->
                Text("${index + 1}. $step", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
