# app-shell (delta)

## ADDED Requirements

### Requirement: El tema de lectura pinta toda la app

Biblioteca, Voces, Ajustes y el drawer SHALL usar los colores del `ReaderTheme` activo. Noche SHALL usar iconos claros en las barras del sistema también fuera del lector.

#### Scenario: Noche en la biblioteca
- **WHEN** el tema es Noche y el usuario está en Biblioteca
- **THEN** el fondo es el de la página nocturna y el texto es claro

#### Scenario: Sepia
- **WHEN** el tema es Sepia
- **THEN** el shell usa el fondo `#F4ECD8` y el texto `#5F4B32`

### Requirement: El menú es un icono

El control que abre el drawer en Biblioteca, Voces y Ajustes SHALL ser un icono de menú con área táctil de icon button, no un botón de texto.

#### Scenario: Abrir el drawer
- **WHEN** el usuario pulsa el icono de la esquina
- **THEN** se abre el drawer

### Requirement: Atrás deshace un paso

La app SHALL recordar el origen. Atrás SHALL cerrar el drawer, ocultar el cromo del lector o volver a la pantalla anterior, en ese orden, antes de salir de la app.

#### Scenario: Voces abiertas desde el libro
- **WHEN** el usuario abre Voces desde el lector y pulsa Atrás
- **THEN** vuelve al lector con el documento abierto

#### Scenario: Voces abiertas desde Ajustes
- **WHEN** el usuario abre Voces desde Ajustes y pulsa Atrás
- **THEN** vuelve a Ajustes

#### Scenario: Cromo visible
- **WHEN** el cromo del lector está visible y el usuario pulsa Atrás
- **THEN** el cromo se oculta y el libro sigue abierto

### Requirement: Ajustes se recorre por secciones

Ajustes SHALL abrir en una lista de secciones. Cada sección SHALL caber sin mezclar lectura, audio y acerca de en el mismo scroll. Atrás dentro de una sección SHALL volver a la lista.

#### Scenario: Entrar a lectura
- **WHEN** el usuario pulsa Lectura
- **THEN** ve tema, fuente, tamaño, interlineado y ancho
- **AND** no ve el bloque de audio en esa misma vista
