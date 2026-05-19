package dam_a52057.wastewatch.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import dam_a52057.wastewatch.data.local.dao.MealPlanDao
import dam_a52057.wastewatch.data.local.entity.MealPlanEntity
import dam_a52057.wastewatch.data.model.RemoteMealPlan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MealPlanRepository @Inject constructor(
    private val mealPlanDao: MealPlanDao
) {
    private val db = FirebaseFirestore.getInstance()
    private var householdSyncListener: ListenerRegistration? = null
    private val groupSyncListeners = mutableMapOf<String, ListenerRegistration>()
    
    private val repositoryScope = CoroutineScope(Dispatchers.IO)
    private val beingProcessedRemoteIds = mutableSetOf<String>()

    fun getPersonalMealPlansForWeek(weekStartDate: Long): Flow<List<MealPlanEntity>> =
        mealPlanDao.getPersonalMealPlansForWeek(weekStartDate)

    fun getHouseholdMealPlansForWeek(householdId: String, weekStartDate: Long): Flow<List<MealPlanEntity>> =
        mealPlanDao.getHouseholdMealPlansForWeek(householdId, weekStartDate)

    fun getGroupMealPlansForWeek(groupId: String, weekStartDate: Long): Flow<List<MealPlanEntity>> =
        mealPlanDao.getGroupMealPlansForWeek(groupId, weekStartDate)

    suspend fun addMealPlan(item: MealPlanEntity): Long {
        val id = mealPlanDao.insert(item)
        syncItemToCloud(id.toInt())
        return id
    }

    suspend fun updateMealPlan(item: MealPlanEntity) {
        mealPlanDao.update(item)
        syncItemToCloud(item.id)
    }

    suspend fun deleteMealPlan(id: Int) {
        val item = mealPlanDao.getById(id) ?: return
        if (item.householdId != null && item.remoteId != null) {
            db.collection("households").document(item.householdId)
                .collection("meal_plans").document(item.remoteId).delete()
        } else if (item.groupId != null && item.remoteId != null) {
            db.collection("groups").document(item.groupId)
                .collection("meal_plans").document(item.remoteId).delete()
        }
        mealPlanDao.deleteById(id)
    }

    // --- Sincronização Cloud ---

    fun startHouseholdSync(householdId: String) {
        repositoryScope.launch {
            try {
                householdSyncListener?.remove()
                householdSyncListener = db.collection("households").document(householdId)
                    .collection("meal_plans")
                    .addSnapshotListener { snapshot, e ->
                        if (e != null || snapshot == null) return@addSnapshotListener
                        repositoryScope.launch {
                            snapshot.documentChanges.forEach { change ->
                                val docId = change.document.id
                                if (beingProcessedRemoteIds.contains(docId)) return@forEach
                                val remoteItem = change.document.toObject(RemoteMealPlan::class.java).copy(id = docId)
                                when (change.type) {
                                    com.google.firebase.firestore.DocumentChange.Type.ADDED,
                                    com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                                        handleRemoteChange(remoteItem, householdId = householdId, groupId = null)
                                    }
                                    com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                                        mealPlanDao.deleteByRemoteId(remoteItem.id)
                                    }
                                }
                            }
                        }
                    }
            } catch (err: Exception) {
                err.printStackTrace()
            }
        }
    }

    fun startGroupSync(groupId: String) {
        repositoryScope.launch {
            try {
                groupSyncListeners[groupId]?.remove()
                groupSyncListeners[groupId] = db.collection("groups").document(groupId)
                    .collection("meal_plans")
                    .addSnapshotListener { snapshot, e ->
                        if (e != null || snapshot == null) return@addSnapshotListener
                        repositoryScope.launch {
                            snapshot.documentChanges.forEach { change ->
                                val docId = change.document.id
                                if (beingProcessedRemoteIds.contains(docId)) return@forEach
                                val remoteItem = change.document.toObject(RemoteMealPlan::class.java).copy(id = docId)
                                when (change.type) {
                                    com.google.firebase.firestore.DocumentChange.Type.ADDED,
                                    com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                                        handleRemoteChange(remoteItem, householdId = null, groupId = groupId)
                                    }
                                    com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                                        mealPlanDao.deleteByRemoteId(remoteItem.id)
                                    }
                                }
                            }
                        }
                    }
            } catch (err: Exception) {
                err.printStackTrace()
            }
        }
    }

    private suspend fun syncItemToCloud(localId: Int) {
        try {
            val item = mealPlanDao.getById(localId) ?: return
            val remoteItem = RemoteMealPlan(
                weekStartDate = item.weekStartDate,
                dayOfWeek = item.dayOfWeek,
                mealType = item.mealType,
                recipeName = item.recipeName,
                ingredients = item.ingredients,
                instructions = item.instructions,
                isDone = item.isDone,
                lastUpdated = System.currentTimeMillis()
            )

            if (item.householdId != null) {
                val collection = db.collection("households").document(item.householdId).collection("meal_plans")
                if (item.remoteId != null) {
                    beingProcessedRemoteIds.add(item.remoteId)
                    collection.document(item.remoteId).set(remoteItem).await()
                    beingProcessedRemoteIds.remove(item.remoteId)
                } else {
                    val docRef = collection.add(remoteItem).await()
                    beingProcessedRemoteIds.add(docRef.id)
                    mealPlanDao.update(item.copy(remoteId = docRef.id))
                    delay(500)
                    beingProcessedRemoteIds.remove(docRef.id)
                }
            } else if (item.groupId != null) {
                val collection = db.collection("groups").document(item.groupId).collection("meal_plans")
                if (item.remoteId != null) {
                    beingProcessedRemoteIds.add(item.remoteId)
                    collection.document(item.remoteId).set(remoteItem).await()
                    beingProcessedRemoteIds.remove(item.remoteId)
                } else {
                    val docRef = collection.add(remoteItem).await()
                    beingProcessedRemoteIds.add(docRef.id)
                    mealPlanDao.update(item.copy(remoteId = docRef.id))
                    delay(500)
                    beingProcessedRemoteIds.remove(docRef.id)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun handleRemoteChange(remoteItem: RemoteMealPlan, householdId: String?, groupId: String?) {
        val existing = mealPlanDao.getByRemoteId(remoteItem.id)
        val newItem = MealPlanEntity(
            id = existing?.id ?: 0,
            weekStartDate = remoteItem.weekStartDate,
            dayOfWeek = remoteItem.dayOfWeek,
            mealType = remoteItem.mealType,
            recipeName = remoteItem.recipeName,
            ingredients = remoteItem.ingredients,
            instructions = remoteItem.instructions,
            isDone = remoteItem.isDone,
            remoteId = remoteItem.id,
            householdId = householdId,
            groupId = groupId,
            lastUpdated = remoteItem.lastUpdated
        )

        if (existing == null) {
            mealPlanDao.insert(newItem)
        } else if (remoteItem.lastUpdated > existing.lastUpdated) {
            mealPlanDao.update(newItem)
        }
    }

    fun stopSync() {
        householdSyncListener?.remove()
        householdSyncListener = null
        groupSyncListeners.forEach { it.value.remove() }
        groupSyncListeners.clear()
    }
}
