# OpenReader — contexto de producto

## Norte

OpenReader es un lector **offline** de texto extraído de PDF.
La superficie principal no es un visor de páginas: es una **columna de texto continuo a pantalla completa**.
Biblioteca, voces, temas y TTS existen para entrar, oír y personalizar esa columna, no para competir con ella.

## Dispositivos

- Compacto (`width < 600dp`): POCO F7. Texto al 100% del ancho útil. Cromo superpuesto y ocultable.
- Expandido (`width >= 840dp`): MatePad 12 X. Misma navegación (drawer, sin rail fijo). Columna centrada `42rem–48rem`.

## Lo que ya existe (rama `feature/initReader`)

- Módulos: `app`, `core/{model,database,pdf,tts}`, `feature/{library,reader,downloader}`.
- Biblioteca con portadas, favoritos, progreso y SAF.
- Lector: `LazyColumn` de párrafos, resaltado TTS, preview PDF nativo, barra TTS textual, top bar siempre visible.
- Temas Claro / Sepia / Noche, tipografías incluyendo OpenDyslexic, DataStore + Room.
- TTS sistema o Sherpa-ONNX Piper con cola N+1.

## Deuda UX que este ciclo cierra

- El cromo del lector no se oculta (incumple el espíritu de REQ-UI-02).
- `ThemeSettingsPanel` existe y no está cableado al lector: hay que salir del libro para cambiar letra.
- Controles con `TextButton` ("Menú", "Vista", "Leer") en lugar de iconos densos.
- PDF nativo y texto se presentan como modos iguales; el texto debe ser el default y el PDF una verificación.
- La barra TTS ocupa demasiado alto cuando el usuario solo quiere leer.
- CI no construye la rama `feature/initReader`.

## No objetivos de este ciclo

- EPUB, comic, web o sync en la nube.
- Anotaciones, subrayado persistente, diccionario.
- Navigation Rail en tablet (rompe la columna de lectura).
- Rediseñar el motor PDF/TTS.
