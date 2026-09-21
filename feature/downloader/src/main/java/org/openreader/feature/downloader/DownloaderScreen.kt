package org.openreader.feature.downloader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.openreader.core.model.DownloadState
import org.openreader.core.model.TTSEngineType
import org.openreader.core.model.VoiceGender
import org.openreader.core.model.VoiceModel

private enum class VoiceTab { READY, CATALOG, SYSTEM }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DownloaderScreen(
    state: DownloaderUiState,
    selectedVoiceId: String,
    selectedSpeakerId: Int,
    onDownload: (String) -> Unit,
    onSelectVoice: (VoiceModel, Int) -> Unit,
    onDelete: (String) -> Unit,
    onPreview: (String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var tab by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var language by remember { mutableStateOf("all") }
    var gender by remember { mutableStateOf<VoiceGender?>(null) }
    val currentTab = VoiceTab.entries.getOrElse(tab) { VoiceTab.READY }

    val catalog = remember(state.neuralVoices, state.systemVoices, currentTab) {
        when (currentTab) {
            VoiceTab.READY -> state.neuralVoices.filter { it.isDownloaded || it.canDelete }
            VoiceTab.CATALOG -> state.neuralVoices
            VoiceTab.SYSTEM -> state.systemVoices
        }
    }
    val languages = remember(catalog) {
        catalog.map { languageGroup(it.languageCode) }.distinct().sorted()
    }
    val filtered = remember(catalog, query, language, gender, currentTab) {
        catalog.filter { voice ->
            val matchesQuery = query.isBlank() ||
                voice.name.contains(query, ignoreCase = true) ||
                languageGroup(voice.languageCode).contains(query, ignoreCase = true)
            val matchesLanguage = language == "all" || languageGroup(voice.languageCode) == language
            val matchesGender = currentTab == VoiceTab.SYSTEM || gender == null || voice.gender == gender
            matchesQuery && matchesLanguage && matchesGender
        }.sortedWith(
            compareByDescending<VoiceModel> { it.id == selectedVoiceId }
                .thenBy { languageGroup(it.languageCode) }
                .thenBy { it.name.lowercase() }
        )
    }
    val grouped = remember(filtered) { filtered.groupBy { languageGroup(it.languageCode) } }
    val busy = state.download is DownloadState.InProgress ||
        state.download is DownloadState.Verifying

    Column(modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Listas") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Catálogo") })
            Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Sistema") })
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    when (currentTab) {
                        VoiceTab.READY -> "Modelos descargados en este dispositivo. Aquí puedes usarlos o eliminarlos."
                        VoiceTab.CATALOG -> "Solo voces Piper medium o high. Escucha una muestra y descarga la que te guste (repositorios rhasspy y sherpa-onnx)."
                        VoiceTab.SYSTEM -> "Voces que Android ya trae. No ocupan espacio extra."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                DownloadStatus(state.download)
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    placeholder = { Text("Buscar") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
            }
            if (languages.size > 1) {
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = language == "all",
                            onClick = { language = "all" },
                            label = { Text("Todos los idiomas") }
                        )
                        languages.forEach { name ->
                            FilterChip(
                                selected = language == name,
                                onClick = { language = name },
                                label = { Text(name) }
                            )
                        }
                    }
                }
            }
            if (currentTab != VoiceTab.SYSTEM) {
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = gender == null,
                            onClick = { gender = null },
                            label = { Text("Todos") }
                        )
                        FilterChip(
                            selected = gender == VoiceGender.FEMALE,
                            onClick = { gender = VoiceGender.FEMALE },
                            label = { Text("Mujer") }
                        )
                        FilterChip(
                            selected = gender == VoiceGender.MALE,
                            onClick = { gender = VoiceGender.MALE },
                            label = { Text("Hombre") }
                        )
                        FilterChip(
                            selected = gender == VoiceGender.MIXED,
                            onClick = { gender = VoiceGender.MIXED },
                            label = { Text("Varias") }
                        )
                    }
                }
            }
            item {
                Text(
                    "${filtered.size} voces",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (filtered.isEmpty()) {
                item {
                    Text(
                        when (currentTab) {
                            VoiceTab.READY -> "Aún no hay modelos descargados. Ve a Catálogo."
                            else -> "Ninguna voz coincide con la búsqueda."
                        },
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            } else {
                grouped.forEach { (group, voices) ->
                    item(key = "h-$group-${currentTab.name}") {
                        Text(
                            group,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    items(voices, key = { it.id }) { voice ->
                        VoiceRow(
                            voice = voice,
                            selected = selectedVoiceId == voice.id,
                            selectedSpeaker = if (selectedVoiceId == voice.id) selectedSpeakerId else 0,
                            busy = busy,
                            playingSample = state.playingSampleUrl,
                            onDownload = { onDownload(voice.id) },
                            onSelect = { speaker -> onSelectVoice(voice, speaker) },
                            onDelete = if (voice.canDelete || voice.isDownloaded) ({ onDelete(voice.id) }) else null,
                            onPreview = { speaker -> onPreview(voice.id, speaker) }
                        )
                    }
                }
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
                    .padding(vertical = 8.dp)
            )
            Text("Descargando ${state.voiceId}…")
        }
        is DownloadState.Verifying -> Text("Comprobando ${state.voiceId}…")
        is DownloadState.Failed -> Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
        is DownloadState.Completed -> Text("Instalado: ${state.voiceId}")
        DownloadState.Idle -> Unit
    }
}

@Composable
private fun VoiceRow(
    voice: VoiceModel,
    selected: Boolean,
    selectedSpeaker: Int,
    busy: Boolean,
    playingSample: String?,
    onDownload: () -> Unit,
    onSelect: (Int) -> Unit,
    onDelete: (() -> Unit)?,
    onPreview: (Int) -> Unit
) {
    val speakers = if (voice.speakerCount > 1) {
        if (voice.speakerLabels.size == voice.speakerCount) voice.speakerLabels
        else (0 until voice.speakerCount).map { "Hablante ${it + 1}" }
    } else emptyList()
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(
            text = if (selected) "${voice.name}  ·  en uso" else voice.name,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = listOfNotNull(
                if (voice.engineType == TTSEngineType.SYSTEM) "Sistema" else "Neuronal",
                genderLabel(voice.gender).takeIf { it != "—" },
                if (voice.canDelete || voice.isDownloaded) "en el dispositivo" else null
            ).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (speakers.isNotEmpty()) {
            Text(
                "Este modelo incluye ${speakers.size} hablantes. Elige cuál usar:",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 6.dp)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                speakers.forEachIndexed { index, label ->
                    FilterChip(
                        selected = selected && selectedSpeaker == index,
                        onClick = { onSelect(index) },
                        label = { Text(label) }
                    )
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            if (voice.engineType == TTSEngineType.SHERPA_ONNX_PIPER) {
                val previewUrl = VoiceCatalog.byId(voice.id)?.sampleUrl(selectedSpeaker)
                val samplePlaying = playingSample != null && playingSample == previewUrl
                OutlinedButton(onClick = { onPreview(selectedSpeaker) }) {
                    Text(if (samplePlaying) "Detener muestra" else "Escuchar muestra")
                }
            }
            when {
                voice.engineType == TTSEngineType.SYSTEM -> {
                    Button(onClick = { onSelect(0) }, enabled = !selected) {
                        Text(if (selected) "Seleccionada" else "Usar")
                    }
                }
                voice.isDownloaded || voice.canDelete -> {
                    Button(
                        onClick = { onSelect(selectedSpeaker) },
                        enabled = !busy
                    ) { Text(if (selected) "Usar esta" else "Usar") }
                    if (onDelete != null) {
                        OutlinedButton(onClick = onDelete, enabled = !busy) { Text("Eliminar") }
                    }
                }
                else -> OutlinedButton(onClick = onDownload, enabled = !busy) { Text("Descargar") }
            }
        }
    }
}

private fun languageGroup(code: String): String = when (code.substringBefore('-').lowercase()) {
    "es" -> "Español"
    "en" -> "Inglés"
    "pt" -> "Portugués"
    "fr" -> "Francés"
    "it" -> "Italiano"
    "de" -> "Alemán"
    else -> code.ifBlank { "Otro" }
}

private fun genderLabel(gender: VoiceGender): String = when (gender) {
    VoiceGender.FEMALE -> "mujer"
    VoiceGender.MALE -> "hombre"
    VoiceGender.MIXED -> "varias"
    VoiceGender.UNKNOWN -> "—"
}
