# OpenReader

OpenReader es un lector de libros en formato PDF de código abierto, 100% offline y enfocado en la privacidad. No requiere cuentas, sincronización en la nube ni conexión a internet para su funcionamiento principal.

Diseñado con una interfaz adaptativa en Jetpack Compose, optimizado tanto para teléfonos compactos (**POCO F7**) como para tablets de gran formato (**MatePad 12 X**).

---

## Funcionalidades V1

- **Biblioteca Local:** Carga de archivos PDF desde almacenamiento interno, tarjetas SD, unidades USB (OTG) y carpetas compartidas usando Storage Access Framework (SAF).
- **Vista PDF Nativa:** Visor opcional estructurado por páginas para comprobación rápida del documento original.
- **Modo Lectura Continuo:** Extracción de texto plano sin saltos de página, encabezados, imágenes ni marcas de agua.
- **Motor de Temas:** Modos Noche, Sepia y Claro. Ajustes de tipografía (Serif, Sans, Mono, OpenDyslexic), tamaño (12sp–32sp), interlineado y ancho máximo alineado (~42–48 rem en pantallas grandes).
- **Persistencia de Progreso:** Guardado exacto de párrafo y desplazamiento de carácter mediante hash SHA-256 por archivo (independiente de la ruta del archivo).
- **Lectura por Voz (TTS) Neuronal Local:**
  - Síntesis local con **Sherpa-ONNX** y modelos **Piper VITS**.
  - Cola de precarga asíncrona ($N+1$) para reproducción fluida sin pausas.
  - Selección entre motor neuronal y motor TTS nativo del sistema.
  - Descarga inicial de modelos a la memoria privada interna del dispositivo (`/data/user/0/.../files/models`).

---

## Entorno de Desarrollo (Dev Container)

El proyecto incluye una configuración de **Dev Container** lista para usar con VS Code o Cursor. Contiene JDK 17, el SDK de Android (API 35), herramientas de compilación y la CLI de D2 preinstaladas.

### Requisitos Previos

- Docker Desktop o Docker Engine ejecutándose.
- VS Code / Cursor con la extensión **Dev Containers** instalada.
- **Importante:** Se recomienda clonar y ejecutar el repositorio siempre en un **disco local SSD** (formateado en Ext4/APFS). Trabajar sobre unidades externas en exFAT/NTFS causará fallos de permisos y lentitud en Gradle.

### Inicio Rápido con Dev Container

1. Clona el repositorio en tu disco local:
   ```bash
   git clone [https://github.com/neri-gut/Ereader.git](https://github.com/neri-gut/Ereader.git)
   cd Ereader

```

2. Abre la carpeta en VS Code / Cursor.
3. Presiona `Ctrl+Shift+P` (o `Cmd+Shift+P` en macOS) y selecciona:
**`Dev Containers: Reopen in Container`**.
4. El contenedor compilará el entorno automáticamente.

---

## Despliegue en Dispositivos Físicos (POCO F7 / MatePad 12 X)

Para instalar y probar la aplicación en dispositivos físicos sin depender de cables desde el contenedor:

1. Activa **Opciones de Desarrollador**, **Depuración USB** y **Depuración Inalámbrica** en tu POCO F7 o MatePad 12 X.
2. Conecta tu PC y los dispositivos a la misma red Wi-Fi.
3. Obtén la dirección IP del dispositivo (Ajustes -> Depuración Inalámbrica).
4. Ejecuta desde la terminal (dentro o fuera del contenedor):
```bash
adb connect <IP_DISPOSITIVO>:5555
./gradlew installDebug

```



---

## Comandos de Desarrollo

```bash
# Ejecutar pruebas unitarias
./gradlew testDebugUnitTest

# Compilar APK Debug
./gradlew assembleDebug

# Instalar directamente en el dispositivo conectado por ADB
./gradlew installDebug

# Renderizar diagramas D2 a SVG localmente
d2 --layout=dagre docs/diagrams/architecture.d2 docs/diagrams/rendered/architecture.svg

```

---

## Estructura de Arquitectura

El proyecto está organizado en módulos desacoplados:

```text
OpenReader
├── .devcontainer/        # Entorno Docker con Android SDK + D2 CLI
├── .openspec/            # Especificaciones funcionales YAML
├── app/                  # Punto de entrada Android y DI principal
├── core/
│   ├── model/            # Modelos de dominio puros
│   ├── database/         # Room Database y DataStore Preferences
│   ├── pdf/              # Motor de extracción de texto (PdfBox)
│   └── tts/              # Motor Sherpa-ONNX, AudioTrack y cola N+1
├── feature/
│   ├── library/          # Explorador de archivos y biblioteca
│   ├── reader/           # Pantalla de lectura continua y controles
│   └── downloader/       # Gestor de descarga de modelos de voz
└── docs/diagrams/        # Diagramas de arquitectura D2 y SVGs renderizados

```

---

## Licencia

Desarrollado bajo licencia MIT. Código 100% abierto y libre de rastreadores.
