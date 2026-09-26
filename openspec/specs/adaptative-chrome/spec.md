# adaptive-chrome

Cromo y navegación al servicio del texto.

## Requirements

### Requirement: Un drawer en todos los tamaños

La app SHALL usar `ModalNavigationDrawer` en Compact y Expanded. SHALL NOT añadir Navigation Rail ni barra inferior permanente en el lector.

#### Scenario: Tablet
- **WHEN** el ancho es Expanded y el usuario está en el lector
- **THEN** no hay rail que robe columna
- **AND** el menú se abre solo por icono o gesto de borde con cromo visible

### Requirement: El shell no invade el lector

El `Scaffold` del shell SHALL omitir `TopAppBar` en la pestaña Lector. El lector pinta su propio overlay.

#### Scenario: Entrar al lector
- **WHEN** `tab == READER`
- **THEN** no hay barra "Biblioteca/Voces/Ajustes" encima del texto

### Requirement: Gestos del drawer no pelean con el scroll

El gesto de abrir drawer SHALL estar desactivado mientras el lector está inmersivo.

#### Scenario: Leyendo
- **WHEN** el cromo del lector está oculto
- **THEN** `gesturesEnabled` del drawer es false
- **AND** un swipe vertical no abre el menú

### Requirement: Destinos satélite

Biblioteca, Voces y Ajustes SHALL ser destinos del drawer. "Seguir leyendo" SHALL estar deshabilitado si no hay documento abierto.

#### Scenario: Sin documento
- **WHEN** no hay documento abierto
- **THEN** "Seguir leyendo" no cambia de pestaña
