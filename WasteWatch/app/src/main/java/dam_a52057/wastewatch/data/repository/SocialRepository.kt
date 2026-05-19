package dam_a52057.wastewatch.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import dam_a52057.wastewatch.data.model.User
import dam_a52057.wastewatch.data.model.PartyGroup
import kotlinx.coroutines.tasks.await
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

interface SocialRepository {
    suspend fun createHousehold(name: String, userId: String): Result<String>
    suspend fun joinHousehold(inviteCode: String, userId: String): Result<Unit>
    suspend fun getHouseholdMembers(householdId: String): Result<List<User>>
    
    // Novas para Household management
    suspend fun getHouseholdCreator(householdId: String): Result<String?>
    suspend fun deleteHousehold(householdId: String): Result<Unit>
    suspend fun removeMemberFromHousehold(householdId: String, memberId: String): Result<Unit>
    suspend fun leaveHousehold(householdId: String, userId: String): Result<Unit>

    // Party Groups
    suspend fun createPartyGroup(name: String, userId: String): Result<String>
    suspend fun joinPartyGroup(inviteCode: String, userId: String): Result<Unit>
    suspend fun getUserPartyGroups(userId: String): Result<List<PartyGroup>>
    suspend fun getPartyGroupMembers(groupId: String): Result<List<User>>
    
    // Novas para Party Group management
    suspend fun deletePartyGroup(groupId: String): Result<Unit>
    suspend fun removeMemberFromPartyGroup(groupId: String, memberId: String): Result<Unit>
    suspend fun leavePartyGroup(groupId: String, userId: String): Result<Unit>
}

@Singleton
class FirestoreSocialRepository @Inject constructor() : SocialRepository {
    private val db = FirebaseFirestore.getInstance()

    override suspend fun createHousehold(name: String, userId: String): Result<String> {
        return try {
            val inviteCode = UUID.randomUUID().toString().substring(0, 6).uppercase()
            val household = hashMapOf(
                "name" to name,
                "inviteCode" to inviteCode,
                "members" to listOf(userId),
                "createdBy" to userId // Guardar quem criou para opções de moderação
            )
            val docRef = db.collection("households").add(household).await()
            
            // Atualizar o utilizador com o ID da casa
            db.collection("users").document(userId).update("householdId", docRef.id).await()
            
            Result.success(inviteCode)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun joinHousehold(inviteCode: String, userId: String): Result<Unit> {
        return try {
            val snapshot = db.collection("households")
                .whereEqualTo("inviteCode", inviteCode)
                .get()
                .await()
            
            if (snapshot.isEmpty) return Result.failure(Exception("Código inválido"))
            
            val doc = snapshot.documents.first()
            val members = doc.get("members") as? List<String> ?: emptyList()
            
            if (!members.contains(userId)) {
                db.collection("households").document(doc.id)
                    .update("members", members + userId).await()
            }
            
            db.collection("users").document(userId).update("householdId", doc.id).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getHouseholdMembers(householdId: String): Result<List<User>> {
        return try {
            val snapshot = db.collection("users")
                .whereEqualTo("householdId", householdId)
                .get()
                .await()
            
            val members = snapshot.toObjects(User::class.java)
            Result.success(members)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getHouseholdCreator(householdId: String): Result<String?> {
        return try {
            val doc = db.collection("households").document(householdId).get().await()
            val creator = doc.getString("createdBy")
            if (creator != null) {
                Result.success(creator)
            } else {
                // Retrocompatibilidade: Se a casa foi criada antes da atualização,
                // o criador é por definição o primeiro membro que se registou na lista.
                val members = doc.get("members") as? List<String> ?: emptyList()
                Result.success(members.firstOrNull())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteHousehold(householdId: String): Result<Unit> {
        return try {
            val doc = db.collection("households").document(householdId).get().await()
            val members = doc.get("members") as? List<String> ?: emptyList()
            
            // 1. Remover householdId de todos os membros
            members.forEach { memberId ->
                db.collection("users").document(memberId).update("householdId", null).await()
            }
            
            // 2. Apagar o documento da casa
            db.collection("households").document(householdId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeMemberFromHousehold(householdId: String, memberId: String): Result<Unit> {
        return try {
            val doc = db.collection("households").document(householdId).get().await()
            val members = doc.get("members") as? List<String> ?: emptyList()
            
            if (members.contains(memberId)) {
                db.collection("households").document(householdId)
                    .update("members", members - memberId).await()
            }
            
            db.collection("users").document(memberId).update("householdId", null).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun leaveHousehold(householdId: String, userId: String): Result<Unit> {
        return try {
            val doc = db.collection("households").document(householdId).get().await()
            val members = doc.get("members") as? List<String> ?: emptyList()
            
            if (members.contains(userId)) {
                db.collection("households").document(householdId)
                    .update("members", members - userId).await()
            }
            
            db.collection("users").document(userId).update("householdId", null).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Novas para Passo 25 (Grupos de Festa)
    override suspend fun createPartyGroup(name: String, userId: String): Result<String> {
        return try {
            val inviteCode = UUID.randomUUID().toString().substring(0, 6).uppercase()
            val groupId = UUID.randomUUID().toString()
            val partyGroup = PartyGroup(
                id = groupId,
                name = name,
                inviteCode = inviteCode,
                members = listOf(userId),
                createdBy = userId
            )
            db.collection("groups").document(groupId).set(partyGroup).await()
            
            // Buscar utilizador atual para obter groupIds
            val userDoc = db.collection("users").document(userId).get().await()
            val currentUser = userDoc.toObject(User::class.java)
            val currentGroupIds = currentUser?.groupIds ?: emptyList()
            
            if (!currentGroupIds.contains(groupId)) {
                db.collection("users").document(userId).update("groupIds", currentGroupIds + groupId).await()
            }
            
            Result.success(inviteCode)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun joinPartyGroup(inviteCode: String, userId: String): Result<Unit> {
        return try {
            val snapshot = db.collection("groups")
                .whereEqualTo("inviteCode", inviteCode)
                .get()
                .await()
            
            if (snapshot.isEmpty) return Result.failure(Exception("Código de grupo inválido"))
            
            val doc = snapshot.documents.first()
            val group = doc.toObject(PartyGroup::class.java) ?: return Result.failure(Exception("Erro ao converter grupo"))
            val groupId = group.id
            
            val members = group.members
            if (!members.contains(userId)) {
                db.collection("groups").document(groupId)
                    .update("members", members + userId).await()
            }
            
            val userDoc = db.collection("users").document(userId).get().await()
            val currentUser = userDoc.toObject(User::class.java)
            val currentGroupIds = currentUser?.groupIds ?: emptyList()
            
            if (!currentGroupIds.contains(groupId)) {
                db.collection("users").document(userId).update("groupIds", currentGroupIds + groupId).await()
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUserPartyGroups(userId: String): Result<List<PartyGroup>> {
        return try {
            val snapshot = db.collection("groups")
                .whereArrayContains("members", userId)
                .get()
                .await()
            
            val groups = snapshot.toObjects(PartyGroup::class.java)
            Result.success(groups)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPartyGroupMembers(groupId: String): Result<List<User>> {
        return try {
            val snapshot = db.collection("users")
                .whereArrayContains("groupIds", groupId)
                .get()
                .await()
            
            val members = snapshot.toObjects(User::class.java)
            Result.success(members)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deletePartyGroup(groupId: String): Result<Unit> {
        return try {
            val doc = db.collection("groups").document(groupId).get().await()
            val group = doc.toObject(PartyGroup::class.java) ?: return Result.failure(Exception("Grupo inválido"))
            val members = group.members
            
            // 1. Remover de todos os utilizadores
            members.forEach { memberId ->
                val userDoc = db.collection("users").document(memberId).get().await()
                val user = userDoc.toObject(User::class.java)
                val currentGroupIds = user?.groupIds ?: emptyList()
                db.collection("users").document(memberId).update("groupIds", currentGroupIds - groupId).await()
            }
            
            // 2. Apagar o grupo
            db.collection("groups").document(groupId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeMemberFromPartyGroup(groupId: String, memberId: String): Result<Unit> {
        return try {
            val doc = db.collection("groups").document(groupId).get().await()
            val group = doc.toObject(PartyGroup::class.java) ?: return Result.failure(Exception("Grupo inválido"))
            val members = group.members
            
            if (members.contains(memberId)) {
                db.collection("groups").document(groupId)
                    .update("members", members - memberId).await()
            }
            
            val userDoc = db.collection("users").document(memberId).get().await()
            val user = userDoc.toObject(User::class.java)
            val currentGroupIds = user?.groupIds ?: emptyList()
            db.collection("users").document(memberId).update("groupIds", currentGroupIds - groupId).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun leavePartyGroup(groupId: String, userId: String): Result<Unit> {
        return try {
            val doc = db.collection("groups").document(groupId).get().await()
            val group = doc.toObject(PartyGroup::class.java) ?: return Result.failure(Exception("Grupo inválido"))
            val members = group.members
            
            if (members.contains(userId)) {
                db.collection("groups").document(groupId)
                    .update("members", members - userId).await()
            }
            
            val userDoc = db.collection("users").document(userId).get().await()
            val user = userDoc.toObject(User::class.java)
            val currentGroupIds = user?.groupIds ?: emptyList()
            db.collection("users").document(userId).update("groupIds", currentGroupIds - groupId).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
