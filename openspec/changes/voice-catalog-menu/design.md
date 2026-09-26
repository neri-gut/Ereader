# Design — voice-catalog-menu

## Problema concreto

En [DownloaderScreen.kt](https://github.com/neri-gut/Ereader/blob/feature/initReader/feature/downloader/src/main/java/org/openreader/feature/downloader/DownloaderScreen.kt) el primer ítem del `LazyColumn` ya es párrafo + `DownloadStatus` + `OutlinedTextField`. Debajo, si hay más de un idioma, un `FlowRow` de chips (`Todos los idiomas` + uno por grupo) y otro `FlowRow` de género. `VoiceRow` repite título, subtítulo, frase "Este modelo incluye N hablantes", chips en `Row` y botones de texto.

`VoiceCatalog` construye la URL como `vits-piper-$id.tar.bz2` en `github.com/k2-fsa/sherpa-onnx`. Esa convención se puede reutilizar para un id que el usuario escriba, sin hardcodear el pack.

## Capas de la pantalla

```
Voces
├── Fila "En uso" (una): nombre · locale · Neuronal|Sistema     [cambiar no hace falta; ya está marcada]
├── Tab: Listas | Explorar
├── Buscar (una línea)
├── Segmento género: Todos · Mujer · Hombre · Varias   (oculto si la lista cabe en una pantalla)
└── Contenido
      Listas:    voz activa primero, luego neuronales instaladas, luego bloque "Sistema"
      Explorar:  filas de idioma (Español 6, English 7, …)
                 al entrar: tarjetas de ese locale
```

Se eliminan la tercera pestaña y el chip wall de idiomas. Sistema deja de ser un destino: es una sección al final de Listas, porque no se descarga.

En tablet el contenido usa `widthIn(max = 48.rem)`, igual que Ajustes. Sin rail.

## Tarjeta de voz

Una sola acción primaria, texto corto:

| Estado | Primaria | Secundaria (icono) | Menú |
|---|---|---|---|
| No instalada | Descargar | Muestra, si hay URL | — |
| Instalada, no activa | Usar | Muestra local | Eliminar, Detalle |
| Activa | (sin botón Usar; badge "En uso") | Muestra local | Eliminar, Detalle |
| Sistema | Usar / badge | — | — |

Hablantes (`speakerCount > 1`): no se pintan en cada fila. Al pulsar Usar, un `ModalBottomSheet` lista los labels y confirma. Sharvard hoy fuerza un `Row` que desborda en compacto.

Muestra:

- No instalada y el pack trae `sampleUrl`: el mp3 remoto actual.
- Instalada: una frase fija sintetizada con el motor local ("Esta es una muestra."). Sin red.
- Sistema: no hay muestra (Android TTS no expone un preview uniforme aquí).

Progreso de descarga: una barra en la tarjeta que se está bajando, no un bloque global encima del buscador. Error en la misma tarjeta.

## Añadir un modelo (sin recompilar)

Entrada: icono `+` en la barra de Voces. Sheet con dos caminos. No un formulario de doce campos.

### 1. Id Piper

Campo único. Patrón:

```
^[a-z]{2,3}_[A-Z]{2}-[A-Za-z0-9_]+-(low|medium|high)$
```

Ejemplo: `es_ES-mls_9972-low`.

La app rellena sola:

- `archiveUrl` = `https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-{id}.tar.bz2`
- `onnxFileName` = `{id}.onnx`
- `languageCode` desde el prefijo `es_ES` → `es-ES`
- `displayName` = id humano, editable en un segundo campo opcional colapsado
- género = desconocido hasta que el usuario lo marque (chip opcional, default `UNKNOWN`)

Descarga con el `ModelDownloadService` que ya existe. Si el id ya está en el catálogo empaquetado, no se duplica: se ofrece Descargar/Usar.

### 2. Archivo local (SAF)

`OpenDocument` de `.tar.bz2` / `.onnx` (más `tokens.txt` en el mismo directorio si el usuario elige el onnx suelto). Copia a `filesDir/models/{id}/`. Sin red. Sirve para un pack que la persona ya bajó por su cuenta.

### Persistencia

`context.filesDir/user-voices.json`, una lista de `VoicePack`. `VoiceCatalog.all()` = empaquetados + usuario, usuario gana si el id choca. Borrar un pack de usuario quita el JSON y los archivos del modelo. Borrar uno empaquetado solo borra archivos, como hoy (`canDelete`).

No se acepta `archiveUrl` escrita a mano. Solo el host fijo de sherpa-onnx o un archivo local. Las muestras remotas siguen en `huggingface.co/rhasspy/piper-voices` y solo para packs cuyo id encaja en el patrón Piper.

## Relación con el lector

- El overlay TTS sigue mostrando `motor · voz` y navega a Voces. No incrusta este menú.
- Velocidad y motor por defecto siguen en Ajustes. Elegir una voz neuronal aquí pone `engineType = SHERPA_ONNX_PIPER` y el `speakerId`, igual que `onSelectVoice` hoy.
- Al confirmar una voz con un libro abierto, volver al lector (el requisito de `voices` ya lo pide).

## Archivos

- Reescribir la composición de `DownloaderScreen` (tabs, `LanguageIndex`, `VoiceCard`). No hace falta partir el ViewModel en dos features.
- Extraer el filtro puro (`filterVoices`) a función testeable: query, locale, género. Hoy está inline en el composable.
- `UserVoiceCatalog.load/save` en `Dispatchers.IO`.

## Riesgos

- Un id Piper válido puede 404 en GitHub. El fallo queda en la tarjeta (`DownloadState.Failed`), sin diálogo bloqueante.
- Un tar.bz2 local mal formado: el extractor actual ya falla; el pack no se marca instalado.
- Catálogo de usuario grande: Explorar agrupa por idioma, no renderiza 200 tarjetas en la raíz.
