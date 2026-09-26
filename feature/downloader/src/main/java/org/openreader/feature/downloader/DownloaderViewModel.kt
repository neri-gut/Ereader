package org.openreader.feature.downloader

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.openreader.core.model.DownloadState
import org.openreader.core.model.TTSEngineType
import org.openreader.core.model.VoiceGender
import org.openreader.core.model.VoiceModel
import java.io.File
import org.openreader.core.tts.TtsController

data class DownloaderUiState(
    val neuralVoices: List<VoiceModel> = emptyList(),
    val systemVoices: List<VoiceModel> = emptyList(),
    val download: DownloadState = DownloadState.Idle,
    val playingSampleKey: String? = null,
    val showAddSheet: Boolean = false,
    val addError: String? = null
)

class DownloaderViewModel(
    private val downloader: ModelDownloader,
    private val ttsController: TtsController
) : ViewModel() {
    private val refresh = MutableStateFlow(0)
    private val playingSample = MutableStateFlow<String?>(null)
    private val showAdd = MutableStateFlow(false)
    private val addError = MutableStateFlow<String?>(null)
    private val samplePlayer = VoiceSamplePlayer()
    @Volatile private var previewGen: Int = 0
    private val userCatalog = UserVoiceCatalog(
        downloader.modelsDirectory().parentFile ?: downloader.modelsDirectory()
    )

    init {
        VoiceCatalog.setUserPacks(userCatalog.load())
        viewModelScope.launch {
            ttsController.awaitSystemReady()
            refresh.value += 1
        }
        viewModelScope.launch {
            downloader.state.collect { refresh.value += 1 }
        }
    }

    val uiState: StateFlow<DownloaderUiState> = combine(
        downloader.state,
        refresh,
        playingSample,
        showAdd,
        addError
    ) { download, _, sampleUrl, adding, error ->
        DownloaderUiState(
            neuralVoices = VoiceCatalog.all().map { pack ->
                VoiceModel(
                    id = pack.id,
                    name = pack.displayName,
                    languageCode = pack.languageCode,
                    engineType = TTSEngineType.SHERPA_ONNX_PIPER,
                    gender = pack.gender,
                    modelFileName = pack.onnxFileName,
                    tokensFileName = pack.tokensFileName,
                    isDownloaded = downloader.isInstalled(pack),
                    canDelete = downloader.hasLocalFiles(pack.id),
                    speakerCount = pack.speakerCount,
                    speakerLabels = pack.speakerLabels
                )
            },
            systemVoices = ttsController.systemVoices(),
            download = download,
            playingSampleKey = sampleUrl,
            showAddSheet = adding,
            addError = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DownloaderUiState()
    )

    fun startDownload(context: Context, voiceId: String) {
        ModelDownloadService.start(context, voiceId)
    }

    fun openAddSheet() {
        addError.value = null
        showAdd.value = true
    }

    fun closeAddSheet() {
        showAdd.value = false
        addError.value = null
    }

    fun addPiperVoice(rawId: String, displayName: String, gender: VoiceGender) {
        val pack = PiperVoiceId.parse(rawId, displayName, gender)
        if (pack == null) {
            addError.value = "El id no es un pack Piper válido"
            return
        }
        if (VoiceCatalog.piperVoices.none { it.id == pack.id }) {
            userCatalog.upsert(pack)
        }
        addError.value = null
        showAdd.value = false
        refresh.value += 1
    }

    fun importLocal(context: Context, uri: Uri, displayName: String) {
        viewModelScope.launch {
            runCatching {
                val name = displayName.ifBlank {
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
                    }
                } ?: "voz-local"
                val id = name.substringBeforeLast('.')
                    .substringBeforeLast(".tar")
                    .replace(Regex("[^A-Za-z0-9_-]"), "_")
                    .take(80)
                    .ifBlank { "voz-local" }
                val temp = File(context.cacheDir, name.substringAfterLast('/'))
                context.contentResolver.openInputStream(uri)?.use { input ->
                    temp.outputStream().use { output -> input.copyTo(output) }
                } ?: error("No se pudo leer el archivo")
                val parsed = PiperVoiceId.parse(id)
                val pack = parsed?.copy(displayName = displayName.ifBlank { parsed.displayName }, archiveUrl = "")
                    ?: VoicePack(
                        id = id,
                        displayName = displayName.ifBlank { id },
                        languageCode = id.substringBefore('_').replace('_', '-').ifBlank { "und" },
                        gender = VoiceGender.UNKNOWN,
                        quality = "local",
                        archiveUrl = "",
                        onnxFileName = if (name.endsWith(".onnx")) name else "$id.onnx"
                    )
                downloader.importLocal(temp, pack)
                temp.delete()
                userCatalog.upsert(pack)
                showAdd.value = false
                addError.value = null
                refresh.value += 1
            }.onFailure { error ->
                addError.value = error.message ?: "No se pudo importar el archivo"
            }
        }
    }

    fun delete(voiceId: String) {
        stopPreview()
        userCatalog.remove(voiceId)
        downloader.delete(voiceId)
        refresh.value += 1
    }

    fun preview(voiceId: String, speakerId: Int = 0) {
        val pack = VoiceCatalog.byId(voiceId) ?: return
        val key = sampleKey(voiceId, speakerId)
        if (playingSample.value == key) {
            stopPreview()
            return
        }
        val generation = ++previewGen
        samplePlayer.stop()
        ttsController.cancelPreview()
        ttsController.stop()
        if (downloader.isInstalled(pack)) {
            playingSample.value = key
            ttsController.previewInstalled(
                java.io.File(downloader.modelsDirectory(), pack.id),
                speakerId
            ) {
                if (previewGen == generation) playingSample.value = null
            }
            return
        }
        val url = pack.sampleUrl(speakerId)
        if (url.isBlank()) {
            playingSample.value = null
            return
        }
        playingSample.value = key
        samplePlayer.play(
            url = url,
            onDone = { if (previewGen == generation) playingSample.value = null },
            onError = { if (previewGen == generation) playingSample.value = null }
        )
    }

    fun stopPreview() {
        previewGen++
        samplePlayer.stop()
        ttsController.cancelPreview()
        playingSample.value = null
    }

    override fun onCleared() {
        samplePlayer.stop()
        super.onCleared()
    }

    class Factory(
        private val downloader: ModelDownloader,
        private val ttsController: TtsController
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DownloaderViewModel(downloader, ttsController) as T
        }
    }
}
