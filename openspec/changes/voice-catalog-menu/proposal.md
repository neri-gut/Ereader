# Change: voice-catalog-menu

## Why

`DownloaderScreen` apila, en el mismo scroll, pestañas, texto de ayuda, estado de descarga, buscador, una fila de chips por idioma, otra por género y, en cada voz, hablantes siempre visibles más dos o tres botones con etiqueta larga (`Escuchar muestra`, `Usar esta`, `Eliminar`). En un POCO F7 la lista de voces queda debajo del pliegue. Los hablantes van en un `Row` que se corta.

El catálogo vive en `VoiceCatalog.piperVoices` (17 packs). Añadir un idioma o una voz Piper implica editar Kotlin y recompilar. El usuario quiere sumar modelos TTS para su propio uso sin convertir Voces en un panel de controles.

Este cambio no mete un LLM ni un chat. "Modelo de idioma" aquí es un pack de voz (locale + motor TTS), no un modelo generativo. El lector de texto sigue siendo la superficie principal; Voces solo prepara con qué voz se oye.

## What

- Voces en dos capas: **Listas** (lo instalado y el sistema) y **Explorar** (índice por idioma, no muro de chips).
- Cada voz es una tarjeta con una acción primaria y el resto en iconos / menú. Los hablantes solo se muestran al usar un modelo multi-speaker.
- Catálogo de usuario encima del empaquetado: id Piper conocido o archivo local (SAF). Persistido en `files/user-voices.json`.
- Hosts permitidos para descarga. Sin URLs arbitrarias.
- El transport del lector no gana controles nuevos. Sigue enlazando aquí (`immersive-reading-ux`).

## Impact

- `feature/downloader/DownloaderScreen.kt` (reestructura de UI).
- `feature/downloader/VoiceCatalog.kt` (merge bundled + user).
- Nuevo `UserVoiceCatalog` + parser JSON.
- `DownloaderViewModel` (añadir / borrar pack de usuario, preview local).
- `.gitignore` ya ignora `files/models/`; añadir `user-voices.json` si alguna copia cae en el repo.
- No toca `PdfTextExtractor`, la cola N+1 ni el esquema Room.

## Out of scope

- LLM, resumen, chat o "preguntar al libro".
- Cuentas, sync, tienda de voces.
- Descargar todos los idiomas de Piper.
- Cambiar el motor Sherpa/AudioTrack.
- Navigation Rail.
