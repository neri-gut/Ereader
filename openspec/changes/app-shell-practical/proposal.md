# Change: app-shell-practical

## Why

El tema de lectura (Claro, Sepia, Noche) solo pinta la columna del libro. Biblioteca, Voces, Ajustes y el drawer siguen en el verde papel de `OpenReaderTheme`, que además se llama con `darkTheme = false` fijo. En Noche el usuario sale del libro y vuelve a una app clara. Las barras de sistema también se fuerzan claras fuera del lector.

El menú del shell es un `TextButton("Menú")` en la esquina. El del lector ya es un icono.

No hay pila. `tab` se sustituye. Atrás en el lector inmersivo salta a Biblioteca; con el cromo visible, Atrás cierra la app. Desde Ajustes, «Abrir catálogo de voces» pierde el sitio de vuelta. Ajustes es un scroll único de chips y sliders.

## What

- El `ReaderTheme.type` pinta toda la app: fondo, superficies, texto, drawer y barras.
- Icono de menú (y iconos en el drawer).
- Pila: Biblioteca → Lector → satélite. Atrás deshace un nivel. El cromo del lector se oculta antes de salir del libro.
- Ajustes en tres filas (Lectura, Audio, Acerca de). Cada una es una pantalla corta con Atrás a la lista. «Elegir voz» apila Voces y Atrás vuelve a Audio.

## Impact

- `app/.../theme/Theme.kt`
- `app/.../OpenReaderApp.kt`
- `app/.../ui/SettingsScreen.kt`

## Out of scope

Motor PDF, cola TTS, catálogo de voces, rail en tablet.
