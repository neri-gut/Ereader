# Design — reading-surface

## Cómo

`ReaderScreen` deja de ser `Column(TopBar + Body + TtsControls)` permanente.

Estructura objetivo:

```
Box(fullscreen, palette.background) {
  ContinuousText | PdfNativePreview | Loading
  AnimatedVisibility(chrome) { ReaderTopOverlay }   // top
  AnimatedVisibility(chrome || thinProgress) { ProgressHairline }
  AnimatedVisibility(chrome && showTtsBar) { TtsTransport } // bottom
  if (sheet) ModalBottomSheet { ThemeSettingsPanel }
}
```

## Decisiones

1. **Un Box, no un Column que reserva alto.** Los overlays flotan. El `LazyColumn` usa `contentPadding` top/bottom igual al alto del overlay para que la primera línea no quede bajo iconos cuando el cromo está visible. En inmersivo el padding vuelve a 16dp.
2. **Zona muerta vs párrafo.** `clickable` sigue en el `Text` del párrafo (selección). El `Box`/`LazyColumn` captura tap en padding mediante `pointerInput` + detección de tap sin drag. Si el tap ocurre sobre un item, gana el párrafo.
3. **Auto-scroll.** `animateScrollToItem` solo si el índice activo no está visible (`layoutInfo.visibleItemsInfo`). Evita pelear con el dedo.
4. **Cablear `ThemeSettingsPanel`.** Hoy está huérfano. El overlay superior abre el sheet; `onThemeChange` ya existe en el ViewModel.
5. **Top overlay.** Iconos: menú, título elipsis, `chrome_reader_mode` (PDF/texto), `tune` (página), `graphic_eq` (mostrar TTS). Adiós `TextButton("Menú")` / `("Vista")`.
6. **TTS transport.** Una fila: prev / play / next + velocidad en un `IconToggle` que revela el slider. Sin párrafo "toca un párrafo…" permanente.
7. **System bars.** `WindowInsetsController` hide on immersive, show with chrome. Color = `palette.background`.

## Archivos

- `feature/reader/.../ReaderScreen.kt`
- `feature/reader/.../ReaderTopBar.kt` → `ReaderTopOverlay.kt`
- `feature/reader/.../TtsControls.kt` → compactar
- `feature/reader/.../ThemeSettingsPanel.kt` (sin cambio de contrato)
- `app/.../OpenReaderApp.kt` (padding del Scaffold: el lector no debe recibir `padding` de una TopAppBar del shell)

## No hacer

- No meter Navigation Rail.
- No extraer otra vez el PDF.
- No rediseñar `PdfNativePreview` salvo insets.
