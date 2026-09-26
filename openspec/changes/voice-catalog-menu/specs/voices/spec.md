# voices (delta)

## MODIFIED Requirements

### Requirement: Voces es un destino satélite

La pantalla Voces SHALL descargar, previsualizar, seleccionar, borrar y **añadir** voces. SHALL NOT embeber un lector ni un modelo generativo.

#### Scenario: Elegir voz y volver
- **WHEN** el usuario selecciona una voz desde el transport del lector
- **THEN** llega a Voces
- **AND** al elegir voz activa puede volver al documento abierto sin reimportarlo

#### Scenario: Añadir no abre el libro
- **WHEN** el usuario añade un pack
- **THEN** permanece en Voces hasta que elige Usar

## ADDED Requirements

### Requirement: Dos capas, no un muro de filtros

Voces SHALL separar lo usable ahora (Listas) de lo descargable (Explorar). Explorar SHALL entrar por idioma. SHALL NOT pintar un chip por idioma en la raíz.

#### Scenario: Muchos idiomas
- **WHEN** el catálogo tiene más de un idioma
- **THEN** Explorar lista idiomas con su conteo
- **AND** las tarjetas de voz aparecen solo tras elegir un idioma o al buscar

#### Scenario: Sistema no es una tercera pestaña
- **WHEN** el usuario abre Listas
- **THEN** las voces de Android aparecen en una sección al final
- **AND** no hay pestaña Sistema

### Requirement: Una acción primaria por voz

Cada voz SHALL mostrar como máximo un botón con texto. El resto SHALL ser icono o menú. Los hablantes de un modelo multi-speaker SHALL elegirlos en un sheet al usar la voz, no en la fila.

#### Scenario: Voz no instalada
- **WHEN** la voz no está en el dispositivo
- **THEN** la acción primaria es Descargar
- **AND** la fila no muestra Eliminar ni chips de hablante

#### Scenario: Voz multi-speaker
- **WHEN** el usuario pulsa Usar en un modelo con `speakerCount > 1`
- **THEN** un sheet lista los hablantes
- **AND** la selección persiste `speakerId`

### Requirement: Catálogo de usuario

La app SHALL aceptar voces nuevas sin recompilar: un id Piper que cumpla el patrón del catálogo, o un archivo local vía SAF. SHALL NOT descargar una URL escrita por el usuario.

#### Scenario: Id Piper nuevo
- **WHEN** el usuario introduce `es_ES-mls_9972-low`
- **THEN** el pack se guarda en el catálogo de usuario
- **AND** la descarga usa el artefacto `vits-piper-es_ES-mls_9972-low.tar.bz2` de sherpa-onnx

#### Scenario: Id inválido
- **WHEN** el texto no cumple el patrón o contiene `..`
- **THEN** no se crea el pack y no hay petición de red

#### Scenario: Archivo local
- **WHEN** el usuario elige un `.tar.bz2` o un `.onnx` con `tokens.txt`
- **THEN** se copia a almacenamiento privado y queda usable offline

### Requirement: La muestra no compite con la descarga

Una voz instalada SHALL previsualizarse sintetizando una frase fija en local. El mp3 remoto SHALL usarse solo si el pack no está instalado y trae muestra.

#### Scenario: Modelo ya en el dispositivo
- **WHEN** el usuario pulsa muestra en una voz instalada
- **THEN** suena una frase corta del motor local
- **AND** no se abre una URL
