# Design — voices

`DownloaderScreen` se mantiene. Pulido menor: jerarquía de filtros ya existente, no meter sliders de lectura aquí.

El lector solo muestra `engineName · voiceLabel` como `TextButton` hacia Voces.
Velocidad sí vive en el transport (es de la sesión de oír este libro).
Motor sistema/neuronal puede vivir en Voces y en Ajustes; no en un tercer sitio encima del texto.
