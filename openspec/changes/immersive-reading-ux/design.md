# Design — immersive-reading-ux

## Approach

Recomponer `ReaderScreen` como un `Box` a pantalla completa. El texto (o el PDF de verificación) es la base. El cromo flota. El sheet de tema se ancla abajo. El ViewModel gana `chromeVisible: Boolean` y reutiliza `showTtsBar`, `showNativePdf`, `theme`.

## State

```
ReaderUiState +
  chromeVisible: Boolean = false after first frame
```

Eventos nuevos: `toggleChrome()`, `showChrome()`, `hideChrome()`.
`selectParagraph` no toca `chromeVisible`.

## Gesture arbitration

```
pointerInput on list container
  tap without drag:
    if hit paragraph text -> selectParagraph
    else -> toggleChrome
  scroll -> hideChrome
drawer.gesturesEnabled = chromeVisible || drawer.isOpen
```

## Overlay contents

**Top (48dp + status insets)**  
Menu | Title ellipsis | Text/PDF | Tune | (optional) TTS eye  

**Bottom (solo con chrome)**  
Prev | Play/Pause | Next | Rate chip  
Voice label → Voces  

**Hairline**  
`paragraphIndex / totalParagraphs` como `LinearProgressIndicator` 2dp.

## Theme sheet

`ModalBottomSheet` + `ThemeSettingsPanel` existente.
Skip part de Ajustes. Persistencia ya está en `updateTheme`.

## System UI

`MainActivity` ya puede ser edge-to-edge. En lector:
- inmersivo: hide status/nav
- cromo: show, color `palette.background`, iconos claros en Noche

## Migration / compat

No hay migración de datos. `showTtsBar` persistido se interpreta como "el usuario quiere transport al mostrar cromo", no como "barra siempre visible".

## Risks

- Doble tap target en párrafos cortos: el padding inferior de 16dp entre items es zona muerta a propósito.
- Auto-scroll vs usuario: guardar `userScroll` y no animar si visible.
- Accesibilidad: overlays deben ser `hidden` para TalkBack cuando `!chromeVisible`, y el tap de párrafo debe anunciar "párrafo N seleccionado".
