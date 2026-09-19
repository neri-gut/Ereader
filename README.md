# OpenReader

OpenReader es un lector de libros en formato PDF de código abierto, 100% offline y enfocado en la privacidad. No requiere cuentas, sincronización en la nube ni conexión a internet para su funcionamiento principal.

Diseñado con una interfaz adaptativa en Jetpack Compose, optimizado tanto para teléfonos compactos (**POCO F7**) como para tablets de gran formato (**MatePad 12 X**).

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

## Arquitectura

El proyecto está estructurado bajo principios de Clean Architecture y modularización estricta:

```text
OpenReader
├── app/                  # Punto de entrada Android y DI principal
├── core/
│   ├── model/            # Modelos de dominio
│   ├── database/         # Room Database y DataStore Preferences
│   ├── pdf/              # Motor de extracción de texto (PdfBox)
│   └── tts/              # Motor Sherpa-ONNX, AudioTrack y cola N+1
├── feature/
│   ├── library/          # Explorador de archivos y biblioteca
│   ├── reader/           # Pantalla de lectura continua y controles
│   └── downloader/       # Gestor de descarga de modelos de voz
├── .openspec/            # Especificaciones funcionales YAML
└── docs/diagrams/        # Diagramas de arquitectura en lenguaje D2
```

## Requisitos de Compilación
- Android Studio Jellyfish (2024.1.1) o superior
- JDK 17
- Android SDK 35 (Min. SDK 26)

## Guía de Desarrollo

# Ejecutar pruebas unitarias
./gradlew test DebugUnitTest

# Compilar APK de desarrollo
./gradlew assembleDebug

# Renderizar diagramas D2 a SVG (requiere D2 CLI instalado)
d2 docs/diagrams/tts-queue-pipeline.d2 docs/diagrams/rendered/tts-queue-pipeline.svg
