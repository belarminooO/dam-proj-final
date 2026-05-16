# WasteWatch 🥗

> Gerencie o seu inventário alimentar e evite desperdício.

**Estudante:** Belarmino Sacate | **Nº:** 52057 | **UC:** DAM 2025–2026

---

## Descrição

WasteWatch é uma aplicação Android de gestão de inventário alimentar doméstico. Permite registar produtos, monitorizar datas de validade, receber alertas antes que os alimentos expirem e obter sugestões de receitas com os ingredientes disponíveis em casa.

---

## API Utilizada

**Open Food Facts API**
- URL base: `https://world.openfoodfacts.org`
- Endpoint: `GET /api/v0/product/{barcode}.json`
- Utilização: identificação automática de produtos por código de barras (nome, marca, categoria)
- Autenticação: não requerida (API pública e gratuita)

---

## Funcionalidades

- **Inventário Digital** – lista organizada de produtos por categoria e localização (Frigorífico, Congelador, Despensa)
- **Scanner de Código de Barras** – adição rápida de produtos via câmara com preenchimento automático
- **Gestão de Validades** – indicadores visuais por cores (verde / amarelo / vermelho)
- **Alertas Inteligentes** – notificações automáticas 2 dias antes da expiração
- **Sugestão de Receitas** – receitas baseadas nos ingredientes em stock, priorizando os mais urgentes
- **Lista de Compras** – gerada automaticamente ao marcar produtos como consumidos

---

## Screenshots

> *(Adicionar screenshots após desenvolvimento)*

| Home / Dashboard | Inventário | Scanner |
|:---:|:---:|:---:|
| *(screenshot)* | *(screenshot)* | *(screenshot)* |

---

## Estrutura do Repositório

```
project-root/
├── app/                          # Código fonte Android
├── docs/
│   ├── 01_overview.md            # Visão geral do projeto
│   ├── 02_features.md            # Funcionalidades detalhadas
│   ├── 03_screens.md             # Descrição de ecrãs e UI
│   ├── 04_data_model.md          # Modelo de dados
│   ├── 05_navigation.md          # Estrutura de navegação
│   ├── 06_architecture.md        # Arquitetura MVVM
│   ├── 07_api_usage.md           # APIs externas utilizadas
│   ├── 08_implementation_plan.md # Plano de desenvolvimento
│   ├── 09_feature_extensions.md  # Extensões pós-plano
│   └── prompts_log.md            # Registo de prompts de IA
├── agents.md                     # Regras para o agente AntiGravity
└── README.md                     # Este ficheiro
```

---

## Como Executar

1. Clonar o repositório
2. Abrir o projeto no **AntiGravity IDE** (ou Android Studio Hedgehog+)
3. Sincronizar dependências Gradle
4. Usar o AntiGravity para fazer build → deploy → run num emulador ou dispositivo físico com Android 8.0 (API 26)+

### Verificar após execução

- Os produtos são listados corretamente no inventário
- Os badges de urgência aparecem com as cores corretas
- O scanner ativa a câmara e identifica produtos
- As notificações são enviadas para produtos a expirar
- A navegação entre ecrãs funciona corretamente

---

## Documentação

Consultar a pasta [`docs/`](./docs/) para documentação detalhada de cada componente do projeto.
