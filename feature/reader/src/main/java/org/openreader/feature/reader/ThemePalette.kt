package org.openreader.feature.reader

import androidx.compose.ui.graphics.Color
import org.openreader.core.model.ThemeType

data class ThemeColors(
    val background: Color,
    val text: Color,
    val highlight: Color
)

fun colorsFor(type: ThemeType): ThemeColors = when (type) {
    ThemeType.LIGHT -> ThemeColors(
        background = Color(0xFFFFFFFF),
        text = Color(0xFF121212),
        highlight = Color(0xFFFFF59D)
    )
    ThemeType.SEPIA -> ThemeColors(
        background = Color(0xFFF4ECD8),
        text = Color(0xFF5F4B32),
        highlight = Color(0xFFE6C990)
    )
    ThemeType.NIGHT -> ThemeColors(
        background = Color(0xFF121212),
        text = Color(0xFFE0E0E0),
        highlight = Color(0xFF455A64)
    )
}
