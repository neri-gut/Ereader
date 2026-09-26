# adaptive-chrome (delta)

## ADDED Requirements

### Requirement: Un drawer en todos los tamaños
La app SHALL usar `ModalNavigationDrawer` en Compact y Expanded y SHALL NOT añadir Navigation Rail.

#### Scenario: Tablet
- **WHEN** el ancho es Expanded en el lector
- **THEN** no hay rail permanente

### Requirement: El shell no invade el lector
El Scaffold SHALL omitir TopAppBar en la pestaña Lector.

#### Scenario: Entrar al lector
- **WHEN** `tab == READER`
- **THEN** el único cromo superior es el overlay del lector
