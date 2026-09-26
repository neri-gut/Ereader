# Tasks — immersive-reading-ux

## Specs y docs

- [ ] Copiar este árbol `openspec/` a la raíz del repo (junto a `.openspec/` legado).
- [ ] Anotar deprecación del Rail en `.openspec/adaptative-ui.yml`.
- [ ] Mover D2 de `openspec/research/diagrams/` a `docs/diagrams/` al commitear.

## Lector

- [ ] Añadir `chromeVisible` al estado / VM con `toggle/show/hide`.
- [ ] Recomponer `ReaderScreen` a `Box` + overlays animados.
- [ ] Zona muerta vs tap de párrafo.
- [ ] Ocultar cromo al hacer scroll.
- [ ] Auto-scroll solo si el párrafo activo salió del viewport.
- [ ] Cablear `ThemeSettingsPanel` en `ModalBottomSheet`.
- [ ] Sustituir `TextButton` del top bar por iconos.
- [ ] Compactar `TtsControls` a una fila + slider revelable.
- [ ] Hairline de progreso.
- [ ] Hairline-only cuando Playing e inmersivo.
- [ ] Default `showNativePdf = false` al abrir.
- [ ] Insets edge-to-edge y hide/show system bars.

## Shell

- [ ] Quitar `TopAppBar` del Scaffold cuando `tab == READER`.
- [ ] No aplicar `padding` de Scaffold al lector.
- [ ] `gesturesEnabled` del drawer según cromo.

## Biblioteca / Ajustes

- [ ] Empty state de dos líneas.
- [ ] `widthIn` en Ajustes para Expanded.

## CI

- [ ] Añadir rama `feature/initReader` (y `feature/**` si se prefiere) al workflow.

## Verificación

- [ ] POCO F7: tap cromo, scroll oculta, sheet cambia letra sin salir.
- [ ] MatePad: columna centrada, sin rail, drawer desde icono.
- [ ] TTS Playing inmersivo: solo highlight + hairline.
- [ ] Abrir libro → texto, no PDF.
- [ ] `./gradlew testDebugUnitTest` sigue verde.
