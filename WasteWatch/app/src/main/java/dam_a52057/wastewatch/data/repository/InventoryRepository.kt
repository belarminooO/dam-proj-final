package dam_a52057.wastewatch.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import dam_a52057.wastewatch.data.local.dao.InventoryItemDao
import dam_a52057.wastewatch.data.local.dao.ProductDao
import dam_a52057.wastewatch.data.local.entity.InventoryItemEntity
import dam_a52057.wastewatch.data.local.entity.InventoryItemWithProduct
import dam_a52057.wastewatch.data.local.entity.ProductEntity
import dam_a52057.wastewatch.data.model.RemoteInventoryItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InventoryRepository @Inject constructor(
    private val inventoryItemDao: InventoryItemDao,
    private val productDao: ProductDao,
    private val authRepository: AuthRepository
) {
    private val db = FirebaseFirestore.getInstance()
    private var syncListener: ListenerRegistration? = null
    private val repositoryScope = CoroutineScope(Dispatchers.IO)
    private val syncMutex = Mutex()
    
    // Cache para evitar processar itens que nós próprios acabámos de enviar
    private val beingProcessedRemoteIds = mutableSetOf<String>()

    fun getAllActiveItems(): Flow<List<InventoryItemEntity>> =
        inventoryItemDao.getAllActiveItems()

    fun getAllActiveItemsWithProduct(): Flow<List<InventoryItemWithProduct>> =
        inventoryItemDao.getAllActiveItemsWithProduct()

    fun getCountInDateRange(start: Long, end: Long): Flow<Int> =
        inventoryItemDao.getCountInDateRange(start, end)

    fun getUrgentCount(threshold: Long): Flow<Int> =
        inventoryItemDao.getCountInDateRange(0, threshold)

    suspend fun addItem(item: InventoryItemEntity): Long {
        val updatedItem = item.copy(lastUpdated = System.currentTimeMillis())
        val id = inventoryItemDao.insert(updatedItem)
        repositoryScope.launch { syncItemToCloud(id) }
        return id
    }

    suspend fun updateItem(item: InventoryItemEntity) {
        val updatedItem = item.copy(lastUpdated = System.currentTimeMillis())
        inventoryItemDao.update(updatedItem)
        repositoryScope.launch { syncItemToCloud(updatedItem.id.toLong()) }
    }

    suspend fun deleteItem(item: InventoryItemEntity) {
        inventoryItemDao.deleteById(item.id)
        val householdId = getHouseholdId()
        if (householdId != null && item.remoteId != null) {
            db.collection("households").document(householdId)
                .collection("inventory").document(item.remoteId).delete()
        }
    }

    suspend fun consumeItem(item: InventoryItemEntity) {
        if (item.quantity > 1) {
            updateItem(item.copy(quantity = item.quantity - 1))
        } else {
            markAsConsumed(item.id)
        }
    }

    suspend fun markAsConsumed(id: Int) {
        // Atualizar timestamp local para garantir que ganhamos a qualquer mudança remota antiga
        val item = inventoryItemDao.getById(id) ?: return
        val updatedItem = item.copy(isConsumed = true, lastUpdated = System.currentTimeMillis())
        inventoryItemDao.update(updatedItem)
        repositoryScope.launch { syncItemToCloud(id.toLong()) }
    }

    suspend fun getItemById(id: Int): InventoryItemEntity? =
        inventoryItemDao.getById(id)

    // --- Lógica de Sincronização ---

    fun startSync() {
        repositoryScope.launch {
            try {
                val householdId = getHouseholdId() ?: return@launch
                
                val localItems = inventoryItemDao.getAllActiveItemsWithProduct().first()
                localItems.forEach { 
                    if (it.item.remoteId == null) {
                        syncItemToCloud(it.item.id.toLong())
                    }
                }

                syncListener?.remove()
                syncListener = db.collection("households").document(householdId)
                    .collection("inventory")
                    .addSnapshotListener { snapshot, e ->
                        if (e != null || snapshot == null) return@addSnapshotListener
                        
                        repositoryScope.launch {
                            try {
                                snapshot.documentChanges.forEach { change ->
                                    val docId = change.document.id
                                    // Ignorar se formos nós a processar este ID agora
                                    if (beingProcessedRemoteIds.contains(docId)) return@forEach
                                    
                                    val remoteItem = change.document.toObject(RemoteInventoryItem::class.java).copy(id = docId)
                                    when (change.type) {
                                        com.google.firebase.firestore.DocumentChange.Type.ADDED,
                                        com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                                            handleRemoteChange(remoteItem, householdId)
                                        }
                                        com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                                            inventoryItemDao.deleteByRemoteId(remoteItem.id)
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

    private suspend fun syncItemToCloud(localId: Long) {
        try {
            val householdId = getHouseholdId() ?: return
            val itemWithProduct = inventoryItemDao.getWithProductById(localId.toInt()) ?: return
            
            val remoteItem = RemoteInventoryItem(
                productId = itemWithProduct.product.id,
                productName = itemWithProduct.product.name,
                productBrand = itemWithProduct.product.brand ?: "",
                productImageUrl = "",
                expiryDate = itemWithProduct.item.expiryDate,
                quantity = itemWithProduct.item.quantity,
                storageLocation = itemWithProduct.item.storageLocation,
                isConsumed = itemWithProduct.item.isConsumed,
                lastUpdated = itemWithProduct.item.lastUpdated // Usar sempre o timestamp local como fonte de verdade
            )

            val collection = db.collection("households").document(householdId).collection("inventory")
            
            if (itemWithProduct.item.remoteId != null) {
                beingProcessedRemoteIds.add(itemWithProduct.item.remoteId)
                collection.document(itemWithProduct.item.remoteId).set(remoteItem).await()
                beingProcessedRemoteIds.remove(itemWithProduct.item.remoteId)
            } else {
                val docRef = collection.add(remoteItem).await()
                beingProcessedRemoteIds.add(docRef.id)
                inventoryItemDao.updateRemoteId(localId.toInt(), docRef.id, householdId)
                // Pequeno delay para garantir que o Room atualizou antes de libertar o ID
                delay(500)
                beingProcessedRemoteIds.remove(docRef.id)
            }
        } catch (err: Exception) {
            err.printStackTrace()
        }
    }

    private suspend fun handleRemoteChange(remoteItem: RemoteInventoryItem, householdId: String) {
        syncMutex.withLock {
            try {
                var localProduct = productDao.getProductById(remoteItem.productId)
                if (localProduct == null) {
                    localProduct = ProductEntity(
                        id = remoteItem.productId,
                        name = remoteItem.productName,
                        brand = remoteItem.productBrand,
                        barcode = null
                    )
                    productDao.insert(localProduct)
                }

                // 1. Tentar encontrar pelo remoteId
                var existingItem = inventoryItemDao.getByRemoteId(remoteItem.id)
                
                // 2. Se não encontrar, tentar encontrar um item local "órfão" (sem remoteId) que seja idêntico
                if (existingItem == null) {
                    val orphanItems = inventoryItemDao.getAllActiveItems().first()
                    existingItem = orphanItems.find { 
                        it.remoteId == null && 
                        it.productId == remoteItem.productId && 
                        it.expiryDate == remoteItem.expiryDate &&
                        it.quantity == remoteItem.quantity
                    }
                    
                    // Se encontramos um órfão, vamos "batizá-lo" com o remoteId em vez de criar um novo
                    if (existingItem != null) {
                        inventoryItemDao.updateRemoteId(existingItem.id, remoteItem.id, householdId)
                    }
                }

                val newItem = InventoryItemEntity(
                    id = existingItem?.id ?: 0,
                    productId = remoteItem.productId,
                    expiryDate = remoteItem.expiryDate,
                    quantity = remoteItem.quantity,
                    storageLocation = remoteItem.storageLocation,
                    isConsumed = remoteItem.isConsumed,
                    remoteId = remoteItem.id,
                    householdId = householdId,
                    lastUpdated = remoteItem.lastUpdated
                )

                if (existingItem == null) {
                    inventoryItemDao.insert(newItem)
                } else if (remoteItem.lastUpdated > existingItem.lastUpdated) {
                    inventoryItemDao.update(newItem)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
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

    fun getTotalActiveCount(): Flow<Int> = inventoryItemDao.getTotalActiveCount()
    fun getTop5UrgentItemsWithProduct(): Flow<List<InventoryItemWithProduct>> = inventoryItemDao.getTop5UrgentItemsWithProduct()
}
