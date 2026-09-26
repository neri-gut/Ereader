# Change: immersive-reading-ux

## Why

La V1 ya lee y habla. La UI no. El lector reserva una top bar y una barra TTS de seis líneas en todo momento, los ajustes de página viven en otra pestaña y el PDF nativo se presenta como hermano del texto. El producto se definió como **lectura de texto a pantalla completa**. Este cambio alinea la interfaz con esa definición.

## What

- Lector inmersivo: overlays ocultables, tap en zona muerta, scroll que esconde cromo.
- Sheet de lectura cableado (`ThemeSettingsPanel`) sobre el libro.
- Transport TTS compacto; en reproducción inmersiva solo highlight + hairline.
- PDF nativo como verificación explícita.
- Drawer sin rail; el shell no pinta TopAppBar encima del lector.
- Pulido de Biblioteca (empty state) y Ajustes (ancho tablet).
- Deprecar el Rail de `.openspec/adaptative-ui.yml`.
- CI: incluir `feature/initReader` en el workflow.

## Impact

- `feature/reader` (pantalla, top bar, TTS controls).
- `app/OpenReaderApp.kt` (insets, gestures, sin app bar en lector).
- `feature/library` (copy vacío).
- `app/ui/SettingsScreen.kt` (widthIn).
- `.openspec/adaptative-ui.yml` nota de deprecación.
- `.github/workflows/build-and-test.yml`.
- Docs: este árbol `openspec/` + D2.

## Out of scope

Motor PDF, cola TTS N+1, Room schema, catálogo de voces, EPUB.
