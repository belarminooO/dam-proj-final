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
import dam_a52057.wastewatch.data.model.User
import dam_a52057.wastewatch.data.model.PartyGroup

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

    var showJoinGroupDialog by remember { mutableStateOf(false) }
    var joinGroupCode by remember { mutableStateOf("") }
    
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var groupName by remember { mutableStateOf("") }

    // Dialogs de Confirmação - Casa
    var showDeleteHouseConfirm by remember { mutableStateOf(false) }
    var showLeaveHouseConfirm by remember { mutableStateOf(false) }
    var memberToRemoveFromHouse by remember { mutableStateOf<User?>(null) }

    // Dialogs de Confirmação - Grupos de Festa
    var selectedGroupForDetails by remember { mutableStateOf<PartyGroup?>(null) }
    var showDeleteGroupConfirm by remember { mutableStateOf<PartyGroup?>(null) }
    var showLeaveGroupConfirm by remember { mutableStateOf<PartyGroup?>(null) }
    var memberToRemoveFromGroup by remember { mutableStateOf<User?>(null) }

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
                    val currentUser = uiState.user
                    val isCreator = currentUser?.uid == uiState.householdCreatorId
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Código de Convite:", style = MaterialTheme.typography.labelMedium)
                                    Text(
                                        text = uiState.inviteCode ?: "Membro Ativo",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (isCreator) {
                                    Button(
                                        onClick = { showDeleteHouseConfirm = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null)
                                        Spacer(Modifier.width(4.dp))
                                        Text("Eliminar")
                                    }
                                } else {
                                    Button(
                                        onClick = { showLeaveHouseConfirm = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Icon(Icons.Default.ExitToApp, contentDescription = null)
                                        Spacer(Modifier.width(4.dp))
                                        Text("Sair")
                                    }
                                }
                            }
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
                    val currentUser = uiState.user
                    val isCreator = currentUser?.uid == uiState.householdCreatorId
                    ListItem(
                        headlineContent = { Text(member.name) },
                        supportingContent = { Text(member.email) },
                        leadingContent = { Icon(Icons.Default.Person, contentDescription = null) },
                        trailingContent = {
                            if (isCreator && member.uid != currentUser?.uid) {
                                IconButton(onClick = { memberToRemoveFromHouse = member }) {
                                    Icon(
                                        Icons.Default.RemoveCircleOutline,
                                        contentDescription = "Remover membro",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    )
                }
            }

            // Grupos (Festas)
            item {
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text("Grupos de Festa (Colaboração)", style = MaterialTheme.typography.titleMedium)
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showCreateGroupDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Criar Grupo")
                    }
                    OutlinedButton(
                        onClick = { showJoinGroupDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.GroupAdd, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Aderir a Grupo")
                    }
                }
            }

            if (uiState.partyGroups.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                            Text(
                                "Não pertences a nenhum grupo de festa. Cria ou adere a um!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(uiState.partyGroups) { group ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            selectedGroupForDetails = group
                            viewModel.loadGroupMembers(group.id)
                        },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = group.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(Icons.Default.ChevronRight, contentDescription = "Ver detalhes")
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${group.members.size} membros",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = group.inviteCode,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs de Ações e Criação
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

    if (showCreateGroupDialog) {
        AlertDialog(
            onDismissRequest = { showCreateGroupDialog = false },
            title = { Text("Criar Grupo de Festa") },
            text = {
                Column {
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        placeholder = { Text("Ex: Churrasco de Domingo") },
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
                    viewModel.createPartyGroup(groupName)
                    showCreateGroupDialog = false
                }) { Text("Criar") }
            }
        )
    }

    if (showJoinGroupDialog) {
        AlertDialog(
            onDismissRequest = { showJoinGroupDialog = false },
            title = { Text("Aderir a Grupo de Festa") },
            text = {
                Column {
                    OutlinedTextField(
                        value = joinGroupCode,
                        onValueChange = { joinGroupCode = it },
                        placeholder = { Text("Inserir código do grupo") },
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
                    viewModel.joinPartyGroup(joinGroupCode)
                    showJoinGroupDialog = false
                }) { Text("Aderir") }
            }
        )
    }

    if (uiState.createdGroupInviteCode != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearGroupInviteCode() },
            title = { Text("Grupo Criado com Sucesso!") },
            text = {
                Column {
                    Text("O teu grupo foi criado! Partilha o código abaixo para que os teus convidados possam aderir:")
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = uiState.createdGroupInviteCode!!,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.clearGroupInviteCode() }) { Text("Ok") }
            }
        )
    }

    // --- DIALOGS DE CONFIRMAÇÃO - CASA ---

    if (showDeleteHouseConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteHouseConfirm = false },
            title = { Text("Eliminar Partilha de Casa?") },
            text = { Text("Tem a certeza que deseja eliminar esta casa? Todos os membros serão removidos e a partilha do stock será desativada.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteHousehold()
                        showDeleteHouseConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteHouseConfirm = false }) { Text("Cancelar") }
            }
        )
    }

    if (showLeaveHouseConfirm) {
        AlertDialog(
            onDismissRequest = { showLeaveHouseConfirm = false },
            title = { Text("Sair da Casa?") },
            text = { Text("Tem a certeza que deseja sair desta casa? Deixará de ter acesso ao stock partilhado.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.leaveHousehold()
                        showLeaveHouseConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Sair") }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveHouseConfirm = false }) { Text("Cancelar") }
            }
        )
    }

    if (memberToRemoveFromHouse != null) {
        AlertDialog(
            onDismissRequest = { memberToRemoveFromHouse = null },
            title = { Text("Remover Membro da Casa?") },
            text = { Text("Tem a certeza que deseja remover ${memberToRemoveFromHouse!!.name} da sua casa? O acesso ao stock partilhado será revogado.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeMemberFromHousehold(memberToRemoveFromHouse!!.uid)
                        memberToRemoveFromHouse = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Remover") }
            },
            dismissButton = {
                TextButton(onClick = { memberToRemoveFromHouse = null }) { Text("Cancelar") }
            }
        )
    }

    // --- DIALOGS E DETALHES DE GRUPOS DE FESTA ---

    if (selectedGroupForDetails != null) {
        val group = selectedGroupForDetails!!
        val isGroupCreator = uiState.user?.uid == group.createdBy
        
        AlertDialog(
            onDismissRequest = { selectedGroupForDetails = null },
            title = { Text(group.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Código de Convite: ${group.inviteCode}", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.height(16.dp))
                    Text("Membros do Grupo (${uiState.selectedGroupMembers.size}):", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    
                    Box(modifier = Modifier.heightIn(max = 200.dp)) {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(uiState.selectedGroupMembers) { member ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(member.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        Text(member.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (isGroupCreator && member.uid != uiState.user?.uid) {
                                        IconButton(onClick = { memberToRemoveFromGroup = member }) {
                                            Icon(
                                                Icons.Default.RemoveCircleOutline,
                                                contentDescription = "Remover do grupo",
                                                tint = MaterialTheme.colorScheme.error
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isGroupCreator) {
                        TextButton(
                            onClick = { showDeleteGroupConfirm = group },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Eliminar")
                        }
                    } else {
                        TextButton(
                            onClick = { showLeaveGroupConfirm = group },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.ExitToApp, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Sair")
                        }
                    }
                    TextButton(onClick = { selectedGroupForDetails = null }) { Text("Fechar") }
                }
            }
        )
    }

    if (showDeleteGroupConfirm != null) {
        val group = showDeleteGroupConfirm!!
        AlertDialog(
            onDismissRequest = { showDeleteGroupConfirm = null },
            title = { Text("Eliminar Grupo?") },
            text = { Text("Tem a certeza que deseja eliminar o grupo '${group.name}'? Todos os membros serão removidos.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePartyGroup(group.id)
                        showDeleteGroupConfirm = null
                        selectedGroupForDetails = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteGroupConfirm = null }) { Text("Cancelar") }
            }
        )
    }

    if (showLeaveGroupConfirm != null) {
        val group = showLeaveGroupConfirm!!
        AlertDialog(
            onDismissRequest = { showLeaveGroupConfirm = null },
            title = { Text("Sair do Grupo?") },
            text = { Text("Tem a certeza que deseja sair do grupo '${group.name}'? Deixará de partilhar o stock para as receitas conjuntas.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.leavePartyGroup(group.id)
                        showLeaveGroupConfirm = null
                        selectedGroupForDetails = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Sair") }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveGroupConfirm = null }) { Text("Cancelar") }
            }
        )
    }

    if (memberToRemoveFromGroup != null) {
        val group = selectedGroupForDetails!!
        AlertDialog(
            onDismissRequest = { memberToRemoveFromGroup = null },
            title = { Text("Remover Membro?") },
            text = { Text("Tem a certeza que deseja remover '${memberToRemoveFromGroup!!.name}' do grupo '${group.name}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeMemberFromPartyGroup(group.id, memberToRemoveFromGroup!!.uid)
                        memberToRemoveFromGroup = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Remover") }
            },
            dismissButton = {
                TextButton(onClick = { memberToRemoveFromGroup = null }) { Text("Cancelar") }
            }
        )
    }
}
