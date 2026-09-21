package org.openreader.feature.downloader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.openreader.core.model.DownloadState
import org.openreader.core.model.VoiceGender
import org.openreader.core.model.VoiceModel

@Composable
fun DownloaderScreen(
    state: DownloaderUiState,
    selectedVoiceId: String,
    onDownload: (String) -> Unit,
    onSelectVoice: (VoiceModel) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                "Voces",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                "Elige una voz del sistema (ya instalada en el teléfono) o descarga un modelo neuronal Piper. Puedes borrar los modelos que no uses.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            DownloadStatus(state.download)
        }
        item {
            Text("Motor del sistema", style = MaterialTheme.typography.titleMedium)
            Text(
                "Voces que ya trae Android. No ocupan descarga.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (state.systemVoices.isEmpty()) {
            item { Text("Cargando voces del sistema…") }
        } else {
            items(state.systemVoices, key = { it.id }) { voice ->
                VoiceRow(
                    voice = voice,
                    selected = selectedVoiceId == voice.id,
                    busy = false,
                    subtitle = voice.languageCode,
                    onDownload = {},
                    onSelect = { onSelectVoice(voice) },
                    onDelete = null
                )
            }
        }
        item {
            Text(
                "Modelos neuronales (Piper)",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 12.dp)
            )
            Text(
                "Mujer, hombre y varios idiomas. Se guardan solo en este dispositivo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        items(state.neuralVoices, key = { it.id }) { voice ->
            VoiceRow(
                voice = voice,
                selected = selectedVoiceId == voice.id,
                busy = state.download is DownloadState.InProgress ||
                    state.download is DownloadState.Verifying,
                subtitle = "${voice.languageCode} · ${genderLabel(voice.gender)}",
                onDownload = { onDownload(voice.id) },
                onSelect = { onSelectVoice(voice) },
                onDelete = if (voice.isDownloaded) ({ onDelete(voice.id) }) else null
            )
        }
    }
}

@Composable
private fun DownloadStatus(state: DownloadState) {
    when (state) {
        is DownloadState.InProgress -> {
            val progress = if (state.totalBytes > 0) {
                state.bytesRead.toFloat() / state.totalBytes.toFloat()
            } else 0f
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
            Text("Descargando ${state.voiceId}…", style = MaterialTheme.typography.labelMedium)
        }
        is DownloadState.Verifying -> Text("Comprobando ${state.voiceId}…")
        is DownloadState.Failed -> Text(
            "Error: ${state.message}",
            color = MaterialTheme.colorScheme.error
        )
        is DownloadState.Completed -> Text("Instalado: ${state.voiceId}")
        DownloadState.Idle -> Unit
    }
}

@Composable
private fun VoiceRow(
    voice: VoiceModel,
    selected: Boolean,
    busy: Boolean,
    subtitle: String,
    onDownload: () -> Unit,
    onSelect: () -> Unit,
    onDelete: (() -> Unit)?
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = if (selected) "${voice.name}  ·  en uso" else voice.name,
            style = MaterialTheme.typography.titleMedium
        )
        Text(subtitle, style = MaterialTheme.typography.bodySmall)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 6.dp)
        ) {
            when {
                voice.engineType == org.openreader.core.model.TTSEngineType.SYSTEM -> {
                    Button(onClick = onSelect, enabled = !selected) {
                        Text(if (selected) "Seleccionada" else "Usar")
                    }
                }
                voice.isDownloaded -> {
                    Button(onClick = onSelect, enabled = !busy && !selected) {
                        Text(if (selected) "Seleccionada" else "Usar")
                    }
                    if (onDelete != null) {
                        OutlinedButton(onClick = onDelete, enabled = !busy) { Text("Eliminar") }
                    }
                }
                else -> OutlinedButton(onClick = onDownload, enabled = !busy) { Text("Descargar") }
            }
        }
    }
}

private fun genderLabel(gender: VoiceGender): String = when (gender) {
    VoiceGender.FEMALE -> "mujer"
    VoiceGender.MALE -> "hombre"
    VoiceGender.MIXED -> "varias voces"
    VoiceGender.UNKNOWN -> "—"
}
