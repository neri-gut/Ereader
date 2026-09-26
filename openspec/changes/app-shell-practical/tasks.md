# Tasks — app-shell-practical

## Specs del change

- [x] `specs/app-shell/spec.md`
- [x] `specs/settings/spec.md` (delta sobre la spec viva de Ajustes)
- [x] `specs/adaptative-chrome/spec.md` (delta sobre la spec viva del shell)

## UI

- [ ] `OpenReaderTheme(themeType)` con Claro, Sepia y Noche.
- [ ] Envolver drawer + scaffold con ese tema.
- [ ] Iconos de status/navigation según Noche, en todas las pestañas.
- [ ] `IconButton` Menú en el `TopAppBar` del shell.
- [ ] Iconos en los ítems del drawer.
- [ ] `stack` en lugar de `tab` suelto. Atrás: drawer, cromo, pop.
- [ ] Push de Voces desde el lector y desde Ajustes.
- [ ] Ajustes: índice Lectura / Audio / Acerca de, Atrás interno, `FlowRow` de fuentes.
- [ ] Compilar `./gradlew :app:compileDebugKotlin`.

## CI

- [x] Arreglar el workflow OpenReader CI. `android-actions/setup-android@v3` pedía el paquete `tools`, que el SDK ya no sirve. CI corre solo con push a `develop`, usa `setup-android@v4` con `platform-tools`, `platforms;android-35` y `build-tools;35.0.0`. Push a `main` lo publica `.github/workflows/release.yml`: rama `release/vX.Y.Z`, tag `vX.Y.Z` y GitHub Release, y falla si el tag ya existe o `versionCode` no sube.

