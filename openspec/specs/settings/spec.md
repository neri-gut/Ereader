# settings

Preferencias persistentes. Misma fuente de verdad que el sheet del lector.

## Requirements

### Requirement: Una sola fuente de tema

Ajustes y el sheet del lector SHALL escribir el mismo `ReaderTheme` / `TTSConfig` (DataStore vía ViewModel).

#### Scenario: Cambio cruzado
- **WHEN** el usuario cambia el tamaño de letra en el sheet del lector
- **THEN** Ajustes refleja ese valor al volver
- **AND** viceversa

### Requirement: Ajustes no es el único camino

El usuario SHALL poder leer un libro entero sin abrir la pestaña Ajustes.

#### Scenario: Primera lectura
- **WHEN** un usuario abre un PDF y cambia a Sepia desde el sheet
- **THEN** no necesita visitar Ajustes para que el cambio persista

### Requirement: Ajustes agrupa lo que no cabe en el libro

Ajustes SHALL conservar motor TTS, velocidad por defecto, enlace a Voces y Acerca de.
El bloque Apariencia/Lectura puede permanecer como espejo, no como único editor.
