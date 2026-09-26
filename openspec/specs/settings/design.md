# Design — settings

`SettingsScreen` ya es un formulario correcto. Este ciclo no lo rediseña de cero.

Orden recomendado:

1. Apariencia (espejo del sheet)
2. Lectura (tamaño, interlineado, ancho tablet)
3. Audio (motor + velocidad + CTA Voces)
4. Acerca de

En tablet, limitar el formulario con el mismo `widthIn(max = 48.rem)` para que no se estire a 12 pulgadas.
