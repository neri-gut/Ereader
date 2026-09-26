# library

Puerta de entrada al libro, no un explorador de archivos.

## Requirements

### Requirement: Abrir es la acción primaria

Tocar una portada SHALL abrir el lector en modo texto.

#### Scenario: Tap en tarjeta
- **WHEN** el usuario toca la portada o el título
- **THEN** se llama a abrir documento y la app navega al lector de texto

### Requirement: Importar no tapa la parrilla

El FAB SHALL ofrecer un PDF, varios PDF o carpeta. SHALL usar icono, no solo "+".

#### Scenario: Añadir
- **WHEN** el usuario pulsa el FAB
- **THEN** ve las tres acciones SAF existentes

### Requirement: Progreso visible sin ruido

Cada tarjeta SHALL mostrar avance si hay `totalParagraphs > 0`. El vacío SHALL ser una sola frase, no un tutorial de tres párrafos.

#### Scenario: Vacío
- **WHEN** no hay documentos
- **THEN** el mensaje cabe en dos líneas y señala el FAB

### Requirement: Biblioteca no rediseña el lector

Los filtros Todos / Recientes / Favoritos y la búsqueda SHALL permanecer. Este ciclo no cambia el modelo `LibraryDocument`.
