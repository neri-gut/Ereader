package org.openreader.feature.downloader

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.openreader.core.model.DownloadState
import org.openreader.core.model.TTSEngineType
import org.openreader.core.model.VoiceGender
import org.openreader.core.model.VoiceModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloaderScreen(
    state: DownloaderUiState,
    selectedVoiceId: String,
    selectedSpeakerId: Int,
    onDownload: (String) -> Unit,
    onSelectVoice: (VoiceModel, Int) -> Unit,
    onDelete: (String) -> Unit,
    onPreview: (String, Int) -> Unit,
    onAddPiper: (String, String, VoiceGender) -> Unit,
    onImportLocal: (Uri) -> Unit,
    onOpenAdd: () -> Unit,
    onCloseAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    var source by remember { mutableStateOf<VoiceSource?>(null) }
    var language by remember { mutableStateOf<String?>(null) }
    var gender by remember { mutableStateOf<VoiceGender?>(null) }
    var speakerFor by remember { mutableStateOf<VoiceModel?>(null) }
    var detail by remember { mutableStateOf<VoiceModel?>(null) }
    val browse = remember(state.neuralVoices, state.systemVoices, source, language, gender, selectedVoiceId) {
        browseVoices(
            neural = state.neuralVoices,
            system = state.systemVoices,
            source = source,
            language = language,
            gender = gender,
            activeId = selectedVoiceId
        )
    }
    val active = (state.neuralVoices + state.systemVoices).find { it.id == selectedVoiceId }
    val drilled = source != null
    fun stepBack() {
        val next = popBrowse(
            state.neuralVoices,
            state.systemVoices,
            BrowseSelection(source, language, gender)
        )
        source = next.source
        language = next.language
        gender = next.gender
    }
    BackHandler(enabled = drilled) { stepBack() }

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 768.dp).fillMaxSize()) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                Text(
                    text = active?.let { "En uso · ${it.name}" } ?: "En uso · voz del sistema",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.clickable(enabled = active != null) {
                        val voice = active ?: return@clickable
                        source = if (voice.engineType == TTSEngineType.SYSTEM) {
                            VoiceSource.SYSTEM
                        } else {
                            VoiceSource.NEURAL
                        }
                        language = languageKey(voice.languageCode)
                        gender = voice.gender
                    }
                )
                if (browse.trail.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { stepBack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                        }
                        Text(browse.trail.joinToString(" · "), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                when (val step = browse.step) {
                    is BrowseStep.Sources -> {
                        if (step.choices.isEmpty()) {
                            item { Text("No hay voces en este dispositivo.") }
                        }
                        items(step.choices, key = { it.id }) { choice ->
                            ChoiceRow(choice.label, choice.count) {
                                source = if (choice.id == "system") VoiceSource.SYSTEM else VoiceSource.NEURAL
                                language = null
                                gender = null
                            }
                        }
                    }
                    is BrowseStep.Languages -> {
                        items(step.choices, key = { it.id }) { choice ->
                            ChoiceRow(choice.label, choice.count) {
                                language = choice.id
                                gender = null
                            }
                        }
                    }
                    is BrowseStep.Genders -> {
                        items(step.choices, key = { it.id }) { choice ->
                            ChoiceRow(choice.label, choice.count) {
                                gender = VoiceGender.valueOf(choice.id)
                            }
                        }
                    }
                    is BrowseStep.Voices -> {
                        if (step.voices.isEmpty()) {
                            item { Text("No hay voces en este grupo.") }
                        }
                        items(step.voices, key = { it.id }) { voice ->
                            val playingSpeaker = sampleSpeaker(state.playingSampleKey, voice.id)
                            VoiceCard(
                                voice = voice,
                                active = voice.id == selectedVoiceId,
                                playing = playingSpeaker != null,
                                download = state.download.takeIf { it.voiceId() == voice.id },
                                onDownload = { onDownload(voice.id) },
                                onUse = {
                                    if (voice.speakerCount > 1) speakerFor = voice
                                    else onSelectVoice(voice, 0)
                                },
                                onPreview = if (canPreviewVoice(voice)) {
                                    {
                                        if (voice.speakerCount > 1 && playingSpeaker == null) {
                                            speakerFor = voice
                                        } else {
                                            onPreview(voice.id, playingSpeaker ?: 0)
                                        }
                                    }
                                } else {
                                    null
                                },
                                onDelete = if (voice.engineType != TTSEngineType.SYSTEM &&
                                    (voice.canDelete || voice.isDownloaded)
                                ) {
                                    { onDelete(voice.id) }
                                } else {
                                    null
                                },
                                onDetail = if (voice.engineType != TTSEngineType.SYSTEM &&
                                    (voice.canDelete || voice.isDownloaded)
                                ) {
                                    { detail = voice }
                                } else {
                                    null
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    speakerFor?.let { voice ->
        ModalBottomSheet(
            onDismissRequest = { speakerFor = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                Modifier.padding(start = 12.dp, end = 12.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    voice.name,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.titleLarge
                )
                voice.speakerLabels.ifEmpty { List(voice.speakerCount) { "Hablante ${it + 1}" } }
                    .forEachIndexed { index, label ->
                        val playing = samplePlaying(state.playingSampleKey, voice.id, index)
                        val current = voice.id == selectedVoiceId && index == selectedSpeakerId
                        Row(
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                label,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (canPreviewVoice(voice)) {
                                SampleButton(playing = playing, onClick = { onPreview(voice.id, index) })
                            }
                            Box(Modifier.width(88.dp), contentAlignment = Alignment.CenterEnd) {
                                if (voice.isDownloaded && current) {
                                    Text("En uso", color = MaterialTheme.colorScheme.primary)
                                } else if (voice.isDownloaded) {
                                    TextButton(onClick = {
                                        onSelectVoice(voice, index)
                                        speakerFor = null
                                    }) { Text("Usar") }
                                }
                            }
                        }
                    }
            }
        }
    }
    detail?.let { voice ->
        AlertDialog(
            onDismissRequest = { detail = null },
            confirmButton = { TextButton(onClick = { detail = null }) { Text("Cerrar") } },
            title = { Text(voice.name) },
            text = {
                Text(
                    listOf(voice.id, voiceTraits(voice))
                        .filter { it.isNotBlank() }
                        .joinToString("\n")
                )
            }
        )
    }
    if (state.showAddSheet) {
        AddVoiceSheet(
            error = state.addError,
            onDismiss = onCloseAdd,
            onPiper = onAddPiper,
            onImport = onImportLocal
        )
    }
}

@Composable
private fun ChoiceRow(label: String, count: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Text(
            count.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddVoiceSheet(
    error: String?,
    onDismiss: () -> Unit,
    onPiper: (String, String, VoiceGender) -> Unit,
    onImport: (Uri) -> Unit
) {
    var id by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf(VoiceGender.UNKNOWN) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onImport(uri)
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Añadir voz", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = id,
                onValueChange = { id = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Id Piper") },
                singleLine = true,
                placeholder = { Text("es_ES-mls_9972-low") }
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nombre (opcional)") },
                singleLine = true
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = gender == VoiceGender.FEMALE, onClick = { gender = VoiceGender.FEMALE }, label = { Text("Mujer") })
                FilterChip(selected = gender == VoiceGender.MALE, onClick = { gender = VoiceGender.MALE }, label = { Text("Hombre") })
            }
            if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
            Button(onClick = { onPiper(id, name, gender) }, modifier = Modifier.fillMaxWidth()) {
                Text("Guardar id")
            }
            TextButton(
                onClick = { picker.launch(arrayOf("*/*")) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Archivo local") }
        }
    }
}

@Composable
private fun VoiceCard(
    voice: VoiceModel,
    active: Boolean,
    playing: Boolean,
    download: DownloadState?,
    onDownload: () -> Unit,
    onUse: () -> Unit,
    onPreview: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    onDetail: (() -> Unit)?
) {
    var menu by remember { mutableStateOf(false) }
    val traits = voiceTraits(voice)
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    voice.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    buildString {
                        if (traits.isNotEmpty()) append(traits)
                        if (active) {
                            if (traits.isNotEmpty()) append(" · ")
                            append("En uso")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (onPreview != null) {
                SampleButton(playing = playing, onClick = onPreview)
            }
            val primary = when {
                voice.engineType != TTSEngineType.SYSTEM && !voice.isDownloaded -> "Descargar" to onDownload
                voice.speakerCount > 1 && voice.isDownloaded -> "Elegir" to onUse
                active -> null
                else -> "Usar" to onUse
            }
            if (primary != null) {
                TextButton(
                    onClick = primary.second,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) { Text(primary.first) }
            }
            if (onDelete != null || onDetail != null) {
                Box {
                    IconButton(onClick = { menu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Más")
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        if (onDetail != null) {
                            DropdownMenuItem(text = { Text("Detalle") }, onClick = { menu = false; onDetail() })
                        }
                        if (onDelete != null) {
                            DropdownMenuItem(text = { Text("Eliminar") }, onClick = { menu = false; onDelete() })
                        }
                    }
                }
            }
        }
        if (download is DownloadState.InProgress) {
            val fraction = if (download.totalBytes > 0) {
                download.bytesRead.toFloat() / download.totalBytes.toFloat()
            } else 0f
            LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
        }
        if (download is DownloadState.Verifying) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        if (download is DownloadState.Failed) {
            Text(download.message, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun SampleButton(playing: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        if (playing) {
            Icon(Icons.Filled.Stop, contentDescription = "Detener")
        } else {
            Icon(Icons.Filled.PlayArrow, contentDescription = "Oír")
        }
    }
}

private fun DownloadState.voiceId(): String? = when (this) {
    is DownloadState.InProgress -> voiceId
    is DownloadState.Verifying -> voiceId
    is DownloadState.Failed -> voiceId
    is DownloadState.Completed -> voiceId
    DownloadState.Idle -> null
}
