# voices

Catálogo para oír, no para leer.

## Requirements

### Requirement: Voces es un destino satélite

La pantalla Voces SHALL descargar, previsualizar, seleccionar y borrar voces. SHALL NOT embeber un lector.

#### Scenario: Elegir voz y volver
- **WHEN** el usuario selecciona una voz desde el transport del lector
- **THEN** llega a Voces
- **AND** al elegir voz activa puede volver al documento abierto sin reimportarlo

### Requirement: El transport no duplica el catálogo

El overlay TTS del lector SHALL enlazar a Voces. SHALL NOT listar el catálogo completo encima del texto.

#### Scenario: Cambiar voz
- **WHEN** el usuario pulsa el label de voz en el transport
- **THEN** navega a Voces
