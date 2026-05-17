package dam_a52057.wastewatch.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import dam_a52057.wastewatch.data.local.dao.ShoppingItemDao
import dam_a52057.wastewatch.data.local.entity.ShoppingItemEntity
import dam_a52057.wastewatch.data.model.RemoteShoppingItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShoppingRepository @Inject constructor(
    private val shoppingItemDao: ShoppingItemDao,
    private val authRepository: AuthRepository
) {
    private val db = FirebaseFirestore.getInstance()
    private var syncListener: ListenerRegistration? = null
    private val repositoryScope = CoroutineScope(Dispatchers.IO)
    private val beingProcessedRemoteIds = mutableSetOf<String>()

    fun getAllItems(): Flow<List<ShoppingItemEntity>> =
        shoppingItemDao.getAllItems()

    suspend fun addItem(item: ShoppingItemEntity): Long {
        val id = shoppingItemDao.insert(item)
        syncItemToCloud(id.toInt())
        return id
    }

    suspend fun updateItem(item: ShoppingItemEntity) {
        shoppingItemDao.update(item)
        syncItemToCloud(item.id)
    }

    suspend fun deleteItem(id: Int) {
        val item = shoppingItemDao.getById(id)
        val householdId = getHouseholdId()
        if (householdId != null && item?.remoteId != null) {
            db.collection("households").document(householdId)
                .collection("shopping").document(item.remoteId).delete()
        }
        shoppingItemDao.deleteById(id)
    }

    suspend fun markAsPurchased(id: Int) {
        shoppingItemDao.markAsPurchased(id)
        syncItemToCloud(id)
    }

    suspend fun clearAll() {
        val householdId = getHouseholdId()
        if (householdId != null) {
            val items = db.collection("households").document(householdId)
                .collection("shopping").get().await()
            items.forEach { it.reference.delete() }
        }
        shoppingItemDao.clearAll()
    }

    suspend fun clearPurchased() {
        val items = shoppingItemDao.getAllItems().first()
        val householdId = getHouseholdId()
        items.filter { it.isPurchased }.forEach { item ->
            if (householdId != null && item.remoteId != null) {
                db.collection("households").document(householdId)
                    .collection("shopping").document(item.remoteId).delete()
            }
            shoppingItemDao.deleteById(item.id)
        }
    }

    // --- Lógica de Sincronização ---

    fun startSync() {
        repositoryScope.launch {
            try {
                val householdId = getHouseholdId() ?: return@launch
                
                // Upload itens locais órfãos
                val localItems = shoppingItemDao.getAllItems().first()
                localItems.forEach { 
                    if (it.remoteId == null) syncItemToCloud(it.id)
                }

                syncListener?.remove()
                syncListener = db.collection("households").document(householdId)
                    .collection("shopping")
                    .addSnapshotListener { snapshot, e ->
                        if (e != null || snapshot == null) return@addSnapshotListener
                        
                        repositoryScope.launch {
                            try {
                                snapshot.documentChanges.forEach { change ->
                                    val docId = change.document.id
                                    if (beingProcessedRemoteIds.contains(docId)) return@forEach
                                    
                                    val remoteItem = change.document.toObject(RemoteShoppingItem::class.java).copy(id = docId)
                                    when (change.type) {
                                        com.google.firebase.firestore.DocumentChange.Type.ADDED,
                                        com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                                            handleRemoteChange(remoteItem, householdId)
                                        }
                                        com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                                            shoppingItemDao.deleteByRemoteId(remoteItem.id)
                                        }
                                    }
                                }
                            } catch (err: Exception) {
                                err.printStackTrace()
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
            val householdId = getHouseholdId() ?: return
            val item = shoppingItemDao.getById(localId) ?: return
            
            val remoteItem = RemoteShoppingItem(
                name = item.name,
                quantity = item.quantity,
                isPurchased = item.isPurchased,
                lastUpdated = System.currentTimeMillis()
            )

            val collection = db.collection("households").document(householdId).collection("shopping")
            
            if (item.remoteId != null) {
                beingProcessedRemoteIds.add(item.remoteId)
                collection.document(item.remoteId).set(remoteItem).await()
                beingProcessedRemoteIds.remove(item.remoteId)
            } else {
                val docRef = collection.add(remoteItem).await()
                beingProcessedRemoteIds.add(docRef.id)
                shoppingItemDao.updateRemoteId(localId, docRef.id, householdId)
                delay(500)
                beingProcessedRemoteIds.remove(docRef.id)
            }
        } catch (err: Exception) {
            err.printStackTrace()
        }
    }

    private suspend fun handleRemoteChange(remoteItem: RemoteShoppingItem, householdId: String) {
        val existingItem = shoppingItemDao.getByRemoteId(remoteItem.id)
        
        // Lógica de deduplicação (órfãos)
        var finalItem = existingItem
        if (finalItem == null) {
            val orphans = shoppingItemDao.getAllItems().first()
            finalItem = orphans.find { 
                it.remoteId == null && it.name == remoteItem.name && it.quantity == remoteItem.quantity 
            }
            if (finalItem != null) {
                shoppingItemDao.updateRemoteId(finalItem.id, remoteItem.id, householdId)
            }
        }

        val newItem = ShoppingItemEntity(
            id = finalItem?.id ?: 0,
            name = remoteItem.name,
            quantity = remoteItem.quantity,
            isPurchased = remoteItem.isPurchased,
            remoteId = remoteItem.id,
            householdId = householdId,
            lastUpdated = remoteItem.lastUpdated
        )

        if (finalItem == null) {
            shoppingItemDao.insert(newItem)
        } else if (remoteItem.lastUpdated > finalItem.lastUpdated) {
            shoppingItemDao.update(newItem)
        }
    }

    private suspend fun getHouseholdId(): String? {
        val userId = authRepository.currentUser?.uid ?: return null
        return authRepository.getUserData(userId).getOrNull()?.householdId
    }

    fun stopSync() {
        syncListener?.remove()
        syncListener = null
    }
}
