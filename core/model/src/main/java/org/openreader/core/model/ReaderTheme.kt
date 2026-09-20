package org.openreader.core.model

enum class ThemeType {
    LIGHT,
    SEPIA,
    NIGHT
}

enum class FontFamilyType {
    SERIF,
    SANS_SERIF,
    MONOSPACE,
    OPENDYSLEXIC
}

/**
 * Configuración visual de la pantalla de lectura continua.
 */
data class ReaderTheme(
    val type: ThemeType = ThemeType.LIGHT,
    val fontFamily: FontFamilyType = FontFamilyType.SANS_SERIF,
    val fontSizeSp: Int = 18,
    val lineHeightMultiplier: Float = 1.4f,
    val maxContainerWidthRem: Int = 44
)
