package org.openreader.feature.reader

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.openreader.core.database.DocumentIdHasher
import org.openreader.core.database.PreferencesRepository
import org.openreader.core.database.ProgressRepository
import org.openreader.core.model.AudioState
import org.openreader.core.model.ParagraphData
import org.openreader.core.model.ReaderTheme
import org.openreader.core.model.ReadingProgress
import org.openreader.core.model.TTSConfig
import org.openreader.core.model.TTSEngineType
import org.openreader.core.pdf.ParagraphCheckpoint
import org.openreader.core.pdf.ParagraphTextCache
import org.openreader.core.pdf.PdfTextExtractor
import org.openreader.core.tts.TtsController
import org.openreader.feature.downloader.ModelDownloader
import org.openreader.feature.downloader.VoiceCatalog

data class ReaderUiState(
    val fileName: String = "",
    val fileHash: String = "",
    val contentUri: String = "",
    val paragraphs: List<ParagraphData> = emptyList(),
    val currentParagraph: Int = 0,
    val loading: Boolean = false,
    val error: String? = null,
    val showNativePdf: Boolean = false,
    val showTtsBar: Boolean = false,
    val chromeVisible: Boolean = false,
    val localPdfPath: String? = null,
    val extractPage: Int = 0,
    val extractTotal: Int = 0,
    val pdfPageMode: PdfPageMode = PdfPageMode.CONTINUOUS,
    val extractComplete: Boolean = false
)

class ReaderViewModel(
    private val extractor: PdfTextExtractor,
    private val paragraphCache: ParagraphTextCache,
    private val hasher: DocumentIdHasher,
    private val progressRepository: ProgressRepository,
    private val preferencesRepository: PreferencesRepository,
    private val ttsController: TtsController,
    private val modelDownloader: ModelDownloader
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()
    val theme: StateFlow<ReaderTheme> = preferencesRepository.theme.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), ReaderTheme()
    )
    val ttsConfig: StateFlow<TTSConfig> = preferencesRepository.ttsConfig.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), TTSConfig()
    )
    val audioState: StateFlow<AudioState> = ttsController.state
    val screenBrightness: StateFlow<Float> = preferencesRepository.screenBrightness.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), -1f
    )
    private var openJob: Job? = null
    private var activeUri: String? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            ttsConfig.collect { config -> ttsController.updateConfig(config) }
        }
        viewModelScope.launch {
            var lastPersistedParagraph = -1
            ttsController.state.collect { state ->
                when (state) {
                    is AudioState.Playing -> {
                        _uiState.update { it.copy(currentParagraph = state.paragraphIndex) }
                        if (state.paragraphIndex != lastPersistedParagraph) {
                            lastPersistedParagraph = state.paragraphIndex
                            persistProgress(state.paragraphIndex, state.startCharOffset)
                        }
                    }
                    is AudioState.Paused -> {
                        _uiState.update { it.copy(currentParagraph = state.paragraphIndex) }
                        persistProgress(state.paragraphIndex, charOffset(state))
                    }
                    is AudioState.Synthesizing -> {
                        _uiState.update { it.copy(currentParagraph = state.paragraphIndex) }
                    }
                    else -> Unit
                }
            }
        }
    }

    fun open(context: Context, uri: Uri, fileName: String) {
        val uriKey = uri.toString()
        val state = _uiState.value
        if (state.contentUri == uriKey && state.extractComplete && state.paragraphs.isNotEmpty()) return
        if (activeUri == uriKey && openJob?.isActive == true) return
        openJob?.cancel()
        ttsController.stop()
        activeUri = uriKey
        openJob = viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            val switching = _uiState.value.contentUri != uriKey
            _uiState.update {
                it.copy(
                    loading = true,
                    extractComplete = false,
                    error = null,
                    fileName = fileName,
                    contentUri = uriKey,
                    showNativePdf = false,
                    chromeVisible = true,
                    paragraphs = if (switching) emptyList() else it.paragraphs,
                    localPdfPath = if (switching) null else it.localPdfPath,
                    extractPage = if (switching) 0 else it.extractPage,
                    extractTotal = if (switching) 0 else it.extractTotal
                )
            }
            var hash = ""
            val durable = mutableListOf<ParagraphData>()
            val pendingPage = mutableListOf<ParagraphData>()
            var nextPage = 1
            var carry = ""
            var repeated = emptySet<String>()
            var sealed = false
            fun visibleParagraphs(): List<ParagraphData> = durable + pendingPage
            try {
                val file = PdfTextExtractor.materialize(context, uri)
                _uiState.update { it.copy(localPdfPath = file.absolutePath) }
                hash = file.inputStream().use { hasher.hash(it, file.length()) }
                val saved = progressRepository.getProgress(hash)
                val cached = paragraphCache.load(hash)
                if (cached != null) {
                    durable += cached.paragraphs
                    nextPage = cached.nextPage
                    carry = cached.carry
                    repeated = cached.repeated
                    if (durable.isNotEmpty() || cached.done) {
                        showExtracted(
                            hash = hash,
                            fileName = fileName,
                            contentUri = uriKey,
                            paragraphs = durable,
                            saved = saved,
                            loading = !cached.done
                        )
                    }
                    if (cached.done) {
                        sealed = true
                        return@launch
                    }
                }
                extractor.extractFromFile(
                    file = file,
                    startPage = nextPage,
                    startId = durable.size,
                    initialCarry = carry,
                    knownRepeated = if (cached != null) repeated else null,
                    onProgress = { page, total ->
                        _uiState.update { it.copy(extractPage = page, extractTotal = total) }
                    },
                    onCheckpoint = { page, pageCarry, pageRepeated ->
                        durable += pendingPage
                        pendingPage.clear()
                        nextPage = page
                        carry = pageCarry
                        repeated = pageRepeated
                        val finishedPage = page - 1
                        if (finishedPage > 0 && finishedPage % CHECKPOINT_PAGES == 0) {
                            writeCheckpoint(hash, durable, nextPage, carry, repeated, done = false)
                        }
                    }
                ).collect { paragraph ->
                    pendingPage += paragraph
                    val shown = visibleParagraphs()
                    if (shown.size == 1 || shown.size % PUBLISH_EVERY == 0) {
                        publishParagraphs(shown, hash)
                    }
                }
                durable += pendingPage
                pendingPage.clear()
                writeCheckpoint(hash, durable, nextPage, "", repeated, done = true)
                sealed = true
                finishOpen(hash, fileName, uriKey, durable, saved)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        loading = false,
                        chromeVisible = true,
                        error = error.message ?: "Error al extraer el texto. Puedes usar la vista PDF nativa."
                    )
                }
            } finally {
                if (!sealed && hash.isNotBlank() && (durable.isNotEmpty() || nextPage > 1)) {
                    writeCheckpoint(hash, durable, nextPage, carry, repeated, done = false)
                }
            }
        }
    }

    private fun publishParagraphs(paragraphs: List<ParagraphData>, hash: String) {
        val snapshot = paragraphs.toList()
        _uiState.update {
            it.copy(
                fileHash = hash.ifBlank { it.fileHash },
                paragraphs = snapshot,
                loading = true,
                extractComplete = false
            )
        }
        ttsController.updateParagraphs(snapshot)
    }

    private fun showExtracted(
        hash: String,
        fileName: String,
        contentUri: String,
        paragraphs: List<ParagraphData>,
        saved: ReadingProgress?,
        loading: Boolean
    ) {
        val snapshot = paragraphs.toList()
        val lastIndex = (snapshot.size - 1).coerceAtLeast(0)
        val start = saved?.paragraphIndex?.coerceIn(0, lastIndex) ?: 0
        ttsController.updateParagraphs(snapshot)
        _uiState.update {
            it.copy(
                fileHash = hash,
                fileName = fileName,
                contentUri = contentUri,
                paragraphs = snapshot,
                currentParagraph = start,
                loading = loading,
                extractComplete = !loading
            )
        }
    }

    private fun writeCheckpoint(
        hash: String,
        paragraphs: List<ParagraphData>,
        nextPage: Int,
        carry: String,
        repeated: Set<String>,
        done: Boolean
    ) {
        paragraphCache.save(
            hash,
            ParagraphCheckpoint(
                paragraphs = paragraphs.toList(),
                nextPage = nextPage,
                carry = carry,
                repeated = repeated,
                done = done
            )
        )
    }

    private suspend fun finishOpen(
        hash: String,
        fileName: String,
        contentUri: String,
        paragraphs: List<ParagraphData>,
        saved: ReadingProgress?
    ) {
        val snapshot = paragraphs.toList()
        val lastIndex = (snapshot.size - 1).coerceAtLeast(0)
        val alreadyHere = _uiState.value.fileHash == hash &&
            _uiState.value.paragraphs.isNotEmpty() &&
            _uiState.value.currentParagraph in snapshot.indices
        val start = if (alreadyHere) {
            _uiState.value.currentParagraph
        } else {
            saved?.paragraphIndex?.coerceIn(0, lastIndex) ?: 0
        }
        ttsController.updateParagraphs(snapshot)
        progressRepository.saveProgress(
            ReadingProgress(
                fileHash = hash,
                fileName = fileName,
                paragraphIndex = start,
                charOffset = saved?.charOffset ?: 0,
                totalParagraphs = snapshot.size
            ),
            contentUri
        )
        _uiState.update {
            it.copy(
                fileHash = hash,
                paragraphs = snapshot,
                currentParagraph = start,
                loading = false,
                extractComplete = true
            )
        }
    }

    fun toggleChrome() {
        _uiState.update { it.copy(chromeVisible = !it.chromeVisible) }
    }

    fun showChrome() {
        _uiState.update { it.copy(chromeVisible = true) }
    }

    fun updateBrightness(value: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            preferencesRepository.saveScreenBrightness(value)
        }
    }

    fun jumpToPage(page: Int) {
        val index = _uiState.value.paragraphs.indexOfFirst { it.pageNumber >= page }
        if (index >= 0) selectParagraph(index)
    }

    fun hideChrome() {
        _uiState.update { it.copy(chromeVisible = false) }
    }

    fun toggleTtsBar() {
        _uiState.update { it.copy(showTtsBar = !it.showTtsBar, chromeVisible = true) }
    }

    fun toggleNativePdf() {
        _uiState.update { it.copy(showNativePdf = !it.showNativePdf) }
    }

    fun setPdfPageMode(mode: PdfPageMode) {
        _uiState.update { it.copy(pdfPageMode = mode) }
    }

    fun selectParagraph(index: Int) {
        _uiState.update {
            it.copy(
                currentParagraph = index,
                showNativePdf = false
            )
        }
        persistProgress(index, 0)
        when (ttsController.state.value) {
            is AudioState.Playing,
            is AudioState.Synthesizing -> ttsController.pause()
            else -> Unit
        }
    }

    fun updateTheme(theme: ReaderTheme) {
        viewModelScope.launch(Dispatchers.IO) { preferencesRepository.saveTheme(theme) }
    }

    fun updateTts(config: TTSConfig) {
        viewModelScope.launch(Dispatchers.IO) {
            preferencesRepository.saveTtsConfig(config)
            ttsController.updateConfig(config)
        }
    }

    fun play() {
        ttsController.setTrackTitle(_uiState.value.fileName)
        val snapshot = _uiState.value
        val config = ttsConfig.value
        val neuralMissing = config.engineType == TTSEngineType.SHERPA_ONNX_PIPER &&
            !ttsController.isNeuralModelReady(config.selectedVoiceId)
        _uiState.update {
            it.copy(
                showTtsBar = true,
                chromeVisible = true,
                showNativePdf = false,
                error = if (neuralMissing) "NEEDS_VOICE" else null
            )
        }
        if (!neuralMissing) {
            ttsController.play(snapshot.currentParagraph)
        }
    }

    fun voiceLabel(): String {
        val config = ttsConfig.value
        return when (config.engineType) {
            TTSEngineType.SYSTEM -> {
                if (config.selectedVoiceId.startsWith("system:")) {
                    config.selectedVoiceId.removePrefix("system:")
                } else {
                    "predeterminada"
                }
            }
            TTSEngineType.SHERPA_ONNX_PIPER ->
                VoiceCatalog.byId(config.selectedVoiceId)?.displayName ?: config.selectedVoiceId
        }
    }

    fun pause() = ttsController.pause()
    fun resume() = ttsController.resume()
    fun skipNext() = ttsController.skipNext()
    fun skipPrevious() = ttsController.skipPrevious()
    fun stop() = ttsController.stop()

    fun availableNeuralVoices(): List<String> =
        VoiceCatalog.piperVoices.filter { modelDownloader.isInstalled(it) }.map { it.id }

    override fun onCleared() {
        ttsController.stop()
        super.onCleared()
    }

    private fun charOffset(state: AudioState): Int = when (state) {
        is AudioState.Playing -> state.startCharOffset
        else -> 0
    }

    private fun persistProgress(paragraphIndex: Int, charOffset: Int) {
        val snapshot = _uiState.value
        if (snapshot.fileHash.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            progressRepository.saveProgress(
                ReadingProgress(
                    fileHash = snapshot.fileHash,
                    fileName = snapshot.fileName,
                    paragraphIndex = paragraphIndex,
                    charOffset = charOffset,
                    totalParagraphs = snapshot.paragraphs.size
                ),
                snapshot.contentUri
            )
        }
    }

    class Factory(
        private val extractor: PdfTextExtractor,
        private val paragraphCache: ParagraphTextCache,
        private val hasher: DocumentIdHasher,
        private val progressRepository: ProgressRepository,
        private val preferencesRepository: PreferencesRepository,
        private val ttsController: TtsController,
        private val modelDownloader: ModelDownloader
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ReaderViewModel(
                extractor,
                paragraphCache,
                hasher,
                progressRepository,
                preferencesRepository,
                ttsController,
                modelDownloader
            ) as T
        }
    }

    private companion object {
        const val PUBLISH_EVERY = 24
        const val CHECKPOINT_PAGES = 4
    }
}
