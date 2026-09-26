# Tasks — voice-catalog-menu

## UI

- [ ] Sustituir las 3 tabs por Listas | Explorar.
- [ ] Quitar el `FlowRow` de idiomas. Explorar muestra una fila por locale con conteo.
- [ ] Dejar género en un segmento de una sola línea; ocultarlo si hay 8 voces o menos.
- [ ] Mover `DownloadStatus` a la tarjeta en curso.
- [ ] `VoiceCard`: una acción primaria, icono de muestra, menú Eliminar/Detalle.
- [ ] Hablantes solo en un sheet al pulsar Usar, no en un `Row` permanente.
- [ ] Fila superior "En uso".
- [ ] `widthIn(max = 48.rem)` en expandido.
- [ ] Icono Añadir en la barra de Voces.

## Catálogo extensible

- [ ] `UserVoiceCatalog` lee y escribe `filesDir/user-voices.json`.
- [ ] `VoiceCatalog.all()` fusiona empaquetados + usuario (el usuario pisa el mismo id).
- [ ] Sheet "Id Piper": validar el patrón, construir URL sherpa-onnx, no aceptar host libre.
- [ ] Sheet "Archivo local": SAF, copia a `filesDir/models/{id}/`.
- [ ] Borrar pack de usuario elimina JSON y archivos.
- [ ] Muestra local de una frase fija si el modelo ya está instalado; mp3 remoto solo si no lo está y hay `sampleUrl`.

## Tests

- [ ] Filtro: query + locale + género, voz activa primero.
- [ ] Parser: id `es_ES-mls_9972-low` → locale `es-ES` y URL esperada.
- [ ] Rechazo: id con `..`, URL fuera de `github.com/k2-fsa/sherpa-onnx`, JSON vacío.

## No tocar

- [ ] Cola N+1, `PdfTextExtractor`, esquema Room, cromo del lector (eso es `immersive-reading-ux`).
