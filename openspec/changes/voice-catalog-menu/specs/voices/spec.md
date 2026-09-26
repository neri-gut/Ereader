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

### Requirement: Cuatro niveles, no un muro de filtros

Voces SHALL descender en este orden: tipo de fuente (Neuronal o Sistema), idioma, género y, al final, el nombre con sus características. Cada pantalla SHALL listar solo el eje siguiente. SHALL NOT mostrar un buscador ni chips de idioma o género junto a las tarjetas. Un nivel con una sola opción SHALL omitirse.

#### Scenario: Elegir la fuente
- **WHEN** el usuario abre Voces
- **THEN** ve Neuronal y Sistema con su conteo
- **AND** no ve tarjetas de voz ni un campo de búsqueda

#### Scenario: Idioma y género
- **WHEN** elige Neuronal y hay más de un idioma
- **THEN** ve los idiomas con su conteo
- **AND** al elegir un idioma ve solo los géneros presentes
- **AND** las tarjetas, con nombre y características, aparecen después del género

#### Scenario: Un solo valor
- **WHEN** un nivel tiene una sola opción
- **THEN** ese nivel se omite y se muestra el siguiente

#### Scenario: Sistema no se mezcla con el catálogo
- **WHEN** el usuario elige Sistema
- **THEN** solo ve voces de Android, agrupadas por idioma
- **AND** no hay pestaña Sistema ni sección de sistema dentro de Neuronal

### Requirement: Una acción primaria por voz

Cada voz SHALL mostrar como máximo un botón con texto. El resto SHALL ser icono o menú. Los hablantes de un modelo multi-speaker SHALL elegirse en un sheet, no en la fila. El control de oír SHALL mostrar detener mientras esa muestra suena y volver a reproducir al detenerla.

#### Scenario: Voz no instalada
- **WHEN** la voz no está en el dispositivo
- **THEN** la acción primaria es Descargar
- **AND** la fila no muestra Eliminar ni chips de hablante

#### Scenario: Voz multi-speaker
- **WHEN** el usuario pulsa Elegir en un modelo con `speakerCount > 1`
- **THEN** un sheet lista cada hablante con Oír y Usar
- **AND** Usar persiste `speakerId`
- **AND** Oír reproduce solo ese hablante

#### Scenario: La muestra se detiene
- **WHEN** una muestra está sonando y el usuario pulsa el mismo control
- **THEN** el audio se detiene
- **AND** el control vuelve a mostrar reproducir

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
