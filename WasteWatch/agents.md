# AI Agent Guidelines – WasteWatch

The AI agent must follow a planning-first approach.

## Rules

- Always read all documentation inside `/docs` before generating any code.
- Follow the architecture defined in `docs/06_architecture.md`.
- Generate Kotlin code only.
- UI must use **Jetpack Compose**.
- Do not generate large files at once.
- Generate code step-by-step following the implementation plan in `docs/08_implementation_plan.md`.
- When implementing a new screen, always check `docs/03_screens.md` first.
- When creating data classes or DAOs, always follow `docs/04_data_model.md`.
- When adding navigation, always follow `docs/05_navigation.md`.
- When calling external APIs, always follow `docs/07_api_usage.md`.
- Do not skip steps in the implementation plan.
- After each step, wait for confirmation before proceeding to the next.
- If a problem occurs, do not guess — ask for clarification or re-read the relevant doc file.
- New features not in the original plan must only be implemented after being added to `docs/09_feature_extensions.md`.

## Stack

- Language: Kotlin
- UI: Jetpack Compose
- Architecture: MVVM
- Database: Room
- Dependency Injection: Hilt
- Navigation: Navigation Compose
- Async: Coroutines + Flow
- HTTP: Retrofit
- Camera / Scan: CameraX + ML Kit Barcode Scanning
- Background Work: WorkManager

## Behavior

This file ensures the AI behaves as a disciplined development assistant — structured, incremental, and always aligned with the documented specifications.
