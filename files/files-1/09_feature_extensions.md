# WasteWatch – Feature Extensions

Extensões identificadas após a conclusão do plano de implementação base (`08_implementation_plan.md`). Cada nova funcionalidade é documentada aqui antes de ser implementada.

---

## Extension 1 – Loading Indicator

**Description:**
Display a loading indicator (circular progress bar) whenever data is being fetched — on app startup, when loading the inventory, and while the scanner queries the Open Food Facts API.

**Implementation tasks:**
- Add `isLoading: Boolean` field to all UiState classes
- Show `CircularProgressIndicator` composable when `isLoading == true` in HomeScreen, InventoryScreen, and ScannerScreen
- Set `isLoading = true` before async operations, `false` on completion or error

**Expected UI changes:**
- Centered progress spinner visible during data loading
- Spinner replaces content area until data is ready

---

## Extension 2 – Favorite Items (FIFO queue, max 5)

**Description:**
Allow the user to mark inventory items as favorites. Favorites are stored in a FIFO queue with a maximum capacity of 5. The user has direct access to the 5 favorite items from any screen via their product images/icons.

**Implementation tasks:**
- Add `is_favorite: Boolean` and `favorite_added_at: Long` fields to `InventoryItemEntity`
- Implement FIFO logic: when a 6th item is marked as favorite, remove the oldest
- Add favorite toggle button to `InventoryItemCard` and `ProductDetailScreen`
- Add persistent favorites strip/row visible on all main screens (above Bottom Nav or in HomeScreen)
- Each favorite shown as a product icon/image; tap navigates to `ProductDetailScreen`

**Expected UI changes:**
- Star/heart icon on each inventory item card
- Horizontal row of up to 5 favorite product icons accessible from all screens

---

## Extension 3 – Offline Access

**Description:**
The app must remain functional without internet access. All inventory data is stored locally in Room. The scanner should handle API failures gracefully and allow manual entry.

**Implementation tasks:**
- Ensure all screens read from Room (already local — verify no screen requires network to display)
- On scanner API failure (no connection), show message: "Sem ligação à internet. Adicione o produto manualmente." and navigate to `AddProductScreen`
- Verify WorkManager notifications work offline (query is local)

**Expected UI changes:**
- Error banner or snackbar on API failure with option to add manually
- No crashes or blank screens when offline

---

## Extension 4 – Graceful API Error Handling

**Description:**
Handle all API errors (network timeout, 404, server error) gracefully across the app without crashing.

**Implementation tasks:**
- Wrap all Retrofit calls in try/catch (already in `07_api_usage.md`)
- Show user-friendly error messages via Snackbar or error state in UI
- Distinguish between "product not found" and "no internet" errors
- Add retry option where applicable

**Expected UI changes:**
- Snackbar with error message on API failure
- "Tentar novamente" button on persistent errors

---

*(Adicionar novas extensões aqui conforme forem identificadas durante o desenvolvimento)*
