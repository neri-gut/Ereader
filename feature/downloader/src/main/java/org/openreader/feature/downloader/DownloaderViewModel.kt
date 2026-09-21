package org.openreader.feature.downloader

import android.content.Context
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
import org.openreader.core.model.VoiceModel
import org.openreader.core.tts.TtsController

data class DownloaderUiState(
    val neuralVoices: List<VoiceModel> = emptyList(),
    val systemVoices: List<VoiceModel> = emptyList(),
    val download: DownloadState = DownloadState.Idle
)

class DownloaderViewModel(
    private val downloader: ModelDownloader,
    private val ttsController: TtsController
) : ViewModel() {
    private val refresh = MutableStateFlow(0)

    init {
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
        refresh
    ) { download, _ ->
        DownloaderUiState(
            neuralVoices = VoiceCatalog.piperVoices.map { pack ->
                VoiceModel(
                    id = pack.id,
                    name = pack.displayName,
                    languageCode = pack.languageCode,
                    engineType = TTSEngineType.SHERPA_ONNX_PIPER,
                    gender = pack.gender,
                    modelFileName = pack.onnxFileName,
                    tokensFileName = pack.tokensFileName,
                    isDownloaded = downloader.isInstalled(pack)
                )
            },
            systemVoices = ttsController.systemVoices(),
            download = download
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DownloaderUiState()
    )

    fun startDownload(context: Context, voiceId: String) {
        ModelDownloadService.start(context, voiceId)
    }

    fun delete(voiceId: String) {
        downloader.delete(voiceId)
        refresh.value += 1
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
