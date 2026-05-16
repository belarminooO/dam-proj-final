# WasteWatch – Navegação

## Estrutura de Navegação

```
MainActivity (NavHost)
│
├── AuthFlow
│   ├── LoginScreen  ← start destination (if not logged in)
│   └── RegisterScreen
│
├── MainFlow (Bottom Nav)
│   ├── HomeScreen
│   │   └── → SocialHubScreen (ícone perfil no TopBar)
│   ├── InventoryScreen
│   │   └── → AddProductScreen (FAB)
│   ├── MealPlanScreen (Calendário Semanal)
│   ├── RecipesScreen (IA Suggestions)
│   └── ShoppingListScreen
│
├── ProductDetailScreen
├── AddProductScreen
└── RecipeDetailScreen
```

---

## Bottom Navigation Bar

Visível em todos os ecrãs principais.

```
HomeScreen → InventoryScreen → MealPlanScreen → RecipesScreen → ShoppingListScreen
```

---

## Rotas

```kotlin
object Routes {
    const val LOGIN           = "login"
    const val REGISTER        = "register"
    const val HOME            = "home"
    const val SOCIAL_HUB      = "social_hub"
    const val INVENTORY       = "inventory"
    const val MEAL_PLAN       = "meal_plan"
    const val RECIPES         = "recipes"
    const val SHOPPING        = "shopping"
    const val PRODUCT_DETAIL  = "product_detail/{itemId}"
    const val ADD_PRODUCT     = "add_product?barcode={barcode}"
}
```

---

## Argumentos de Navegação

- `ProductDetailScreen` recebe `itemId: Int`
- `AddProductScreen` recebe `barcode: String?` (opcional)
