package org.openreader.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.openreader.core.model.FontFamilyType
import org.openreader.core.model.ReaderTheme
import org.openreader.core.model.TTSConfig
import org.openreader.core.model.TTSEngineType
import org.openreader.core.model.ThemeType
import org.openreader.feature.reader.rem

private enum class SettingsPage { HUB, READING, AUDIO, ABOUT }

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
    var page by rememberSaveable { mutableStateOf(SettingsPage.HUB) }
    BackHandler(enabled = page != SettingsPage.HUB) { page = SettingsPage.HUB }

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .then(if (expanded) Modifier.widthIn(max = 48.rem) else Modifier.fillMaxWidth())
                .verticalScroll(rememberScrollState())
        ) {
            when (page) {
                SettingsPage.HUB -> SettingsHub(
                    theme = theme,
                    ttsConfig = ttsConfig,
                    onOpen = { page = it }
                )
                SettingsPage.READING -> ReadingSettings(theme, onThemeChange)
                SettingsPage.AUDIO -> AudioSettings(
                    ttsConfig = ttsConfig,
                    neuralReady = neuralReady,
                    onTtsChange = onTtsChange,
                    onOpenVoices = onOpenVoices
                )
                SettingsPage.ABOUT -> AboutSettings()
            }
        }
    }
}

@Composable
private fun SettingsHub(
    theme: ReaderTheme,
    ttsConfig: TTSConfig,
    onOpen: (SettingsPage) -> Unit
) {
    val engine = when (ttsConfig.engineType) {
        TTSEngineType.SYSTEM -> "Sistema"
        TTSEngineType.SHERPA_ONNX_PIPER -> "Neuronal"
    }
    SettingsRow(
        title = "Lectura",
        detail = "${themeLabel(theme.type)} · ${fontLabel(theme.fontFamily)} · ${theme.fontSizeSp} sp",
        onClick = { onOpen(SettingsPage.READING) }
    )
    SettingsRow(
        title = "Audio",
        detail = "$engine · ${"%.1f".format(ttsConfig.speechRate)}×",
        onClick = { onOpen(SettingsPage.AUDIO) }
    )
    SettingsRow(
        title = "Acerca de",
        detail = "Versión 1.0.0",
        onClick = { onOpen(SettingsPage.ABOUT) }
    )
}

@Composable
private fun SettingsRow(title: String, detail: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(detail) },
        trailingContent = {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
    HorizontalDivider()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReadingSettings(
    theme: ReaderTheme,
    onThemeChange: (ReaderTheme) -> Unit
) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text("Tema", style = MaterialTheme.typography.titleMedium)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
        ) {
            ThemeType.entries.forEach { type ->
                FilterChip(
                    selected = theme.type == type,
                    onClick = { onThemeChange(theme.copy(type = type)) },
                    label = { Text(themeLabel(type)) }
                )
            }
        }
        Text("Tipografía", style = MaterialTheme.typography.titleMedium)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
        ) {
            FontFamilyType.entries.forEach { font ->
                FilterChip(
                    selected = theme.fontFamily == font,
                    onClick = { onThemeChange(theme.copy(fontFamily = font)) },
                    label = { Text(fontLabel(font)) }
                )
            }
        }
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
        Text("Ancho en tablet ${theme.maxContainerWidthRem} rem")
        Slider(
            value = theme.maxContainerWidthRem.toFloat(),
            onValueChange = { onThemeChange(theme.copy(maxContainerWidthRem = it.toInt())) },
            valueRange = ReaderTheme.MIN_WIDTH_REM.toFloat()..ReaderTheme.MAX_WIDTH_REM.toFloat()
        )
        Spacer(Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AudioSettings(
    ttsConfig: TTSConfig,
    neuralReady: Boolean,
    onTtsChange: (TTSConfig) -> Unit,
    onOpenVoices: () -> Unit
) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text("Motor", style = MaterialTheme.typography.titleMedium)
        FlowRow(
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
        Spacer(Modifier.height(8.dp))
    }
    SettingsRow(
        title = "Elegir voz",
        detail = "Idioma, género y modelos",
        onClick = onOpenVoices
    )
}

@Composable
private fun AboutSettings() {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text("OpenReader es un lector de texto offline, sin cuentas ni rastreo.")
        Spacer(Modifier.height(8.dp))
        Text("Versión 1.0.0 · Licencia MIT", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun themeLabel(type: ThemeType): String = when (type) {
    ThemeType.LIGHT -> "Claro"
    ThemeType.SEPIA -> "Sepia"
    ThemeType.NIGHT -> "Noche"
}

private fun fontLabel(font: FontFamilyType): String = when (font) {
    FontFamilyType.SERIF -> "Serif"
    FontFamilyType.SANS_SERIF -> "Sans"
    FontFamilyType.MONOSPACE -> "Mono"
    FontFamilyType.OPENDYSLEXIC -> "OpenDyslexic"
}
