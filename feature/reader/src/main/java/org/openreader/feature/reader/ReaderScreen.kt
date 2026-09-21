package org.openreader.feature.reader

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun ReaderScreen(
    state: ReaderUiState,
    theme: ReaderTheme,
    ttsConfig: TTSConfig,
    audioState: AudioState,
    widthSizeClass: WindowWidthSizeClass,
    neuralReady: Boolean,
    onToggleNative: () -> Unit,
    onPdfPageMode: (PdfPageMode) -> Unit,
    onTtsChange: (TTSConfig) -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpenVoices: () -> Unit,
    onToggleTtsBar: () -> Unit,
    onSelectParagraph: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val expanded = widthSizeClass == WindowWidthSizeClass.Expanded
    val palette = colorsFor(theme.type)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        ReaderTopBar(
            fileName = state.fileName,
            showNativePdf = state.showNativePdf,
            showTtsBar = state.showTtsBar,
            pdfPageMode = state.pdfPageMode,
            onToggleNative = onToggleNative,
            onToggleTts = onToggleTtsBar,
            onPdfPageMode = onPdfPageMode
        )
        if (state.loading) {
            val label = if (state.extractTotal > 0) {
                "Extrayendo texto ${state.extractPage}/${state.extractTotal}"
            } else {
                "Abriendo documento…"
            }
            Text(
                label,
                color = palette.text,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
        ReaderBody(
            state = state,
            theme = theme,
            audioState = audioState,
            expanded = expanded,
            onSelectParagraph = onSelectParagraph,
            modifier = Modifier.weight(1f)
        )
        if (state.showTtsBar) {
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
                modifier = Modifier.imePadding()
            )
        }
    }
}

@Composable
private fun ReaderBody(
    state: ReaderUiState,
    theme: ReaderTheme,
    audioState: AudioState,
    expanded: Boolean,
    onSelectParagraph: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = colorsFor(theme.type)
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        when {
            state.showNativePdf && state.contentUri.isNotBlank() -> PdfNativePreview(
                uri = Uri.parse(state.contentUri),
                localPath = state.localPdfPath,
                pageMode = state.pdfPageMode,
                modifier = Modifier.fillMaxSize()
            )
            state.paragraphs.isNotEmpty() -> ContinuousText(
                paragraphs = state.paragraphs,
                currentParagraph = state.currentParagraph,
                audioState = audioState,
                theme = theme,
                expanded = expanded,
                onSelectParagraph = onSelectParagraph
            )
            state.loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                val progress = if (state.extractTotal > 0) {
                    "Extrayendo ${state.extractPage}/${state.extractTotal}"
                } else {
                    "Abriendo PDF…"
                }
                Text(
                    progress,
                    color = palette.text,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                )
            }
            state.error != null && state.error != "NEEDS_VOICE" -> Text(
                state.error,
                color = palette.text,
                modifier = Modifier.padding(24.dp)
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
    onSelectParagraph: (Int) -> Unit
) {
    val palette = colorsFor(theme.type)
    val font = readerFontFamily(theme.fontFamily)
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = currentParagraph.coerceIn(0, (paragraphs.size - 1).coerceAtLeast(0))
    )
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
