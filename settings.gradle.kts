pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Repositorio de soporte para Sherpa-ONNX u otras librerías externas
        maven { url = java.net.URI("https://jitpack.io") }
    }
}

rootProject.name = "OpenReader"

// Módulo principal
include(":app")

// Módulos Core
include(":core:model")
include(":core:database")
include(":core:pdf")
include(":core:tts")

// Módulos Feature
include(":feature:library")
include(":feature:reader")
include(":feature:downloader")