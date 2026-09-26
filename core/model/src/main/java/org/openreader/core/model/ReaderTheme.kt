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
) {
    companion object {
        const val MIN_FONT_SIZE_SP = 12
        const val MAX_FONT_SIZE_SP = 32
        const val MIN_LINE_HEIGHT = 1.0f
        const val MAX_LINE_HEIGHT = 2.0f
        const val MIN_WIDTH_REM = 42
        const val MAX_WIDTH_REM = 48
    }
}
