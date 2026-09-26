package org.openreader.feature.reader

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.openreader.core.model.FontFamilyType
import org.openreader.core.model.ReaderTheme
import org.openreader.core.model.ThemeType

@Composable
fun ThemeSettingsPanel(
    theme: ReaderTheme,
    expandedLayout: Boolean,
    onChange: (ReaderTheme) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Tema")
        Spacer(Modifier.height(8.dp))
        ThemeType.entries.forEach { type ->
            FilterChip(
                selected = theme.type == type,
                onClick = { onChange(theme.copy(type = type)) },
                label = {
                    Text(
                        when (type) {
                            ThemeType.LIGHT -> "Claro"
                            ThemeType.SEPIA -> "Sepia"
                            ThemeType.NIGHT -> "Noche"
                        }
                    )
                },
                modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
            )
        }
        Text("Tipografía")
        FontFamilyType.entries.forEach { font ->
            FilterChip(
                selected = theme.fontFamily == font,
                onClick = { onChange(theme.copy(fontFamily = font)) },
                label = {
                    Text(
                        when (font) {
                            FontFamilyType.SERIF -> "Serif"
                            FontFamilyType.SANS_SERIF -> "Sans"
                            FontFamilyType.MONOSPACE -> "Mono"
                            FontFamilyType.OPENDYSLEXIC -> "OpenDyslexic"
                        }
                    )
                },
                modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
            )
        }
        Text("Tamaño ${theme.fontSizeSp}sp")
        Slider(
            value = theme.fontSizeSp.toFloat(),
            onValueChange = { onChange(theme.copy(fontSizeSp = it.toInt())) },
            valueRange = ReaderTheme.MIN_FONT_SIZE_SP.toFloat()..ReaderTheme.MAX_FONT_SIZE_SP.toFloat()
        )
        Text("Interlineado ${"%.1f".format(theme.lineHeightMultiplier)}x")
        Slider(
            value = theme.lineHeightMultiplier,
            onValueChange = { onChange(theme.copy(lineHeightMultiplier = it)) },
            valueRange = ReaderTheme.MIN_LINE_HEIGHT..ReaderTheme.MAX_LINE_HEIGHT
        )
        if (expandedLayout) {
            Text("Ancho máximo ${theme.maxContainerWidthRem} rem")
            Slider(
                value = theme.maxContainerWidthRem.toFloat(),
                onValueChange = { onChange(theme.copy(maxContainerWidthRem = it.toInt())) },
                valueRange = ReaderTheme.MIN_WIDTH_REM.toFloat()..ReaderTheme.MAX_WIDTH_REM.toFloat()
            )
        }
    }
}
