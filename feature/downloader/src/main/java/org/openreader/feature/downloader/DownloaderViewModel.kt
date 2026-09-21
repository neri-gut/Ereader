package org.openreader.feature.downloader

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.openreader.core.model.DownloadState
import org.openreader.core.model.TTSEngineType
import org.openreader.core.model.VoiceModel

data class DownloaderUiState(
    val voices: List<VoiceModel> = emptyList(),
    val download: DownloadState = DownloadState.Idle
)

class DownloaderViewModel(
    private val downloader: ModelDownloader
) : ViewModel() {

    val uiState: StateFlow<DownloaderUiState> = downloader.state
        .map { download ->
            DownloaderUiState(
                voices = VoiceCatalog.piperVoices.map { pack ->
                    VoiceModel(
                        id = pack.id,
                        name = pack.displayName,
                        languageCode = pack.languageCode,
                        engineType = TTSEngineType.SHERPA_ONNX_PIPER,
                        modelFileName = pack.onnxFileName,
                        tokensFileName = pack.tokensFileName,
                        isDownloaded = downloader.isInstalled(pack)
                    )
                },
                download = download
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DownloaderUiState(
                voices = VoiceCatalog.piperVoices.map { pack ->
                    VoiceModel(
                        id = pack.id,
                        name = pack.displayName,
                        languageCode = pack.languageCode,
                        engineType = TTSEngineType.SHERPA_ONNX_PIPER,
                        modelFileName = pack.onnxFileName,
                        tokensFileName = pack.tokensFileName,
                        isDownloaded = downloader.isInstalled(pack)
                    )
                }
            )
        )

    fun startDownload(context: Context, voiceId: String) {
        ModelDownloadService.start(context, voiceId)
    }

    class Factory(
        private val downloader: ModelDownloader
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DownloaderViewModel(downloader) as T
        }
    }
}
