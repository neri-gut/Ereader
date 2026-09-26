# adaptative-chrome (delta)

## ADDED Requirements

### Requirement: El tema de lectura pinta el shell

Biblioteca, Voces, Ajustes y el drawer SHALL usar los colores del `ReaderTheme` activo. Noche SHALL usar iconos claros en las barras del sistema también fuera del lector.

#### Scenario: Noche en la biblioteca
- **WHEN** el tema es Noche y el usuario está en Biblioteca
- **THEN** el fondo es el de la página nocturna y el texto es claro

#### Scenario: Sepia
- **WHEN** el tema es Sepia
- **THEN** el shell usa el fondo `#F4ECD8` y el texto `#5F4B32`

### Requirement: El menú del shell es un icono

El control que abre el drawer en Biblioteca, Voces y Ajustes SHALL ser un icono de menú, no el texto «Menú».

#### Scenario: Abrir el drawer
- **WHEN** el usuario pulsa el icono de la esquina
- **THEN** se abre el drawer

### Requirement: Atrás deshace un paso

La app SHALL recordar el origen. Atrás SHALL cerrar el drawer, ocultar el cromo del lector o volver a la pantalla anterior, en ese orden, antes de salir de la app.

#### Scenario: Voces abiertas desde el libro
- **WHEN** el usuario abre Voces desde el lector y pulsa Atrás
- **THEN** vuelve al lector con el documento abierto

#### Scenario: Cromo visible
- **WHEN** el cromo del lector está visible y el usuario pulsa Atrás
- **THEN** el cromo se oculta y el libro sigue abierto
