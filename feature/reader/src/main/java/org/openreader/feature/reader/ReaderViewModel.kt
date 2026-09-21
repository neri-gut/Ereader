package org.openreader.feature.reader

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
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
    val controlsVisible: Boolean = true
)

class ReaderViewModel(
    private val extractor: PdfTextExtractor,
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
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(loading = true, error = null, fileName = fileName, contentUri = uri.toString())
            }
            try {
                val hash = context.contentResolver.openInputStream(uri)?.use { hasher.hash(it) }
                    ?: error("No se pudo leer el archivo")
                val saved = progressRepository.getProgress(hash)
                val paragraphs = mutableListOf<ParagraphData>()
                extractor.extract(uri).collect { paragraphs += it }
                val start = saved?.paragraphIndex?.coerceIn(0, paragraphs.lastIndex.coerceAtLeast(0)) ?: 0
                ttsController.updateParagraphs(paragraphs)
                progressRepository.saveProgress(
                    ReadingProgress(
                        fileHash = hash,
                        fileName = fileName,
                        paragraphIndex = start,
                        charOffset = saved?.charOffset ?: 0,
                        totalParagraphs = paragraphs.size
                    ),
                    uri.toString()
                )
                _uiState.update {
                    it.copy(
                        fileHash = hash,
                        paragraphs = paragraphs,
                        currentParagraph = start,
                        loading = false
                    )
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(loading = false, error = error.message ?: "Error al abrir el PDF")
                }
            }
        }
    }

    fun toggleControls() {
        _uiState.update { it.copy(controlsVisible = !it.controlsVisible) }
    }

    fun hideControls() {
        _uiState.update { it.copy(controlsVisible = false) }
    }

    fun toggleNativePdf() {
        _uiState.update { it.copy(showNativePdf = !it.showNativePdf) }
    }

    fun selectParagraph(index: Int) {
        _uiState.update { it.copy(currentParagraph = index) }
        persistProgress(index, 0)
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
        val state = _uiState.value
        val config = ttsConfig.value
        if (config.engineType == TTSEngineType.SHERPA_ONNX_PIPER &&
            !ttsController.isNeuralModelReady(config.selectedVoiceId)
        ) {
            _uiState.update { it.copy(error = "NEEDS_VOICE") }
            return
        }
        ttsController.play(state.currentParagraph)
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
                hasher,
                progressRepository,
                preferencesRepository,
                ttsController,
                modelDownloader
            ) as T
        }
    }
}
