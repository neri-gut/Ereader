package org.openreader.feature.reader

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontFamily.Companion.Monospace
import androidx.compose.ui.text.font.FontFamily.Companion.SansSerif
import androidx.compose.ui.text.font.FontFamily.Companion.Serif
import org.openreader.core.model.FontFamilyType

private val OpenDyslexic = FontFamily(Font(R.font.opendyslexic_regular))

@Composable
fun readerFontFamily(type: FontFamilyType): FontFamily = when (type) {
    FontFamilyType.SERIF -> Serif
    FontFamilyType.SANS_SERIF -> SansSerif
    FontFamilyType.MONOSPACE -> Monospace
    FontFamilyType.OPENDYSLEXIC -> OpenDyslexic
}
