package org.openreader.app

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenReaderApp(
    container: AppContainer,
    widthSizeClass: WindowWidthSizeClass
) {
    val context = LocalContext.current
    var tab by remember { mutableStateOf(AppTab.LIBRARY) }
    var hasOpenDocument by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val libraryViewModel: LibraryViewModel = viewModel(factory = container.libraryFactory)
    val readerViewModel: ReaderViewModel = viewModel(factory = container.readerFactory)
    val downloaderViewModel: DownloaderViewModel = viewModel(factory = container.downloaderFactory)
    val documents by libraryViewModel.documents.collectAsStateWithLifecycle()
    val theme by readerViewModel.theme.collectAsStateWithLifecycle()
    val ttsConfig by readerViewModel.ttsConfig.collectAsStateWithLifecycle()

    fun goTo(destination: AppTab) {
        tab = destination
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = tab != AppTab.READER || drawerState.isOpen,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    text = "OpenReader",
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)
                )
                NavigationDrawerItem(
                    label = { Text("Biblioteca") },
                    selected = tab == AppTab.LIBRARY,
                    onClick = { goTo(AppTab.LIBRARY) }
                )
                NavigationDrawerItem(
                    label = { Text("Seguir leyendo") },
                    selected = tab == AppTab.READER,
                    onClick = { if (hasOpenDocument) goTo(AppTab.READER) }
                )
                NavigationDrawerItem(
                    label = { Text("Voces") },
                    selected = tab == AppTab.VOICES,
                    onClick = { goTo(AppTab.VOICES) }
                )
                NavigationDrawerItem(
                    label = { Text("Ajustes") },
                    selected = tab == AppTab.SETTINGS,
                    onClick = { goTo(AppTab.SETTINGS) }
                )
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                if (tab != AppTab.READER) {
                    TopAppBar(
                        title = {
                            Text(
                                when (tab) {
                                    AppTab.LIBRARY -> "Biblioteca"
                                    AppTab.VOICES -> "Voces"
                                    AppTab.SETTINGS -> "Ajustes"
                                    AppTab.READER -> "Lector"
                                }
                            )
                        },
                        navigationIcon = {
                            TextButton(onClick = { scope.launch { drawerState.open() } }) {
                                Text("Menú")
                            }
                        }
                    )
                }
            }
        ) { padding ->
            val contentModifier = Modifier
                .fillMaxSize()
                .padding(padding)
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
                    onImportQuiet = { uri, name ->
                        libraryViewModel.importDocument(context, uri, name)
                    },
                    onImportTree = { uri -> libraryViewModel.importTree(context, uri) },
                    onToggleFavorite = libraryViewModel::toggleFavorite,
                    onRemove = libraryViewModel::remove,
                    showPageTitle = false,
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
                        voiceLabel = readerViewModel.voiceLabel(),
                        onOpenMenu = { scope.launch { drawerState.open() } },
                        onToggleNative = readerViewModel::toggleNativePdf,
                        onPdfPageMode = readerViewModel::setPdfPageMode,
                        onTtsChange = readerViewModel::updateTts,
                        onPlay = readerViewModel::play,
                        onPause = readerViewModel::pause,
                        onResume = readerViewModel::resume,
                        onPrevious = readerViewModel::skipPrevious,
                        onNext = readerViewModel::skipNext,
                        onOpenVoices = { goTo(AppTab.VOICES) },
                        onToggleTtsBar = readerViewModel::toggleTtsBar,
                        onSelectParagraph = readerViewModel::selectParagraph,
                        modifier = contentModifier
                    )
                }
                AppTab.VOICES -> {
                    val downloaderState by downloaderViewModel.uiState.collectAsStateWithLifecycle()
                    DownloaderScreen(
                        state = downloaderState,
                        selectedVoiceId = ttsConfig.selectedVoiceId,
                        selectedSpeakerId = ttsConfig.speakerId,
                        onDownload = { id -> downloaderViewModel.startDownload(context, id) },
                        onSelectVoice = { voice, speaker ->
                            readerViewModel.updateTts(
                                ttsConfig.copy(
                                    selectedVoiceId = voice.id,
                                    engineType = voice.engineType,
                                    speakerId = speaker
                                )
                            )
                        },
                        onDelete = { id ->
                            downloaderViewModel.delete(id)
                            if (ttsConfig.selectedVoiceId == id) {
                                readerViewModel.updateTts(
                                    ttsConfig.copy(
                                        selectedVoiceId = org.openreader.core.model.TTSConfig.SYSTEM_VOICE_ID,
                                        engineType = TTSEngineType.SYSTEM,
                                        speakerId = 0
                                    )
                                )
                            }
                        },
                        onPreview = { id, speaker -> downloaderViewModel.preview(id, speaker) },
                        modifier = contentModifier
                    )
                }
                AppTab.SETTINGS -> SettingsScreen(
                    theme = theme,
                    ttsConfig = ttsConfig,
                    neuralReady = readerViewModel.availableNeuralVoices().isNotEmpty(),
                    onThemeChange = readerViewModel::updateTheme,
                    onTtsChange = readerViewModel::updateTts,
                    onOpenVoices = { goTo(AppTab.VOICES) },
                    modifier = contentModifier
                )
            }
        }
    }
}
