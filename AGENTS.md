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

## Reglas de Documentación y Diagramado con D2
1. **Estándar de Diagramas:** Usa **D2 Language** (`.d2`) para documentar arquitectura, flujos de datos y máquinas de estado. No uses Mermaid ni PlantUML.
2. **Ubicación de Fuentes:** Guarda las fuentes `.d2` en `docs/diagrams/` usando nombres en minúsculas con guiones.
3. **Renderizado Local Automático:**
   - Cada vez que crees o modifiques un archivo `.d2`, ejecuta inmediatamente la herramienta CLI en el entorno local para generar el SVG correspondiente en `docs/diagrams/rendered/`:
     ```bash
     d2 --layout=dagre docs/diagrams/<nombre>.d2 docs/diagrams/rendered/<nombre>.svg
     ```
4. **Entregable:** Ambas fuentes (`.d2`) y su renderizado (`.svg`) deben quedar listos para ser incluidos en el commit local.
