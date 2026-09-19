# AGENTS.md — OpenReader Protocol

## Principios Globales
1. **Fidelidad OpenSpec:** Revisa `.openspec/openreader-spec.yaml` antes de implementar cualquier feature. No agregues librerías no documentadas.
2. **Offline-First & Open Source:** Prohibido agregar SDKs de analítica, Firebase, tracking o APIs de pago.
3. **Optimización de RAM:** El procesamiento de PDF y la cola TTS ($N+1$) no deben almacenar cadenas masivas en memoria. Usa `Flow` y `Channels`.

## Reglas de Código Kotlin & Jetpack Compose
- **UI:** Exclusivamente Jetpack Compose. No usar XML Layouts.
- **State Management:** Usa `StateFlow` y `ViewModel`. Los componibles deben ser apátridas (Stateless) usando elevación de estado (State Hoisting).
- **Adaptabilidad:** Soporta `WindowSizeClass` (`Compact` vs `Expanded`). No hardcodear anchos fijos en `dp`; usa `Modifier.widthIn(max = 48.rem)` para pantallas anchas.
- **Corrutinas:** Especifica siempre el `CoroutineDispatcher` explícito (`Dispatchers.IO` para I/O o `Dispatchers.Default` para procesamiento CPU).

## Protocolo de Ahorro de Tokens
- Genera soluciones incrementales y modulares por archivo.
- No reimplementes archivos enteros si solo se modifica una función; usa diffs claros o fragmentos con comentarios `// ... rest of the code ...`.
- Mantén las respuestas técnicas directas sin resúmenes introductorios innecesarios.
