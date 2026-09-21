package org.openreader.feature.reader

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.openreader.core.model.AudioState
import org.openreader.core.model.ParagraphData
import org.openreader.core.model.ReaderTheme
import org.openreader.core.model.TTSConfig
import org.openreader.core.model.TTSEngineType

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun ReaderScreen(
    state: ReaderUiState,
    theme: ReaderTheme,
    ttsConfig: TTSConfig,
    audioState: AudioState,
    widthSizeClass: WindowWidthSizeClass,
    neuralReady: Boolean,
    onBack: () -> Unit,
    onToggleNative: () -> Unit,
    onToggleControls: () -> Unit,
    onHideControls: () -> Unit,
    onSelectParagraph: (Int) -> Unit,
    onThemeChange: (ReaderTheme) -> Unit,
    onTtsChange: (TTSConfig) -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpenVoices: () -> Unit,
    modifier: Modifier = Modifier
) {
    val expanded = widthSizeClass == WindowWidthSizeClass.Expanded
    val palette = colorsFor(theme.type)
    var showSettings by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        if (expanded) {
            Row(Modifier.fillMaxSize()) {
                AnimatedVisibility(visible = state.controlsVisible) {
                    NavigationRail(modifier = Modifier.statusBarsPadding()) {
                        NavigationRailItem(
                            selected = false,
                            onClick = onBack,
                            icon = { Text("Lib") },
                            label = { Text("Biblioteca") }
                        )
                        NavigationRailItem(
                            selected = showSettings,
                            onClick = { showSettings = !showSettings },
                            icon = { Text("Aa") },
                            label = { Text("Ajustes") }
                        )
                        NavigationRailItem(
                            selected = state.showNativePdf,
                            onClick = onToggleNative,
                            icon = { Text("PDF") },
                            label = { Text("Original") }
                        )
                    }
                }
                if (showSettings && state.controlsVisible) {
                    ThemeSettingsPanel(
                        theme = theme,
                        expandedLayout = true,
                        onChange = onThemeChange,
                        modifier = Modifier
                            .width(320.dp)
                            .fillMaxHeight()
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    ReaderBody(
                        state = state,
                        theme = theme,
                        audioState = audioState,
                        expanded = true,
                        onToggleControls = onToggleControls,
                        onHideControls = onHideControls,
                        onSelectParagraph = onSelectParagraph
                    )
                    AnimatedVisibility(visible = state.controlsVisible) {
                        TtsControls(
                            audioState = audioState,
                            config = ttsConfig,
                            neuralReady = neuralReady,
                            onPlay = onPlay,
                            onPause = onPause,
                            onResume = onResume,
                            onPrevious = onPrevious,
                            onNext = onNext,
                            onRateChange = { onTtsChange(ttsConfig.copy(speechRate = it)) },
                            onEngineChange = { onTtsChange(ttsConfig.copy(engineType = it)) },
                            onOpenVoices = onOpenVoices,
                            modifier = Modifier.navigationBarsPadding()
                        )
                    }
                }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                AnimatedVisibility(visible = state.controlsVisible) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onBack) { Text("Biblioteca") }
                        TextButton(onClick = { showSettings = true }) { Text("Ajustes") }
                        TextButton(onClick = onToggleNative) {
                            Text(if (state.showNativePdf) "Texto" else "PDF")
                        }
                    }
                }
                ReaderBody(
                    state = state,
                    theme = theme,
                    audioState = audioState,
                    expanded = false,
                    onToggleControls = onToggleControls,
                    onHideControls = onHideControls,
                    onSelectParagraph = onSelectParagraph,
                    modifier = Modifier.weight(1f)
                )
                AnimatedVisibility(visible = state.controlsVisible) {
                    TtsControls(
                        audioState = audioState,
                        config = ttsConfig,
                        neuralReady = neuralReady,
                        onPlay = onPlay,
                        onPause = onPause,
                        onResume = onResume,
                        onPrevious = onPrevious,
                        onNext = onNext,
                        onRateChange = { onTtsChange(ttsConfig.copy(speechRate = it)) },
                        onEngineChange = { onTtsChange(ttsConfig.copy(engineType = it)) },
                        onOpenVoices = onOpenVoices,
                        modifier = Modifier
                            .navigationBarsPadding()
                            .imePadding()
                    )
                }
            }
            if (showSettings) {
                ModalBottomSheet(
                    onDismissRequest = { showSettings = false },
                    sheetState = sheetState
                ) {
                    ThemeSettingsPanel(
                        theme = theme,
                        expandedLayout = false,
                        onChange = onThemeChange
                    )
                }
            }
        }
    }
}

@Composable
private fun ReaderBody(
    state: ReaderUiState,
    theme: ReaderTheme,
    audioState: AudioState,
    expanded: Boolean,
    onToggleControls: () -> Unit,
    onHideControls: () -> Unit,
    onSelectParagraph: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = colorsFor(theme.type)
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onToggleControls() })
            },
        contentAlignment = Alignment.TopCenter
    ) {
        when {
            state.loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            state.error != null && state.error != "NEEDS_VOICE" -> Text(
                state.error,
                color = palette.text,
                modifier = Modifier.padding(24.dp)
            )
            state.showNativePdf && state.contentUri.isNotBlank() -> PdfNativePreview(
                uri = Uri.parse(state.contentUri),
                modifier = Modifier.fillMaxSize()
            )
            else -> ContinuousText(
                paragraphs = state.paragraphs,
                currentParagraph = state.currentParagraph,
                audioState = audioState,
                theme = theme,
                expanded = expanded,
                onHideControls = onHideControls,
                onSelectParagraph = onSelectParagraph
            )
        }
        if (state.error == "NEEDS_VOICE") {
            Text(
                "Descarga una voz neuronal para usar el motor Piper.",
                color = palette.text,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }
    }
}

@Composable
private fun ContinuousText(
    paragraphs: List<ParagraphData>,
    currentParagraph: Int,
    audioState: AudioState,
    theme: ReaderTheme,
    expanded: Boolean,
    onHideControls: () -> Unit,
    onSelectParagraph: (Int) -> Unit
) {
    val palette = colorsFor(theme.type)
    val font = readerFontFamily(theme.fontFamily)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = currentParagraph)
    LaunchedEffect(currentParagraph) {
        if (paragraphs.isNotEmpty()) {
            val target = currentParagraph.coerceIn(0, paragraphs.lastIndex)
            listState.animateScrollToItem(target)
        }
    }
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }.collect { scrolling ->
            if (scrolling) onHideControls()
        }
    }
    val maxWidth = if (expanded) theme.maxContainerWidthRem.rem else 48.rem
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = maxWidth)
            .padding(horizontal = if (expanded) 24.dp else 16.dp, vertical = 16.dp)
    ) {
        itemsIndexed(paragraphs, key = { _, item -> item.id }) { index, paragraph ->
            val playing = audioState as? AudioState.Playing
            val highlight = playing != null && playing.paragraphIndex == index
            val annotated = buildAnnotatedString {
                if (highlight) {
                    val start = playing.startCharOffset.coerceIn(0, paragraph.text.length)
                    val end = playing.endCharOffset.coerceIn(start, paragraph.text.length)
                    append(paragraph.text.substring(0, start))
                    withStyle(SpanStyle(background = palette.highlight, fontWeight = FontWeight.Medium)) {
                        append(paragraph.text.substring(start, end))
                    }
                    append(paragraph.text.substring(end))
                } else {
                    append(paragraph.text)
                }
            }
            Text(
                text = annotated,
                color = palette.text,
                fontFamily = font,
                fontSize = theme.fontSizeSp.sp,
                lineHeight = (theme.fontSizeSp * theme.lineHeightMultiplier).sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectParagraph(index) }
                    .padding(bottom = 16.dp)
                    .background(
                        if (index == currentParagraph && !highlight) palette.highlight.copy(alpha = 0.35f)
                        else palette.background
                    )
            )
        }
    }
}
