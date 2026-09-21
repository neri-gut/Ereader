package org.openreader.app

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.openreader.core.model.TTSEngineType
import org.openreader.feature.downloader.DownloaderScreen
import org.openreader.feature.downloader.DownloaderViewModel
import org.openreader.feature.library.LibraryScreen
import org.openreader.feature.library.LibraryViewModel
import org.openreader.feature.reader.ReaderScreen
import org.openreader.feature.reader.ReaderViewModel

sealed interface AppRoute {
    data object Library : AppRoute
    data class Reader(val uri: String, val fileName: String) : AppRoute
    data object Voices : AppRoute
}

@Composable
fun OpenReaderApp(
    container: AppContainer,
    widthSizeClass: WindowWidthSizeClass
) {
    val context = LocalContext.current
    var route by remember { mutableStateOf<AppRoute>(AppRoute.Library) }
    val libraryViewModel: LibraryViewModel = viewModel(factory = container.libraryFactory)
    val readerViewModel: ReaderViewModel = viewModel(factory = container.readerFactory)
    val downloaderViewModel: DownloaderViewModel = viewModel(factory = container.downloaderFactory)
    val documents by libraryViewModel.documents.collectAsStateWithLifecycle()

    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        val screenModifier = Modifier
            .fillMaxSize()
            .padding(padding)

        when (route) {
            AppRoute.Library -> LibraryScreen(
                documents = documents,
                onOpenDocument = { doc ->
                    readerViewModel.open(context, Uri.parse(doc.contentUri), doc.fileName)
                    route = AppRoute.Reader(doc.contentUri, doc.fileName)
                },
                onImportUri = { uri, name -> libraryViewModel.importDocument(context, uri, name) },
                onImportTree = { uri -> libraryViewModel.importTree(context, uri) },
                onRemove = libraryViewModel::remove,
                modifier = screenModifier
            )
            is AppRoute.Reader -> {
                val readerState by readerViewModel.uiState.collectAsStateWithLifecycle()
                val theme by readerViewModel.theme.collectAsStateWithLifecycle()
                val ttsConfig by readerViewModel.ttsConfig.collectAsStateWithLifecycle()
                val audio by readerViewModel.audioState.collectAsStateWithLifecycle()
                ReaderScreen(
                    state = readerState,
                    theme = theme,
                    ttsConfig = ttsConfig,
                    audioState = audio,
                    widthSizeClass = widthSizeClass,
                    neuralReady = readerViewModel.availableNeuralVoices().isNotEmpty(),
                    onBack = { route = AppRoute.Library },
                    onToggleNative = readerViewModel::toggleNativePdf,
                    onToggleControls = readerViewModel::toggleControls,
                    onHideControls = readerViewModel::hideControls,
                    onSelectParagraph = readerViewModel::selectParagraph,
                    onThemeChange = readerViewModel::updateTheme,
                    onTtsChange = readerViewModel::updateTts,
                    onPlay = readerViewModel::play,
                    onPause = readerViewModel::pause,
                    onResume = readerViewModel::resume,
                    onPrevious = readerViewModel::skipPrevious,
                    onNext = readerViewModel::skipNext,
                    onOpenVoices = { route = AppRoute.Voices },
                    modifier = screenModifier
                )
            }
            AppRoute.Voices -> {
                val downloaderState by downloaderViewModel.uiState.collectAsStateWithLifecycle()
                val ttsConfig by readerViewModel.ttsConfig.collectAsStateWithLifecycle()
                DownloaderScreen(
                    state = downloaderState,
                    onDownload = { id -> downloaderViewModel.startDownload(context, id) },
                    onSelectVoice = { voice ->
                        readerViewModel.updateTts(
                            ttsConfig.copy(
                                selectedVoiceId = voice.id,
                                engineType = TTSEngineType.SHERPA_ONNX_PIPER
                            )
                        )
                        route = AppRoute.Library
                    },
                    modifier = screenModifier
                )
            }
        }
    }
}
