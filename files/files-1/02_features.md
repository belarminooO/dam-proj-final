# WasteWatch – Funcionalidades

## Funcionalidades Principais

### 1. Inventário Digital
Listagem organizada de todos os produtos alimentares existentes em casa, categorizados por tipo (ex: Frigorífico, Congelador, Despensa). Permite ao utilizador saber o que tem em casa sem precisar de abrir armários ou o frigorífico.

- Visualização por categoria e por localização
- Filtros por categoria e local de armazenamento
- Pesquisa por nome de produto
- Contador total de itens e de itens urgentes

### 2. Scanner de Código de Barras
Utiliza a câmara do dispositivo para identificar produtos rapidamente no momento das compras. Ao escanear o código de barras, a aplicação preenche automaticamente o nome, marca e, quando disponível, a data de validade.

- Leitura de código de barras via câmara
- Preenchimento automático de nome e marca
- Opção de adicionar manualmente caso o scan falhe

### 3. Gestão de Validades
Registo da data de expiração de cada item no inventário, com indicadores visuais de urgência por cores:

- 🟢 **Verde** – produto dentro da validade (mais de 3 dias)
- 🟡 **Amarelo** – expira em breve (≤ 3 dias)
- 🔴 **Vermelho** – expirou ou expira hoje

### 4. Sistema de Alertas Inteligentes
Envio de notificações automáticas para avisar o utilizador que um produto está prestes a expirar:

- Notificação 2 dias antes da expiração
- Notificação no próprio dia de expiração
- Configurável pelo utilizador (ativar/desativar)

### 5. Matching de Receitas
Sugestão de receitas baseadas nos ingredientes que o utilizador tem em stock, priorizando os que expiram mais cedo.

- Receitas geradas com base no inventário atual
- Priorização de ingredientes próximos do fim da validade
- Incentivo ao consumo antes do desperdício

### 6. Lista de Compras Automática
Quando um produto é marcado como "consumido" ou removido do inventário, pode ser adicionado automaticamente a uma lista de compras para a próxima ida ao supermercado.

- Geração automática de lista de compras
- Marcação de itens como consumidos
- Gestão manual da lista

## Funcionalidades Secundárias

- Ecrã de Dashboard com resumo (expiram hoje, urgentes, total de itens)
- Top 5 itens mais urgentes visível na página inicial
- Navegação entre secções via bottom navigation bar
