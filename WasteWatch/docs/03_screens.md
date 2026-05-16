# WasteWatch – Ecrãs da Aplicação

## Screen: Home / Dashboard

**Components:**
- Header com título "WasteWatch" e subtítulo "Gerencie seu inventário e evite desperdício"
- Card: Expiram Hoje (contador numérico)
- Card: Urgentes ≤3 dias (contador numérico)
- Card: Total de Itens (contador numérico)
- Secção "Top 5 – Itens Mais Urgentes" com botão "Ver Todos"
- InventoryItemCard (nome, marca, localização, badge de urgência, data de validade, quantidade)
- Bottom Navigation Bar

**Actions:**
- Navegar para Inventário completo ("Ver Todos")
- Aceder à **Central Social / Perfil** (ícone no topo)
- Navegar para qualquer ecrã via Bottom Nav

---

## Screen: Inventory Screen

**Components:**
- Top bar com título "Inventário" e botão de retroceder
- Search bar ("Pesquisar produtos...")
- Dropdown de categorias ("Todas as Categorias")
- Botão de filtro de local ("Local")
- Contador de itens (ex: "15 itens")
- LazyColumn com InventoryItemCards:
  - Ícone do produto
  - Nome e marca
  - ExpiryBadge (ex: "Expirou há 2 dias", "Expira hoje!", "Expira amanhã")
  - Tags: categoria, localização, quantidade
  - Data de validade
  - Botão "✓ Consumir"
  - Botão de eliminar (ícone de lixo)

**Actions:**
- Pesquisar produto por nome
- Filtrar por categoria ou local
- Marcar produto como consumido
- Eliminar produto do inventário
- Navegar para detalhe do produto

---

## Screen: Scanner Screen

**Components:**
- Top bar com título "Adicionar Produto" e botão de retroceder
- Área de preview da câmara (CameraX)
- Instrução: "Escanear Código de Barras – Aponte a câmara para o código de barras do produto"
- Botão "Iniciar Scanner"
- Secção alternativa: "Não consegue escanear o código?"
- Botão "Adicionar Manualmente"

**Actions:**
- Ativar câmara e iniciar leitura de código de barras (ML Kit)
- Em scan bem-sucedido → navegar para Add Product Screen com dados pré-preenchidos
- Em scan falhado → navegar para Add Product Screen (modo manual)

---

## Screen: Add Product Screen

**Components:**
- Top bar com título "Adicionar Produto" e botão de retroceder
- Campo: Nome do produto (TextField)
- Campo: Marca (TextField)
- Dropdown: Categoria (Laticínios, Carne, Vegetais, Frutas, Padaria, etc.)
- Dropdown: Local de armazenamento (Frigorífico, Congelador, Despensa)
- Campo: Quantidade (NumberField)
- Campo: Data de validade (DatePicker)
- Campo: Código de barras (TextField, pré-preenchido se veio do scanner)
- Botão "Guardar"
- Botão "Cancelar"

**Actions:**
- Guardar produto no inventário (Room)
- Cancelar e voltar ao ecrã anterior

---

## Screen: Product Detail Screen

**Components:**
- Top bar com título do produto e botão de retroceder
- Nome, marca, categoria
- ExpiryBadge com data de validade
- Quantidade e local de armazenamento
- Código de barras (se disponível)
- Botão "✓ Marcar como Consumido"
- Botão "Editar"
- Botão "Eliminar"

**Actions:**
- Marcar produto como consumido
- Editar campos do produto
- Eliminar produto

---

## Screen: Recipes Screen

**Components:**
- Top bar com título "Receitas"
- Subtítulo com ingredientes mais urgentes em destaque
- LazyColumn com RecipeCards:
  - Nome da receita
  - Ingredientes em stock utilizados (destacados)
  - Ingredientes em falta
- Indicação: "Usa X ingredientes que expiram em breve"

**Actions:**
- Navegar para detalhe de receita
- Ver ingredientes utilizados do inventário

---

## Screen: Shopping List Screen

**Components:**
- Top bar com título "Lista de Compras"
- LazyColumn com itens:
  - Checkbox por item
  - Nome do produto
  - Quantidade
- Botão "Adicionar Item"
- Botão "Limpar Lista"

**Actions:**
- Marcar item como comprado (checkbox)
- Adicionar item manualmente
- Limpar toda a lista

---

---

## Screen: Login Screen

**Components:**
- Ilustração WasteWatch
- Campo Email
- Campo Password
- Botão "Entrar"
- Link "Não tem conta? Registe-se"

**Actions:**
- Autenticação via Firebase
- Navegação para Dashboard após sucesso

---

## Screen: Register Screen

**Components:**
- Campo Nome
- Campo Email
- Campo Password
- Botão "Criar Conta"
- Link "Já tem conta? Faça Login"

---

## Screen: Social Hub / Profile Screen

**Components:**
- Nome e Email do utilizador
- Secção "Minha Casa" (Household):
    - Botão "Gerar Código de Convite"
    - Botão "Aderir a uma Casa" (Input de código)
    - Lista de membros da casa
- Secção "Grupos de Festa" (Social Groups):
    - Botão "Criar Novo Grupo"
    - Lista de grupos ativos
- Botão "Terminar Sessão" (Logout)

---

## Screen: Weekly Meal Planning Screen

**Components:**
- Seletor de Semana
- Grelha Vertical (Segunda a Domingo)
- Slots por dia: Matabixo, Almoço, Jantar
- Cada slot vazio tem botão "Sugerir (IA)"
- Slots preenchidos mostram o nome da receita e botão "Ver Receita"

---

## Bottom Navigation Bar (Atualizada)

| Label | Rota | Ícone |
|-------|------|-------|
| Home | `home` | Home |
| Inventário | `inventory` | ListAlt |
| Plano | `meal_plan` | EventNote |
| Receitas | `recipes` | Restaurant |
| Compras | `shopping` | ShoppingCart |
