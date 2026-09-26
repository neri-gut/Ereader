# Design — library

La parrilla actual (`GridCells.Adaptive(156.dp)`, portada 0.72, estrella, menú quitar) se conserva.

Cambios de pulido:

- Sustituir el texto de ayuda largo por un empty state corto.
- FAB con `Icons.Default.Add` (ya casi).
- El `TopAppBar` del shell se queda en Biblioteca: aquí sí aporta título y menú.
- No añadir rail. En tablet la parrilla gana columnas solas.

No tocar SAF ni `PdfCoverLoader` salvo que el empty state lo requiera.
