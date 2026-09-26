# Design — app-shell-practical

## Tema en toda la app

`OpenReaderTheme(themeType)` arma el `ColorScheme` con la misma paleta que `colorsFor`:

| Tipo | Fondo | Texto | Barras |
|---|---|---|---|
| LIGHT | `#FFFFFF` | `#121212` | iconos oscuros |
| SEPIA | `#F4ECD8` | `#5F4B32` | iconos oscuros |
| NIGHT | `#121212` | `#E0E0E0` | iconos claros |

Se aplica dentro de `OpenReaderApp`, alrededor del drawer y del scaffold, para que Biblioteca, Voces, Ajustes y el drawer lo hereden. El lector sigue usando `ThemePalette` en la página; al salir, el shell ya coincide.

`lightBars = theme.type != NIGHT` también fuera del lector. Hoy la condición `tab != READER` deja iconos oscuros sobre Noche.

## Menú

`TopAppBar.navigationIcon` pasa de `TextButton("Menú")` a `IconButton` + `Icons.Filled.Menu` (material-icons-core, ya en `:app`).

Drawer, mismos cuatro destinos, con icono de core: Home, PlayArrow, List, Settings. Sin rail.

## Pila

Sin Navigation Compose.

```
stack: List<AppTab>   // último = visible
LIBRARY
LIBRARY → READER
LIBRARY → READER → VOICES | SETTINGS
LIBRARY → SETTINGS → VOICES
```

- El drawer abre Biblioteca, Lector, Voces o Ajustes reemplazando el satélite, no anidándolo.
- El transport del lector y «Elegir voz» hacen push, para que Atrás vuelva al origen.
- Elegir una voz con libro abierto deja `LIBRARY → READER`.
- Atrás: si el drawer está abierto, se cierra; si el lector tiene cromo, se oculta; si hay más de una entrada, se quita la última. En inmersivo, Atrás sale del libro a Biblioteca, no mata el proceso a la primera.

## Ajustes

Índice de tres filas. La fila dice el valor actual («Noche · Serif · 18 sp», «Neuronal · 1.0×»). Entrar muestra solo ese bloque. Atrás del sistema vuelve al índice antes de salir de Ajustes. Los chips de tipografía van en `FlowRow` para que OpenDyslexic no se corte en el POCO F7.
