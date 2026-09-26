# reading-surface

Superficie primaria: texto continuo a pantalla completa.

## Requirements

### Requirement: El texto es el destino por defecto

Al abrir un documento la app SHALL mostrar la columna de párrafos extraídos, no el preview PDF nativo.

#### Scenario: Abrir desde biblioteca
- **WHEN** el usuario abre un PDF desde Biblioteca
- **THEN** entra al lector en modo texto
- **AND** el preview nativo permanece apagado hasta una acción explícita

#### Scenario: Reanudar
- **WHEN** el usuario elige "Seguir leyendo"
- **THEN** recupera párrafo y offset guardados en la columna de texto

### Requirement: Pantalla llena de página

En modo inmersivo el lector SHALL dedicar el área segura al texto (compacto: ancho completo; expandido: columna centrada con `maxContainerWidthRem`).

#### Scenario: Compacto
- **WHEN** `WindowWidthSizeClass` es Compact
- **THEN** la columna usa el ancho disponible con padding horizontal ≤ 16dp

#### Scenario: Expandido
- **WHEN** `WindowWidthSizeClass` es Expanded
- **THEN** la columna se centra y no supera el ancho en rem persistido

### Requirement: Cromo ocultable

El lector SHALL iniciar en modo inmersivo tras la carga y SHALL permitir mostrar u ocultar overlays con un tap en zona muerta.

#### Scenario: Tap para cromo
- **WHEN** el usuario toca el padding o el espacio entre párrafos
- **THEN** los overlays superior e inferior conmutan su visibilidad

#### Scenario: Scroll oculta
- **WHEN** el usuario desplaza la lista y el cromo está visible
- **THEN** el cromo se oculta

#### Scenario: Reproducción sin tapar
- **WHEN** el TTS está en Playing y el cromo está oculto
- **THEN** solo permanece una franja de progreso ≤ 3dp o el resaltado del párrafo
- **AND** no permanece la fila de botones de transporte

### Requirement: Ajustes de lectura desde el libro

El lector SHALL exponer tema, fuente, tamaño, interlineado y (en expandido) ancho máximo sin navegar fuera del documento.

#### Scenario: Abrir sheet
- **WHEN** el usuario activa "Página" desde el overlay superior
- **THEN** aparece un sheet sobre el texto con los controles de `ThemeSettingsPanel`
- **AND** los cambios se aplican al `LazyColumn` al instante y se persisten

### Requirement: PDF nativo es verificación

El preview nativo SHALL ser un modo explícito y reversible, no el default.

#### Scenario: Alternar
- **WHEN** el usuario elige "Ver PDF original"
- **THEN** el preview ocupa el área de lectura
- **AND** existe una acción visible para volver a texto

### Requirement: Selección de párrafo no abre menús

Un tap sobre el cuerpo de un párrafo SHALL solo actualizar el origen de TTS/progreso.

#### Scenario: Elegir origen
- **WHEN** el usuario toca el texto de un párrafo
- **THEN** ese índice queda como `currentParagraph`
- **AND** no se abre el drawer ni el sheet

### Requirement: Resaltado de reproducción

Mientras TTS reproduce, el lector SHALL resaltar el rango de caracteres activo sin tapar controles permanentes.

#### Scenario: Highlight
- **WHEN** `AudioState.Playing` aporta `startCharOffset` y `endCharOffset`
- **THEN** ese rango usa el color de highlight del tema
- **AND** el párrafo activo permanece visible (scroll solo si salió de viewport)
