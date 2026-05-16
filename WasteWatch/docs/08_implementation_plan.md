# WasteWatch – Implementation Plan

AntiGravity should follow this plan when generating code. Execute one step at a time and wait for confirmation before proceeding.

---

## Step 1
Create Android project with Kotlin and Jetpack Compose. Configure `build.gradle` with all required dependencies (Room, Hilt, Navigation Compose, Retrofit, CameraX, ML Kit, WorkManager, Coroutines).

## Step 2
Create `Category` data class and `CategoryEntity` with Room annotations. Create `CategoryDao` with query to get all categories. Seed initial categories: Laticínios, Carne, Vegetais, Frutas, Padaria, Bebidas, Congelados, Outros.

## Step 3
Create `Product` data class and `ProductEntity`. Create `ProductDao` with insert, delete, and get by barcode queries.

## Step 4
Create `InventoryItem` data class and `InventoryItemEntity`. Create `InventoryItemDao` with:
- insert, update, delete
- get all items ordered by expiry date
- get items expiring within N days
- get items by category and storage location

## Step 5
Create `ShoppingItem` data class and `ShoppingItemEntity`. Create `ShoppingItemDao` with insert, delete, get all, and mark as purchased.

## Step 6
Create `AppDatabase` with Room, including all entities and DAOs. Configure `AppModule` with Hilt to provide database and DAO instances.

## Step 7
Create `InventoryRepository` and `ProductRepository` with methods that wrap DAO calls and expose `Flow`.

## Step 8
Create `HomeViewModel` that exposes:
- count of items expiring today
- count of urgent items (≤3 days)
- total item count
- top 5 most urgent items

## Step 9
Design and implement `HomeScreen` composable with:
- header with app name
- three summary cards (Expiram Hoje, Urgentes, Total)
- Top 5 list with `InventoryItemCard` composable
- `ExpiryBadge` composable (green / yellow / red)

## Step 10
Create `InventoryViewModel` with StateFlow for item list, search query, and category filter.

## Step 11
Design and implement `InventoryScreen` composable with search bar, category dropdown, filter button, item count, and LazyColumn of `InventoryItemCard` with Consumir and delete actions.

## Step 12
Configure `NavHost` in `MainActivity` with all routes defined in `docs/05_navigation.md`. Implement `BottomNavBar` composable.

## Step 13
Implement `AddProductScreen` composable with all form fields (name, brand, category dropdown, storage location dropdown, quantity, date picker, barcode). Create `AddProductViewModel` to handle save logic.

## Step 14
Integrate Retrofit with Open Food Facts API. Create `OpenFoodFactsApi` interface and `ProductRemoteDataSource`.

## Step 15
Implement `ScannerScreen` with CameraX preview and ML Kit Barcode Scanning. On successful scan, call Open Food Facts API and navigate to `AddProductScreen` with pre-filled data.

## Step 16
Implement `ProductDetailScreen` with item details, consume, edit, and delete actions.

## Step 17
Create `ExpiryNotificationWorker` with WorkManager. Schedule daily check for items expiring within 2 days. Send one notification per urgent item found. Request `POST_NOTIFICATIONS` permission on Android 13+.

## Step 18
Create local recipes database (JSON asset or Room). Implement `RecipesViewModel` with ingredient matching logic — prioritize recipes that use items expiring soonest. Implement `RecipesScreen` with recipe list.

## Step 19
Create `ShoppingRepository`. Implement `ShoppingListScreen` with checklist. Wire "Consumir" action in inventory to prompt adding the item to the shopping list.

## Step 20
Handle all error and empty states across all screens (empty inventory, API error on scan, no recipes matched). Add loading indicators where data is being fetched. Review UI consistency across all screens.

