package dam_a52057.wastewatch.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialHubScreen(
    onLogout: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: SocialHubViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showJoinDialog by remember { mutableStateOf(false) }
    var joinCode by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var houseName by remember { mutableStateOf("") }

    LaunchedEffect(uiState.isLoggedOut) {
        if (uiState.isLoggedOut) onLogout()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Central Social") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.logout() }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sair")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Perfil
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                text = uiState.user?.name ?: "Utilizador",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = uiState.user?.email ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Minha Casa (Household)
            item {
                Text("Minha Casa (Partilha de Stock)", style = MaterialTheme.typography.titleMedium)
            }

            if (uiState.user?.householdId == null) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { showCreateDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Criar Minha Casa")
                        }
                        OutlinedButton(
                            onClick = { showJoinDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.GroupAdd, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Aderir a uma Casa")
                        }
                    }
                }
            } else {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Código de Convite:", style = MaterialTheme.typography.labelMedium)
                            Text(
                                text = uiState.inviteCode ?: "Membro Ativo",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Partilha este código com quem vive contigo para dividirem o mesmo stock.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                item {
                    Text("Membros da Casa:", style = MaterialTheme.typography.labelLarge)
                }

                items(uiState.householdMembers) { member ->
                    ListItem(
                        headlineContent = { Text(member.name) },
                        supportingContent = { Text(member.email) },
                        leadingContent = { Icon(Icons.Default.Person, contentDescription = null) }
                    )
                }
            }

            // Grupos (Festas) - Placeholder
            item {
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text("Grupos de Festa (Colaboração)", style = MaterialTheme.typography.titleMedium)
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(Modifier.padding(16.dp)) {
                        Text("Funcionalidade de Grupos/Festas em desenvolvimento...")
                    }
                }
            }
        }
    }

    // Dialogs
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Nome da Casa") },
            text = {
                Column {
                    OutlinedTextField(
                        value = houseName,
                        onValueChange = { houseName = it },
                        placeholder = { Text("Ex: Casa do Belarmino") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (uiState.error != null) {
                        Text(
                            text = uiState.error!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.createHousehold(houseName)
                    showCreateDialog = false
                }) { Text("Criar") }
            }
        )
    }

    if (showJoinDialog) {
        AlertDialog(
            onDismissRequest = { showJoinDialog = false },
            title = { Text("Aderir a uma Casa") },
            text = {
                Column {
                    OutlinedTextField(
                        value = joinCode,
                        onValueChange = { joinCode = it },
                        placeholder = { Text("Inserir código (6 dígitos)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (uiState.error != null) {
                        Text(
                            text = uiState.error!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.joinHousehold(joinCode)
                    showJoinDialog = false
                }) { Text("Aderir") }
            }
        )
    }
}
