package org.openreader.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.openreader.feature.reader.rem
import org.openreader.core.model.FontFamilyType
import org.openreader.core.model.ReaderTheme
import org.openreader.core.model.TTSConfig
import org.openreader.core.model.TTSEngineType
import org.openreader.core.model.ThemeType

@Composable
fun SettingsScreen(
    theme: ReaderTheme,
    ttsConfig: TTSConfig,
    neuralReady: Boolean,
    expanded: Boolean = false,
    onThemeChange: (ReaderTheme) -> Unit,
    onTtsChange: (TTSConfig) -> Unit,
    onOpenVoices: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Column(
        Modifier
            .then(if (expanded) Modifier.widthIn(max = 48.rem) else Modifier.fillMaxWidth())
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        SettingsSection("Apariencia")
        Text("Tema de lectura", style = MaterialTheme.typography.bodyMedium)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
        ) {
            ThemeType.entries.forEach { type ->
                FilterChip(
                    selected = theme.type == type,
                    onClick = { onThemeChange(theme.copy(type = type)) },
                    label = {
                        Text(
                            when (type) {
                                ThemeType.LIGHT -> "Claro"
                                ThemeType.SEPIA -> "Sepia"
                                ThemeType.NIGHT -> "Noche"
                            }
                        )
                    }
                )
            }
        }
        Text("Tipografía", style = MaterialTheme.typography.bodyMedium)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .padding(top = 8.dp, bottom = 24.dp)
                .fillMaxWidth()
        ) {
            FontFamilyType.entries.forEach { font ->
                FilterChip(
                    selected = theme.fontFamily == font,
                    onClick = { onThemeChange(theme.copy(fontFamily = font)) },
                    label = {
                        Text(
                            when (font) {
                                FontFamilyType.SERIF -> "Serif"
                                FontFamilyType.SANS_SERIF -> "Sans"
                                FontFamilyType.MONOSPACE -> "Mono"
                                FontFamilyType.OPENDYSLEXIC -> "OpenDyslexic"
                            }
                        )
                    }
                )
            }
        }

        SettingsSection("Lectura")
        Text("Tamaño de letra ${theme.fontSizeSp} sp")
        Slider(
            value = theme.fontSizeSp.toFloat(),
            onValueChange = { onThemeChange(theme.copy(fontSizeSp = it.toInt())) },
            valueRange = ReaderTheme.MIN_FONT_SIZE_SP.toFloat()..ReaderTheme.MAX_FONT_SIZE_SP.toFloat()
        )
        Text("Interlineado ${"%.1f".format(theme.lineHeightMultiplier)}×")
        Slider(
            value = theme.lineHeightMultiplier,
            onValueChange = { onThemeChange(theme.copy(lineHeightMultiplier = it)) },
            valueRange = ReaderTheme.MIN_LINE_HEIGHT..ReaderTheme.MAX_LINE_HEIGHT
        )
        Text("Ancho máximo en tablet ${theme.maxContainerWidthRem} rem")
        Slider(
            value = theme.maxContainerWidthRem.toFloat(),
            onValueChange = { onThemeChange(theme.copy(maxContainerWidthRem = it.toInt())) },
            valueRange = ReaderTheme.MIN_WIDTH_REM.toFloat()..ReaderTheme.MAX_WIDTH_REM.toFloat(),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        SettingsSection("Audio")
        Text(
            "Las voces se organizan en Menú → Voces: listas o catálogo, idioma, mujer/hombre y sistema o neuronal.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text("Motor de voz")
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
        ) {
            FilterChip(
                selected = ttsConfig.engineType == TTSEngineType.SYSTEM,
                onClick = { onTtsChange(ttsConfig.copy(engineType = TTSEngineType.SYSTEM)) },
                label = { Text("Sistema") }
            )
            FilterChip(
                selected = ttsConfig.engineType == TTSEngineType.SHERPA_ONNX_PIPER,
                onClick = { onTtsChange(ttsConfig.copy(engineType = TTSEngineType.SHERPA_ONNX_PIPER)) },
                label = { Text(if (neuralReady) "Neuronal" else "Neuronal (sin modelo)") }
            )
        }
        Text("Velocidad ${"%.1f".format(ttsConfig.speechRate)}×")
        Slider(
            value = ttsConfig.speechRate,
            onValueChange = { onTtsChange(ttsConfig.copy(speechRate = it)) },
            valueRange = TTSConfig.MIN_SPEECH_RATE..TTSConfig.MAX_SPEECH_RATE
        )
        Button(onClick = onOpenVoices, modifier = Modifier.padding(bottom = 24.dp)) {
            Text("Abrir catálogo de voces")
        }

        SettingsSection("Acerca de")
        Text("OpenReader es un lector PDF offline, sin cuentas ni rastreo.")
        Spacer(Modifier.height(8.dp))
        Text("Versión 1.0.0 · Licencia MIT")
        Spacer(Modifier.height(24.dp))
    }
    }
}

@Composable
private fun SettingsSection(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
    )
}
