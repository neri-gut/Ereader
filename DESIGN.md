# DESIGN.md — OpenReader (producto)

## Contexto

La V1 ya abre PDF, extrae texto, guarda progreso y lee en voz alta.
La UI todavía se siente como un prototipo funcional: barras permanentes, botones de texto, ajustes fuera del libro y el PDF nativo al mismo nivel que la lectura.

Este documento fija el **cómo visual y de interacción** del producto. El comportamiento comprobable vive en `openspec/specs/`. El plan de implementación vive en `openspec/changes/immersive-reading-ux/`.

## Principio único

> Si un control no ayuda a leer o a oír el párrafo actual, no puede permanecer en pantalla.

## Arquitectura de superficies

```
App shell (drawer)
├── Biblioteca     → elegir qué leer
├── Lector         → superficie primaria (texto full-screen)
│     ├── overlay superior (ocultable)
│     ├── columna de texto
│     ├── overlay TTS (ocultable; franja mínima si reproduce)
│     └── sheet de lectura (tema / tipografía)
├── Voces          → preparar el oído, no leer
└── Ajustes        → preferencias persistentes (misma fuente de verdad)
```

No hay Navigation Rail. En tablet el drawer sigue siendo el menú. El espacio horizontal extra se usa en la columna de texto, no en chrome permanente.

## Sistema visual

### Paletas de lectura (ya en `ThemePalette`)

| Tema  | Fondo     | Texto     | Resaltado TTS        |
|-------|-----------|-----------|----------------------|
| Claro | `#FFFFFF` | `#121212` | `#FFF59D`            |
| Sepia | `#F4ECD8` | `#5F4B32` | `#E6C990`            |
| Noche | `#121212` | `#E0E0E0` | `#455A64`            |

El shell (biblioteca, voces, ajustes) usa `OpenReaderTheme` verde papel (`#1B4332` / `#F4F1EA`). El lector **no** hereda el verde del shell: hereda la paleta de lectura para que el 100% del pixel sea página.

### Tipografía de lectura

Serif / Sans / Mono / OpenDyslexic. Rango 12–32sp. Interlineado 1.0–2.0. Ancho tablet 42–48 rem.
La preview del cambio es el propio `LazyColumn`, no un recuadro de ejemplo en Ajustes.

### Cromo

- Iconos 24dp, área táctil 48dp, contraste sobre la paleta activa.
- Overlays con scrim 40–60% del color de fondo, no Material surface verde.
- Progreso: una línea de 2dp bajo el overlay superior o sobre el borde inferior. Nunca un texto "Párrafo 12" permanente.

## Gestos del lector

| Gesto | Acción |
|-------|--------|
| Tap en margen / zona muerta del párrafo | Mostrar u ocultar cromo |
| Tap en un párrafo | Seleccionar origen TTS (no abre menús) |
| Scroll | Ocultar cromo si estaba visible |
| Deslizar desde el borde izquierdo | Drawer (solo si el cromo está visible o el drawer ya abierto) |
| Back | Si hay sheet/menú, cerrarlo; si el cromo está visible, ocultarlo; si no, volver a Biblioteca |

PDF nativo: mismos overlays. El documento original no sustituye la columna de texto al abrir un libro.

## Estados

1. **Inmersivo** (default a los 1.2s de abrir o al primer scroll): solo texto + highlight TTS.
2. **Cromo**: top overlay + transport TTS + línea de progreso.
3. **Sheet de lectura**: tema y tipografía, anclado abajo, el texto sigue visible arriba.
4. **Verificación PDF**: preview nativo a pantalla completa; un control "Texto" para volver.
5. **Extracción**: la columna puede ir llenándose; un indicador lineal discreto, no un spinner que tapa la página.

## Decisiones que no se reabren en este ciclo

- Drawer único en compacto y expandido.
- Texto extraído como formato canónico de lectura.
- TTS local (sistema o Piper), sin streaming a la nube.
- Identidad de documento por hash, no por ruta.

## Riesgos

- Confundir tap-para-cromo con tap-para-seleccionar párrafo: la zona de tap del párrafo es el bloque de texto; la zona muerta es padding horizontal y el espacio entre párrafos.
- Edge-to-edge + highlight + system bars en Noche: forzar iconos claros en status bar.
- `LaunchedEffect(currentParagraph)` ya hace auto-scroll y pelea con el scroll del usuario: el rediseño no debe animar si el item ya está visible.
