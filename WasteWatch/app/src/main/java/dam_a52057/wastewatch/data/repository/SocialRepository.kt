package dam_a52057.wastewatch.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import dam_a52057.wastewatch.data.model.User
import kotlinx.coroutines.tasks.await
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

interface SocialRepository {
    suspend fun createHousehold(name: String, userId: String): Result<String>
    suspend fun joinHousehold(inviteCode: String, userId: String): Result<Unit>
    suspend fun getHouseholdMembers(householdId: String): Result<List<User>>
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
                "members" to listOf(userId)
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
}
