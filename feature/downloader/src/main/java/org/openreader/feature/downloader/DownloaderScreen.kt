package org.openreader.feature.downloader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import org.openreader.core.model.VoiceModel

@Composable
fun DownloaderScreen(
    state: DownloaderUiState,
    onDownload: (String) -> Unit,
    onSelectVoice: (VoiceModel) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Voces neuronales (Piper)",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        Text(
            text = "Los modelos se guardan en la memoria privada del dispositivo y se verifican con SHA-256.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        DownloadStatus(state.download)
        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(state.voices, key = { it.id }) { voice ->
                VoiceRow(
                    voice = voice,
                    busy = state.download is DownloadState.InProgress ||
                        state.download is DownloadState.Verifying,
                    onDownload = { onDownload(voice.id) },
                    onSelect = { onSelectVoice(voice) }
                )
            }
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
                    .padding(bottom = 12.dp)
            )
            Text(
                text = "Descargando ${state.voiceId}…",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
        is DownloadState.Verifying -> Text(
            "Comprobando integridad de ${state.voiceId}…",
            modifier = Modifier.padding(bottom = 12.dp)
        )
        is DownloadState.Failed -> Text(
            "Error: ${state.message}",
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        is DownloadState.Completed -> Text(
            "Instalado: ${state.voiceId}",
            modifier = Modifier.padding(bottom = 12.dp)
        )
        DownloadState.Idle -> Unit
    }
}

@Composable
private fun VoiceRow(
    voice: VoiceModel,
    busy: Boolean,
    onDownload: () -> Unit,
    onSelect: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 720.dp)
    ) {
        Text(voice.name, style = MaterialTheme.typography.titleMedium)
        Text(voice.languageCode, style = MaterialTheme.typography.bodySmall)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            if (voice.isDownloaded) {
                Button(onClick = onSelect, enabled = !busy) { Text("Usar voz") }
            } else {
                OutlinedButton(onClick = onDownload, enabled = !busy) { Text("Descargar") }
            }
        }
    }
}
