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

- [x] Arreglar el workflow OpenReader CI. `android-actions/setup-android@v3` pedía el paquete `tools`, que el SDK ya no sirve. CI corre solo con push a `develop`, usa `setup-android@v4` con `platform-tools`, `platforms;android-35` y `build-tools;35.0.0`. Push a `main` lo publica `.github/workflows/release.yml`: rama `release/vX.Y.Z`, tag `vX.Y.Z` y GitHub Release, y falla si el tag ya existe o `versionCode` no sube.

