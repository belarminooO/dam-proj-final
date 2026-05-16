# WasteWatch – Modelo de Dados

## Product

```
id:          Int      (Primary Key, auto-increment)
name:        String   (nome do produto, ex: "Alface")
brand:       String?  (marca, ex: "Horta Nova")
barcode:     String?  (código de barras EAN/UPC)
category_id: Int      (Foreign Key → Category)
```

---

## InventoryItem

```
id:               Int      (Primary Key, auto-increment)
product_id:       Int      (Foreign Key → Product)
expiry_date:      Long     (timestamp da data de expiração)
quantity:         Int      (quantidade disponível)
storage_location: String   ("Frigorífico" | "Congelador" | "Despensa")
added_date:       Long     (timestamp de quando foi adicionado)
is_consumed:      Boolean  (true se foi marcado como consumido)
```

---

## Category

```
id:   Int    (Primary Key, auto-increment)
name: String (ex: "Laticínios", "Carne", "Vegetais", "Frutas", "Padaria")
```

---

## ShoppingItem

```
id:           Int     (Primary Key, auto-increment)
name:         String  (nome do produto a comprar)
quantity:     Int     (quantidade desejada)
is_purchased: Boolean (true se já foi comprado)
```

---

## Relações

```
Category (1) ──── (N) Product (1) ──── (N) InventoryItem
```

- Uma Category tem vários Products
- Um Product pode ter vários InventoryItems (ex: dois pacotes de leite com datas diferentes)
- Um InventoryItem pertence a um único Product

---

## Lógica de Urgência

```
days_remaining = expiry_date - today

days_remaining < 0   →  "Expirou há X dias"   → cor: Vermelho
days_remaining == 0  →  "Expira hoje!"         → cor: Vermelho
days_remaining == 1  →  "Expira amanhã"        → cor: Amarelo
days_remaining <= 3  →  "Expira em X dias"     → cor: Amarelo
days_remaining > 3   →  dentro da validade     → cor: Verde
```

---

## Implementação Room

- `ProductEntity` + `ProductDao`
- `InventoryItemEntity` + `InventoryItemDao`
- `CategoryEntity` + `CategoryDao`
- `ShoppingItemEntity` + `ShoppingItemDao`
- `AppDatabase` — classe Room principal com todas as entidades
