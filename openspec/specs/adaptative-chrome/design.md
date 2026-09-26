# Design — adaptive-chrome

## Conflicto legado

`.openspec/adaptative-ui.yml` pide Navigation Rail y TTS flotante en MatePad.
`docs` / `adaptive-ui.d2` y este ciclo dicen: **mismo drawer, sin rail**.
Gana este documento. El YAML se marcará deprecado en la change.

## Cómo

En `OpenReaderApp`:

- `gesturesEnabled = chromeVisible && tab == READER || tab != READER || drawerState.isOpen`
- El lector recibe `Modifier.fillMaxSize()` **sin** `padding` del Scaffold (insets los aplica el propio lector).
- El drawer mantiene cuatro destinos. Iconos + label. El título "OpenReader" puede quedar.

Compacto y expandido comparten destinos. La única variante es el `widthIn` de la columna de texto.
