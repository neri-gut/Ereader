# Tasks — voice-catalog-menu

## UI

- [ ] Sustituir tabs y buscador por cuatro niveles: fuente, idioma, género, nombre.
- [ ] Cada pantalla lista solo el eje siguiente. Un eje con una sola opción se omite.
- [ ] La tarjeta muestra nombre y características (región, hablantes, Lista o Por descargar).
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

- [ ] Descenso: fuente, luego idioma, luego género; un solo género salta al nombre; la activa queda primera.
- [ ] Parser: id `es_ES-mls_9972-low` → locale `es-ES` y URL esperada.
- [ ] Rechazo: id con `..`, URL fuera de `github.com/k2-fsa/sherpa-onnx`, JSON vacío.

## No tocar

- [ ] Cola N+1, `PdfTextExtractor`, esquema Room, cromo del lector (eso es `immersive-reading-ux`).
