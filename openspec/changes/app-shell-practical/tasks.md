# Tasks — app-shell-practical

## Specs del change

- [x] `specs/app-shell/spec.md`
- [x] `specs/settings/spec.md` (delta sobre la spec viva de Ajustes)
- [x] `specs/adaptative-chrome/spec.md` (delta sobre la spec viva del shell)

## UI

- [x] `OpenReaderTheme(themeType)` con Claro, Sepia y Noche.
- [x] Envolver drawer + scaffold con ese tema.
- [x] Iconos de status/navigation según Noche, en todas las pestañas.
- [x] `IconButton` Menú en el `TopAppBar` del shell.
- [x] Iconos en los ítems del drawer.
- [x] `stack` en lugar de `tab` suelto. Atrás: drawer, cromo, pop.
- [x] Push de Voces desde el lector y desde Ajustes.
- [x] Ajustes: índice Lectura / Audio / Acerca de, Atrás interno, `FlowRow` de fuentes.
- [x] Compilar `./gradlew :app:compileDebugKotlin`.

## CI

- [x] Arreglar el workflow OpenReader CI. El PR `Feature/init reader` falla en el paso **Set up Android SDK** (`android-actions/setup-android@v3`): `sdkmanager` termina con `Failed to find package 'tools'` y exit code 1. El paquete `tools` ya no existe en el SDK actual, así que el job no llega a `testDebugUnitTest` ni a `assembleDebug`. Quitar esa instalación obsoleta (subir la acción o limitar los paquetes a `platforms;android-35` y `build-tools`) y dejar el run del PR en verde.
