package org.openreader.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.openreader.core.model.FontFamilyType
import org.openreader.core.model.ReaderTheme
import org.openreader.core.model.TTSConfig
import org.openreader.core.model.TTSEngineType
import org.openreader.core.model.ThemeType

private enum class SettingsCategory {
    APPEARANCE,
    READING,
    AUDIO,
    ABOUT
}

@Composable
fun SettingsScreen(
    theme: ReaderTheme,
    ttsConfig: TTSConfig,
    neuralReady: Boolean,
    onThemeChange: (ReaderTheme) -> Unit,
    onTtsChange: (TTSConfig) -> Unit,
    onOpenVoices: () -> Unit,
    modifier: Modifier = Modifier
) {
    var category by remember { mutableStateOf(SettingsCategory.APPEARANCE) }
    Row(modifier.fillMaxSize()) {
        NavigationRail(modifier = Modifier.fillMaxHeight()) {
            Text(
                "Ajustes",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(12.dp)
            )
            NavigationRailItem(
                selected = category == SettingsCategory.APPEARANCE,
                onClick = { category = SettingsCategory.APPEARANCE },
                icon = { Text("Aa") },
                label = { Text("Apariencia") }
            )
            NavigationRailItem(
                selected = category == SettingsCategory.READING,
                onClick = { category = SettingsCategory.READING },
                icon = { Text("Le") },
                label = { Text("Lectura") }
            )
            NavigationRailItem(
                selected = category == SettingsCategory.AUDIO,
                onClick = { category = SettingsCategory.AUDIO },
                icon = { Text("Au") },
                label = { Text("Audio") }
            )
            NavigationRailItem(
                selected = category == SettingsCategory.ABOUT,
                onClick = { category = SettingsCategory.ABOUT },
                icon = { Text("i") },
                label = { Text("Acerca") }
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            when (category) {
                SettingsCategory.APPEARANCE -> AppearanceSettings(theme, onThemeChange)
                SettingsCategory.READING -> ReadingSettings(theme, onThemeChange)
                SettingsCategory.AUDIO -> AudioSettings(ttsConfig, neuralReady, onTtsChange, onOpenVoices)
                SettingsCategory.ABOUT -> AboutSettings()
            }
        }
    }
}

@Composable
private fun AppearanceSettings(theme: ReaderTheme, onChange: (ReaderTheme) -> Unit) {
    Text("Apariencia", style = MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.height(8.dp))
    Text("Tema de lectura")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
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
                }
            )
        }
    }
    Text("Tipografía")
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth()
    ) {
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
                }
            )
        }
    }
}

@Composable
private fun ReadingSettings(theme: ReaderTheme, onChange: (ReaderTheme) -> Unit) {
    Text("Lectura", style = MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.height(12.dp))
    Text("Tamaño de letra ${theme.fontSizeSp} sp")
    Slider(
        value = theme.fontSizeSp.toFloat(),
        onValueChange = { onChange(theme.copy(fontSizeSp = it.toInt())) },
        valueRange = ReaderTheme.MIN_FONT_SIZE_SP.toFloat()..ReaderTheme.MAX_FONT_SIZE_SP.toFloat()
    )
    Text("Interlineado ${"%.1f".format(theme.lineHeightMultiplier)}×")
    Slider(
        value = theme.lineHeightMultiplier,
        onValueChange = { onChange(theme.copy(lineHeightMultiplier = it)) },
        valueRange = ReaderTheme.MIN_LINE_HEIGHT..ReaderTheme.MAX_LINE_HEIGHT
    )
    Text("Ancho máximo en tablet ${theme.maxContainerWidthRem} rem")
    Slider(
        value = theme.maxContainerWidthRem.toFloat(),
        onValueChange = { onChange(theme.copy(maxContainerWidthRem = it.toInt())) },
        valueRange = ReaderTheme.MIN_WIDTH_REM.toFloat()..ReaderTheme.MAX_WIDTH_REM.toFloat()
    )
}

@Composable
private fun AudioSettings(
    config: TTSConfig,
    neuralReady: Boolean,
    onChange: (TTSConfig) -> Unit,
    onOpenVoices: () -> Unit
) {
    Text("Audio", style = MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.height(12.dp))
    Text("Motor de voz")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
        FilterChip(
            selected = config.engineType == TTSEngineType.SYSTEM,
            onClick = { onChange(config.copy(engineType = TTSEngineType.SYSTEM)) },
            label = { Text("Sistema") }
        )
        FilterChip(
            selected = config.engineType == TTSEngineType.SHERPA_ONNX_PIPER,
            onClick = { onChange(config.copy(engineType = TTSEngineType.SHERPA_ONNX_PIPER)) },
            label = { Text(if (neuralReady) "Neuronal" else "Neuronal (sin modelo)") }
        )
    }
    Text("Velocidad ${"%.1f".format(config.speechRate)}×")
    Slider(
        value = config.speechRate,
        onValueChange = { onChange(config.copy(speechRate = it)) },
        valueRange = TTSConfig.MIN_SPEECH_RATE..TTSConfig.MAX_SPEECH_RATE
    )
    Spacer(Modifier.height(8.dp))
    androidx.compose.material3.Button(onClick = onOpenVoices) {
        Text("Gestionar voces neuronales")
    }
}

@Composable
private fun AboutSettings() {
    Text("Acerca de OpenReader", style = MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.height(12.dp))
    Text("Lector PDF offline, sin cuentas ni rastreo.")
    Spacer(Modifier.height(8.dp))
    Text("Versión 1.0.0")
    Spacer(Modifier.width(8.dp))
    Text("Licencia MIT")
}
