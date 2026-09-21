package org.openreader.app

import android.net.Uri
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import org.openreader.app.ui.SettingsScreen
import org.openreader.core.model.TTSEngineType
import org.openreader.feature.downloader.DownloaderScreen
import org.openreader.feature.downloader.DownloaderViewModel
import org.openreader.feature.library.LibraryScreen
import org.openreader.feature.library.LibraryViewModel
import org.openreader.feature.reader.ReaderScreen
import org.openreader.feature.reader.ReaderViewModel

private enum class AppTab {
    LIBRARY,
    READER,
    VOICES,
    SETTINGS
}

@Composable
fun OpenReaderApp(
    container: AppContainer,
    widthSizeClass: WindowWidthSizeClass
) {
    val context = LocalContext.current
    var tab by remember { mutableStateOf(AppTab.LIBRARY) }
    var hasOpenDocument by remember { mutableStateOf(false) }
    val libraryViewModel: LibraryViewModel = viewModel(factory = container.libraryFactory)
    val readerViewModel: ReaderViewModel = viewModel(factory = container.readerFactory)
    val downloaderViewModel: DownloaderViewModel = viewModel(factory = container.downloaderFactory)
    val documents by libraryViewModel.documents.collectAsStateWithLifecycle()
    val theme by readerViewModel.theme.collectAsStateWithLifecycle()
    val ttsConfig by readerViewModel.ttsConfig.collectAsStateWithLifecycle()

    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        Row(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            NavigationRail(modifier = Modifier.fillMaxHeight()) {
                NavigationRailItem(
                    selected = tab == AppTab.LIBRARY,
                    onClick = { tab = AppTab.LIBRARY },
                    icon = { Text("Lib") },
                    label = { Text("Biblioteca") }
                )
                NavigationRailItem(
                    selected = tab == AppTab.READER,
                    onClick = { if (hasOpenDocument) tab = AppTab.READER },
                    enabled = hasOpenDocument,
                    icon = { Text("Leer") },
                    label = { Text("Lector") }
                )
                NavigationRailItem(
                    selected = tab == AppTab.VOICES,
                    onClick = { tab = AppTab.VOICES },
                    icon = { Text("Voz") },
                    label = { Text("Voces") }
                )
                NavigationRailItem(
                    selected = tab == AppTab.SETTINGS,
                    onClick = { tab = AppTab.SETTINGS },
                    icon = { Text("Cfg") },
                    label = { Text("Ajustes") }
                )
            }
            val contentModifier = Modifier
                .weight(1f)
                .fillMaxSize()
            when (tab) {
                AppTab.LIBRARY -> LibraryScreen(
                    documents = documents,
                    onOpenDocument = { doc ->
                        readerViewModel.open(context, Uri.parse(doc.contentUri), doc.fileName)
                        hasOpenDocument = true
                        tab = AppTab.READER
                    },
                    onImportUri = { uri, name ->
                        libraryViewModel.importDocument(context, uri, name) { doc ->
                            readerViewModel.open(context, Uri.parse(doc.contentUri), doc.fileName)
                            hasOpenDocument = true
                            tab = AppTab.READER
                        }
                    },
                    onImportTree = { uri -> libraryViewModel.importTree(context, uri) },
                    onToggleFavorite = libraryViewModel::toggleFavorite,
                    onRemove = libraryViewModel::remove,
                    modifier = contentModifier
                )
                AppTab.READER -> {
                    val readerState by readerViewModel.uiState.collectAsStateWithLifecycle()
                    val audio by readerViewModel.audioState.collectAsStateWithLifecycle()
                    ReaderScreen(
                        state = readerState,
                        theme = theme,
                        ttsConfig = ttsConfig,
                        audioState = audio,
                        widthSizeClass = widthSizeClass,
                        neuralReady = readerViewModel.availableNeuralVoices().isNotEmpty(),
                        onToggleNative = readerViewModel::toggleNativePdf,
                        onPdfPageMode = readerViewModel::setPdfPageMode,
                        onTtsChange = readerViewModel::updateTts,
                        onPlay = readerViewModel::play,
                        onPause = readerViewModel::pause,
                        onResume = readerViewModel::resume,
                        onPrevious = readerViewModel::skipPrevious,
                        onNext = readerViewModel::skipNext,
                        onOpenVoices = { tab = AppTab.VOICES },
                        onToggleTtsBar = readerViewModel::toggleTtsBar,
                        onSelectParagraph = readerViewModel::selectParagraph,
                        modifier = contentModifier
                    )
                }
                AppTab.VOICES -> {
                    val downloaderState by downloaderViewModel.uiState.collectAsStateWithLifecycle()
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
                            tab = if (hasOpenDocument) AppTab.READER else AppTab.LIBRARY
                        },
                        modifier = contentModifier
                    )
                }
                AppTab.SETTINGS -> SettingsScreen(
                    theme = theme,
                    ttsConfig = ttsConfig,
                    neuralReady = readerViewModel.availableNeuralVoices().isNotEmpty(),
                    onThemeChange = readerViewModel::updateTheme,
                    onTtsChange = readerViewModel::updateTts,
                    onOpenVoices = { tab = AppTab.VOICES },
                    modifier = contentModifier
                )
            }
        }
    }
}
