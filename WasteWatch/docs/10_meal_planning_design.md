# WasteWatch # 10 Meal Planning & Collaboration Design

Este documento detalha o desenho de arquitetura e lógica para a sincronização de utilizadores (Minha Casa vs. Grupos de Festa) e o Planeamento Semanal de Refeições com Integração de IA (Gemini).

---

## 1. Minha Casa (Household) vs. Grupos de Festa (Party Groups)

Para responder perfeitamente aos requisitos de colaboração, a aplicação separa dois conceitos fundamentais:

| Característica | **Minha Casa (Household)** | **Grupo de Festa (Party Group)** |
| :--- | :--- | :--- |
| **Objetivo** | Partilha diária e permanente de stock e compras entre pessoas que coabitam. | Colaboração pontual ou recorrente para festas/eventos entre pessoas com casas separadas. |
| **Privacidade** | **Totalmente Partilhado**. Ambos os membros veem e editam exatamente o mesmo inventário e lista de compras. | **Privado + Coletivo**. Cada membro mantém o seu stock privado. O grupo combina temporariamente a soma dos stocks para receitas. |
| **Planeamento** | Planeador Semanal Partilhado (atualizado em tempo real para toda a casa). | Planeador do Evento (ex: Churrasco de sábado com contribuições individuais). |
| **Identificador** | Um único `householdId` no perfil do utilizador. | Uma lista `groupIds` (um utilizador pode pertencer a vários grupos). |

---

## 2. Como duas pessoas na mesma casa partilham o Stock e Receitas?

### A. Sincronização em Tempo Real (Room + Firestore)
Esta funcionalidade baseia-se na sincronização bidirecional que já está parcialmente implementada no `InventoryRepository`:

- **Gravação Local com Sincronização Automática**: Quando o Utilizador A consome ou adiciona um produto, o Room local é atualizado imediatamente (com um timestamp `lastUpdated = System.currentTimeMillis()`) e um Job em background ou Coroutine assíncrona envia a atualização para o Firestore em `households/{householdId}/inventory/{remoteId}`.
- **Deteção Remota por Snapshot Listener**: O Utilizador B tem um listener ativo no Firestore (`addSnapshotListener`) que monitoriza a mesma coleção `households/{householdId}/inventory`. Ao detetar a mudança, o listener atualiza localmente o Room do Utilizador B se a alteração remota for mais recente (`remoteLastUpdated > localLastUpdated`).
- **Acesso Offline Resiliente**: Como os dados são lidos e gravados localmente no Room e depois sincronizados assincronamente com o Firestore, a app continua a funcionar offline, reconciliando-se quando a rede volta.

### B. Receitas Partilhadas
1. Como o inventário Room local de ambos está em sintonia, quando qualquer um deles clica em **"Receitas"**, a lista de ingredientes ativa gerada a partir do Room local é idêntica.
2. A chamada ao **Gemini AI** (`GeminiService.getRecipesForIngredients(ingredients)`) receberá a mesma lista de ingredientes disponíveis, gerando receitas contextualizadas com o stock real da casa.

---

## 3. Gestão de Convites (Invite System)

O sistema de convites permite que os utilizadores se conectem de forma extremamente simples através de um código alfanumérico curto de 6 caracteres (gerado a partir de um UUID).

### A. Convite para "Minha Casa" (Household)
1. **Criar Casa**: O Utilizador A clica em "Gerar Código de Convite".
   - É criado um documento em `/households/{householdId}` com `inviteCode = "WWX7Z9"` e `members = ["UserA_UID"]`.
   - O `householdId` do Utilizador A em `/users/UserA_UID` é atualizado.
2. **Aderir à Casa**: O Utilizador B clica em "Aderir a uma Casa" e insere `"WWX7Z9"`.
   - A app pesquisa no Firestore por `inviteCode == "WWX7Z9"`.
   - Se encontrado, adiciona `"UserB_UID"` à lista `members` do documento da casa.
   - Atualiza o `householdId` do Utilizador B para o mesmo da casa.
   - **Gatilho de Sincronização**: O `InventoryRepository` inicia o `startSync()`, descarregando todo o stock partilhado para o Room do Utilizador B.

### B. Convite para "Grupos de Festa" (Party Groups)
1. **Criar Grupo**: O Utilizador A clica em "Criar Novo Grupo", dá um nome (ex: "Churrasco de Fim de Ano") e gera o convite.
   - Cria um documento em `/groups/{groupId}`:
     ```json
     {
       "id": "groupId_123",
       "name": "Churrasco de Fim de Ano",
       "inviteCode": "GRP88F",
       "members": ["UserA_UID"],
       "createdBy": "UserA_UID"
     }
     ```
2. **Aderir ao Grupo**: O Utilizador B insere `"GRP88F"`.
   - A app valida e insere `"UserB_UID"` na lista `members` de `/groups/groupId_123`.
   - Adiciona `"groupId_123"` à lista `groupIds` de `/users/UserB_UID`.

---

## 4. Planeamento Semanal de Refeições (Meal Planner)

O ecrã de planeamento alimentar será organizado como uma grelha estruturada de 7 dias com 3 refeições diárias: **Matabixo**, **Almoço** e **Jantar**.

### A. Estrutura de Dados Local (MealPlanEntity)
Para permitir o funcionamento offline (conforme as diretrizes), persistimos o planeamento no Room:

```kotlin
@Entity(
    tableName = "meal_plans"
)
data class MealPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val weekStartDate: Long, // Timestamp da Segunda-feira correspondente à semana do plano
    val dayOfWeek: String,    // "SEGUNDA", "TERCA", "QUARTA", "QUINTA", "SEXTA", "SABADO", "DOMINGO"
    val mealType: String,     // "MATABIXO", "ALMOCO", "JANTAR"
    val recipeName: String,   // Nome do prato/receita
    val ingredients: String,  // JSON com os ingredientes necessários (armazenado para leitura offline)
    val instructions: String?,// JSON ou texto com o modo de preparação
    val isDone: Boolean = false, // Se a refeição já foi consumida/confecionada
    val remoteId: String? = null,
    val householdId: String? = null, // Define se pertence ao stock partilhado da casa
    val groupId: String? = null,     // Define se pertence a um evento/grupo de festa
    val lastUpdated: Long = System.currentTimeMillis()
)
```

### B. Fluxo de Interação e Sugestões da IA (Gemini)

O fluxo foi desenhado para ser intuitivo e reativo ao stock:

1. **Abertura do Plano**: O Utilizador visualiza a grelha de Segunda a Domingo com slots de Matabixo, Almoço e Jantar.
2. **Seleção de Slot Vazio**: Ao clicar num slot vazio (ex: "Sábado Matabixo"), é exibido um diálogo de opções:
   - *Adicionar Manualmente*: Permite introduzir o nome de um prato caseiro simples.
   - *Sugerir com IA (Gemini)*: Solicita sugestões inteligentes à IA personalizadas para aquele slot.
3. **Chamada Inteligente ao Gemini**:
   - A app recolhe todos os ingredientes do inventário local Room.
   - Dispara uma consulta ao `GeminiService` indicando o tipo de refeição selecionado (Matabixo, Almoço ou Jantar).
   - O Gemini gera 3 sugestões baseadas no stock atual e na categoria da refeição.
4. **Carrossel de Escolha**: As 3 sugestões aparecem num carrossel estilizado com tempo de preparação e ingredientes em falta. O utilizador escolhe uma, que preenche o slot e grava localmente (Room) e sincroniza na cloud (Firestore).

### C. Prompt Especializado para Gemini Meal Planning
A chamada ao `GeminiService` será enriquecida para aceitar o slot específico (ex: Matabixo):

```kotlin
suspend fun getMealSuggestions(
    ingredients: List<String>, 
    mealType: String // "MATABIXO" | "ALMOCO" | "JANTAR"
): List<RecipeAi>
```

**Prompt enviado ao Gemini:**
> "Sou um assistente de cozinha inteligente doméstico.
> Tenho em stock os seguintes ingredientes: [Ingredientes].
> Sugere 3 opções de receitas deliciosas adequadas especificamente para a refeição **[mealType]** (Matabixo = pequeno-almoço/pequenas refeições; Almoço = refeição principal; Jantar = refeição leve ou de prato principal).
> Prioriza o uso dos ingredientes listados para reduzir o desperdício alimentar."

---

## 5. Como o Planeamento em Grupo usa o Stock de Todos?

No **Planeamento de Grupo (Eventos/Festas)**, a lógica de ingredientes muda para permitir a cooperação sem violar a privacidade dos stocks privados de cada casa:

1. **Obtenção dos Stocks**:
   - Ao abrir o plano de um **Grupo de Festa**, a app obtém a lista de membros do grupo.
   - Para cada membro, faz uma consulta rápida no Firestore para buscar a sua lista de ingredientes ativos `/households/{memberHouseholdId}/inventory`.
2. **Combinação de Ingredientes**:
   - A app soma logicamente os stocks individuais:
     * *Casa do João*: Frango (1x), Batatas (5x)
     * *Casa da Maria*: Cerveja (6x), Natas (2x)
     * *Stock Coletivo*: Frango (1x), Batatas (5x), Cerveja (6x), Natas (2x)
3. **Chamada ao Gemini Coletiva**:
   - Envia a lista combinada ao Gemini: "Sugere receitas para a nossa festa usando: Frango, Batatas, Cerveja, Natas."
   - O Gemini sugere, por exemplo: "Frango com Natas e Batatas Assadas".
4. **Divisão de Contribuições**:
   - No planeador do grupo, a receita mostra quem tem cada ingrediente:
     * 🍗 Frango (Trazido por João)
     * 🥔 Batatas (Trazido por João)
     * 🥛 Natas (Trazido por Maria)
   - Quando o evento é concluído, o João clica em "Consumir no meu stock", o que desconta os seus ingredientes localmente (que depois sincroniza com a sua casa).

---

## 6. Próximos Passos de Implementação (Fase 6)

Seguindo rigorosamente o `docs/08_implementation_plan.md`, avançamos passo a passo:

1. **Adicionar Extensão 5** em `docs/09_feature_extensions.md` registando o Planeador Alimentar e Partilha em Grupo.
2. **Passo 24**: Implementar a partilha total de stock e compras da "Minha Casa" (Household) usando a sincronização em tempo real (já estruturada).
3. **Passo 25**: Implementar "Grupos de Festa" no Firestore e repositórios para leitura de stocks combinados.
4. **Passo 26**: Criar a entidade Room `MealPlanEntity`, o respetivo DAO, repositório de planeamento, e atualizar o `GeminiService` para sugestões focadas no tipo de refeição.
5. **UI & Navigation**: Substituir o placeholder do `meal_plan` por uma UI Jetpack Compose premium, viva, e responsiva com micro-animações, conforme as diretrizes visuais.
