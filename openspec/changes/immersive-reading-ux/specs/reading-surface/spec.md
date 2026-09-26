# reading-surface (delta)

## ADDED Requirements

### Requirement: El texto es el destino por defecto
Al abrir un documento la app SHALL mostrar la columna de párrafos extraídos, no el preview PDF nativo.

#### Scenario: Abrir desde biblioteca
- **WHEN** el usuario abre un PDF desde Biblioteca
- **THEN** entra al lector en modo texto

### Requirement: Cromo ocultable
El lector SHALL permitir mostrar u ocultar overlays con tap en zona muerta y SHALL ocultarlos al hacer scroll.

#### Scenario: Tap para cromo
- **WHEN** el usuario toca el padding o el espacio entre párrafos
- **THEN** los overlays conmutan

### Requirement: Ajustes de lectura desde el libro
El lector SHALL exponer tema y tipografía en un sheet sin salir del documento.

#### Scenario: Abrir sheet
- **WHEN** el usuario activa "Página"
- **THEN** `ThemeSettingsPanel` aparece sobre el texto y persiste cambios
