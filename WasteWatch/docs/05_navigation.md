# WasteWatch – Navegação

## Estrutura de Navegação

```
MainActivity (NavHost)
│
├── HomeScreen  ← start destination
│   ├── → InventoryScreen  (botão "Ver Todos")
│   └── → ProductDetailScreen  (clique num item urgente)
│
├── InventoryScreen
│   ├── → ProductDetailScreen  (clique num item)
│   └── → AddProductScreen  (FAB adicionar)
│
├── ScannerScreen
│   ├── → AddProductScreen  (scan bem-sucedido, dados pré-preenchidos)
│   └── → AddProductScreen  (opção "Adicionar Manualmente")
│
├── RecipesScreen
│   └── → RecipeDetailScreen  (clique numa receita)
│
├── ShoppingListScreen
│
├── ProductDetailScreen
│   └── ← back → InventoryScreen / HomeScreen
│
└── AddProductScreen
    └── ← back / save → InventoryScreen
```

---

## Bottom Navigation Bar

Visível em todos os ecrãs principais.

```
HomeScreen → InventoryScreen → ScannerScreen → RecipesScreen → ShoppingListScreen
```

---

## Rotas

```kotlin
object Routes {
    const val HOME            = "home"
    const val INVENTORY       = "inventory"
    const val SCANNER         = "scanner"
    const val RECIPES         = "recipes"
    const val SHOPPING        = "shopping"
    const val PRODUCT_DETAIL  = "product_detail/{itemId}"
    const val ADD_PRODUCT     = "add_product?barcode={barcode}"
}
```

---

## Argumentos de Navegação

- `ProductDetailScreen` recebe `itemId: Int`
- `AddProductScreen` recebe `barcode: String?` (opcional, vindo do scanner)
