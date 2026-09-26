# settings (delta)

## MODIFIED Requirements

### Requirement: Ajustes agrupa lo que no cabe en el libro

Ajustes SHALL abrir en un índice de tres secciones: Lectura, Audio y Acerca de. Cada sección SHALL mostrar solo sus controles. El índice SHALL mostrar el valor actual. Atrás dentro de una sección SHALL volver al índice antes de salir de Ajustes.

Lectura SHALL editar tema, tipografía, tamaño, interlineado y ancho. Audio SHALL editar motor y velocidad, y SHALL enlazar a Voces sin perder el regreso a Ajustes. Acerca de SHALL quedar en su propia sección.

#### Scenario: Entrar a lectura
- **WHEN** el usuario pulsa Lectura
- **THEN** ve tema, fuente, tamaño, interlineado y ancho
- **AND** no ve motor, velocidad ni Acerca de en esa misma vista

#### Scenario: Volver al índice
- **WHEN** el usuario está dentro de una sección y pulsa Atrás
- **THEN** vuelve a la lista Lectura / Audio / Acerca de

#### Scenario: Elegir voz
- **WHEN** el usuario pulsa Elegir voz desde Audio y luego Atrás
- **THEN** vuelve a Ajustes, no a Biblioteca
