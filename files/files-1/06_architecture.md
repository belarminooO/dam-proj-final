# WasteWatch – Arquitetura

## Architecture: MVVM

```
Layers:
UI (Composables) → ViewModel → Repository → Room Database / API Service
```

---

## Camadas

**UI Layer**
- Composables (Screens) observam `StateFlow` expostos pelos ViewModels
- Sem lógica de negócio — apenas renderização e eventos de utilizador

**ViewModel Layer**
- `HomeViewModel` — queries de resumo (expiram hoje, urgentes, total)
- `InventoryViewModel` — lista de itens, filtros, pesquisa, consumir/eliminar
- `ScannerViewModel` — chamada à API Open Food Facts por código de barras
- `AddProductViewModel` — guardar produto no inventário
- `RecipesViewModel` — matching de receitas com ingredientes em stock
- `ShoppingViewModel` — gestão da lista de compras

**Repository Layer**
- `InventoryRepository` — acesso a `InventoryItemDao` e `ProductDao`
- `ProductRepository` — lookup de produtos e chamadas à Open Food Facts API
- `ShoppingRepository` — acesso a `ShoppingItemDao`

**Data Layer**
- Room Database: `AppDatabase` com todas as entidades e DAOs
- Retrofit: cliente HTTP para Open Food Facts API

---

## Package Structure

```
com.wastewatch/
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt
│   │   ├── dao/
│   │   └── entity/
│   ├── remote/
│   │   └── OpenFoodFactsApi.kt
│   └── repository/
├── domain/
│   └── model/
├── ui/
│   ├── home/
│   ├── inventory/
│   ├── scanner/
│   ├── addproduct/
│   ├── recipes/
│   ├── shopping/
│   ├── components/
│   └── theme/
├── notifications/
│   └── ExpiryNotificationWorker.kt
├── di/
│   └── AppModule.kt
└── MainActivity.kt
```

---

## State Management

Cada ViewModel expõe um `UiState` via `StateFlow`:

```kotlin
data class InventoryUiState(
    val items: List<InventoryItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val filterCategory: String? = null,
    val searchQuery: String = ""
)
```

---

## Background Work

- Notificações implementadas com `WorkManager` (`PeriodicWorkRequest`, intervalo diário)
- `ExpiryNotificationWorker` verifica itens a expirar nos próximos 2 dias e envia `NotificationCompat`
