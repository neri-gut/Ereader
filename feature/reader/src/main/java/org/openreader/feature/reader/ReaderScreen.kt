package org.openreader.feature.reader

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.E
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlinx.coroutines.withTimeoutOrNull
import org.openreader.core.model.AudioState
import org.openreader.core.model.ParagraphData
import org.openreader.core.model.ReaderTheme
import org.openreader.core.model.TTSConfig

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    state: ReaderUiState,
    theme: ReaderTheme,
    ttsConfig: TTSConfig,
    audioState: AudioState,
    widthSizeClass: WindowWidthSizeClass,
    voiceLabel: String,
    onOpenMenu: () -> Unit,
    onToggleNative: () -> Unit,
    onPdfPageMode: (PdfPageMode) -> Unit,
    onTtsChange: (TTSConfig) -> Unit,
    onThemeChange: (ReaderTheme) -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpenVoices: () -> Unit,
    onToggleTtsBar: () -> Unit,
    screenBrightness: Float,
    onBrightnessChange: (Float) -> Unit,
    onToggleChrome: () -> Unit,
    onShowChrome: () -> Unit,
    onHideChrome: () -> Unit,
    onSelectParagraph: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val expanded = widthSizeClass == WindowWidthSizeClass.Expanded
    val palette = colorsFor(theme.type)
    val view = LocalView.current
    var readingSheet by remember { mutableStateOf(false) }
    var liveBrightness by remember(screenBrightness) { mutableStateOf(screenBrightness) }
    var hud by remember { mutableStateOf<ReaderHud?>(null) }
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        val attrs = window.attributes
        attrs.screenBrightness = liveBrightness
        window.attributes = attrs
    }
    val playing = audioState is AudioState.Playing || audioState is AudioState.Synthesizing
    val readFraction = if (state.paragraphs.isEmpty()) {
        0f
    } else {
        (state.currentParagraph + 1f) / state.paragraphs.size.toFloat()
    }

    BackHandler(enabled = readingSheet || state.chromeVisible) {
        when {
            readingSheet -> readingSheet = false
            state.chromeVisible -> onHideChrome()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        ReaderBody(
            state = state,
            theme = theme,
            audioState = audioState,
            expanded = expanded,
            onSelectParagraph = onSelectParagraph,
            onToggleChrome = onToggleChrome,
            onShowChrome = onShowChrome,
            onScrubParagraph = { index ->
                hud = index?.let {
                    ReaderHud(Icons.Filled.FormatListNumbered, "${it + 1}")
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        if (state.loading) {
            val fraction = if (state.extractTotal > 0) {
                state.extractPage.toFloat() / state.extractTotal.toFloat()
            } else {
                0f
            }
            if (state.extractTotal > 0) {
                LinearProgressIndicator(
                    progress = { fraction.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(2.dp),
                    color = palette.text,
                    trackColor = palette.text.copy(alpha = 0.15f)
                )
            } else {
                LinearProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(2.dp),
                    color = palette.text,
                    trackColor = palette.text.copy(alpha = 0.15f)
                )
            }
        }
        if (playing && !state.chromeVisible) {
            LinearProgressIndicator(
                progress = { readFraction.coerceIn(0f, 1f) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(2.dp),
                color = palette.highlight,
                trackColor = palette.text.copy(alpha = 0.12f)
            )
        }
        AnimatedVisibility(
            visible = state.chromeVisible,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Column {
                ReaderTopBar(
                    showNativePdf = state.showNativePdf,
                    showTtsBar = state.showTtsBar,
                    pdfPageMode = state.pdfPageMode,
                    palette = palette,
                    onOpenMenu = onOpenMenu,
                    onToggleNative = onToggleNative,
                    onToggleTts = onToggleTtsBar,
                    onOpenPage = { readingSheet = true },
                    onPdfPageMode = onPdfPageMode
                )
                if (state.paragraphs.isNotEmpty()) {
                    LinearProgressIndicator(
                        progress = { readFraction.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                        color = palette.text,
                        trackColor = palette.text.copy(alpha = 0.15f)
                    )
                }
            }
        }
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .width(48.dp)
                .fillMaxHeight()
                .padding(top = 96.dp)
                .clickable(onClick = onToggleChrome)
        )
        if (state.chromeVisible) {
            BrightnessEdge(
                brightness = liveBrightness,
                onChange = { value ->
                    liveBrightness = value
                    hud = ReaderHud(Icons.Filled.BrightnessHigh, "${(value * 100).roundToInt()}%")
                },
                onFinish = {
                    onBrightnessChange(liveBrightness)
                    hud = null
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(48.dp)
                    .fillMaxHeight()
                    .padding(top = 96.dp, bottom = 96.dp)
            )
        }
        hud?.let { current ->
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(palette.background.copy(alpha = 0.72f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(current.icon, contentDescription = null, tint = palette.text)
                Text(current.value, color = palette.text, fontSize = 20.sp)
            }
        }
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
            if (state.chromeVisible) {
                SpeedEdge(
                    rate = ttsConfig.speechRate,
                    onChange = { value ->
                        hud = ReaderHud(Icons.Filled.Speed, "${"%.1f".format(value)}×")
                    },
                    onFinish = { value ->
                        onTtsChange(ttsConfig.copy(speechRate = value))
                        hud = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                )
            }
            AnimatedVisibility(
                visible = state.chromeVisible && state.showTtsBar,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it }
            ) {
                TtsControls(
                    audioState = audioState,
                    config = ttsConfig,
                    voiceLabel = voiceLabel,
                    palette = palette,
                    onPlay = onPlay,
                    onPause = onPause,
                    onResume = onResume,
                    onPrevious = onPrevious,
                    onNext = onNext,
                    onRateChange = { onTtsChange(ttsConfig.copy(speechRate = it)) },
                    onOpenVoices = onOpenVoices
                )
            }
        }
        if (state.error == "NEEDS_VOICE" && state.chromeVisible) {
            Text(
                "Elige una voz en el control de audio.",
                color = palette.text,
                fontSize = 13.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
            )
        }
    }

    if (readingSheet && !state.showNativePdf) {
        ModalBottomSheet(
            onDismissRequest = { readingSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = palette.background
        ) {
            ThemeSettingsPanel(
                theme = theme,
                expandedLayout = expanded,
                onChange = onThemeChange
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
    onToggleChrome: () -> Unit,
    onShowChrome: () -> Unit,
    onScrubParagraph: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = colorsFor(theme.type)
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        when {
            state.showNativePdf && state.contentUri.isNotBlank() -> PdfNativePreview(
                uri = Uri.parse(state.contentUri),
                localPath = state.localPdfPath,
                pageMode = state.pdfPageMode,
                showChrome = state.chromeVisible,
                onToggleChrome = onToggleChrome,
                onUserScroll = {},
                modifier = Modifier.fillMaxSize()
            )
            state.paragraphs.isNotEmpty() -> ContinuousText(
                paragraphs = state.paragraphs,
                currentParagraph = state.currentParagraph,
                audioState = audioState,
                theme = theme,
                expanded = expanded,
                onSelectParagraph = onSelectParagraph,
                onToggleChrome = onToggleChrome,
                onShowChrome = onShowChrome,
                onScrubParagraph = onScrubParagraph
            )
            state.error != null && state.error != "NEEDS_VOICE" -> Text(
                state.error ?: "",
                color = palette.text,
                modifier = Modifier
                    .padding(24.dp)
                    .clickable(onClick = onToggleChrome)
            )
            else -> Box(
                Modifier
                    .fillMaxSize()
                    .clickable(onClick = onToggleChrome)
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
    onSelectParagraph: (Int) -> Unit,
    onToggleChrome: () -> Unit,
    onShowChrome: () -> Unit,
    onScrubParagraph: (Int?) -> Unit
) {
    val palette = colorsFor(theme.type)
    val font = readerFontFamily(theme.fontFamily)
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = currentParagraph.coerceIn(0, (paragraphs.size - 1).coerceAtLeast(0))
    )
    val showChromeState = rememberUpdatedState(onShowChrome)
    val revealDistance = with(LocalDensity.current) { 24.dp.toPx() }
    val revealOnPull = remember(revealDistance) {
        var pulled = 0f
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.Drag) return Offset.Zero
                if (available.y > 0f) {
                    pulled += available.y
                    if (pulled >= revealDistance) {
                        showChromeState.value()
                        pulled = 0f
                    }
                } else {
                    pulled = 0f
                }
                return Offset.Zero
            }
        }
    }
    LaunchedEffect(currentParagraph, paragraphs.size) {
        if (currentParagraph !in paragraphs.indices) return@LaunchedEffect
        val visible = listState.layoutInfo.visibleItemsInfo.any { it.index == currentParagraph }
        if (!visible) {
            listState.animateScrollToItem(currentParagraph)
        }
    }
    val maxWidth = if (expanded) theme.maxContainerWidthRem.rem else 48.rem
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(vertical = 72.dp),
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = maxWidth)
            .nestedScroll(revealOnPull)
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
                    .paragraphScrub(
                        index = index,
                        lastIndex = paragraphs.lastIndex,
                        onSelect = onSelectParagraph,
                        onScrub = onScrubParagraph
                    )
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
                    .semantics {
                        if (index == currentParagraph) {
                            contentDescription = "Párrafo ${index + 1} seleccionado"
                        }
                    }
                    .background(
                        when {
                            highlight -> palette.highlight.copy(alpha = 0.55f)
                            index == currentParagraph -> palette.highlight.copy(alpha = 0.35f)
                            else -> palette.background
                        }
                    )
            )
        }
    }
}

private data class ReaderHud(val icon: ImageVector, val value: String)

private fun Modifier.paragraphScrub(
    index: Int,
    lastIndex: Int,
    onSelect: (Int) -> Unit,
    onScrub: (Int?) -> Unit
): Modifier = pointerInput(index, lastIndex) {
    val slop = viewConfiguration.touchSlop
    val fullTravel = 220.dp.toPx()
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val origin = down.position
        val held = withTimeoutOrNull(350) {
            var change = down
            while (true) {
                if (!change.pressed) return@withTimeoutOrNull 1
                if ((change.position - origin).getDistance() > slop) return@withTimeoutOrNull 2
                val event = awaitPointerEvent()
                change = event.changes.firstOrNull { it.id == down.id } ?: return@withTimeoutOrNull 2
            }
            @Suppress("UNREACHABLE_CODE")
            2
        }
        if (held == null) {
            var current = index
            onScrub(current)
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) break
                change.consume()
                val next = paragraphAtTravel(
                    start = index,
                    lastIndex = lastIndex,
                    dy = change.position.y - origin.y,
                    fullTravel = fullTravel
                )
                if (next != current) {
                    current = next
                    onScrub(current)
                }
            }
            onSelect(current)
            onScrub(null)
        } else if (held == 1) {
            onSelect(index)
        }
    }
}

private fun paragraphAtTravel(start: Int, lastIndex: Int, dy: Float, fullTravel: Float): Int {
    val reach = if (dy >= 0f) lastIndex - start else start
    if (reach <= 0 || fullTravel <= 0f) return start
    val t = (abs(dy) / fullTravel).coerceIn(0f, 1f)
    val curved = ln(1f + t * (E.toFloat() - 1f))
    val delta = (curved * reach).roundToInt()
    return (start + if (dy >= 0f) delta else -delta).coerceIn(0, lastIndex)
}

@Composable
private fun BrightnessEdge(
    brightness: Float,
    onChange: (Float) -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val changeState = rememberUpdatedState(onChange)
    val finishState = rememberUpdatedState(onFinish)
    val startState = rememberUpdatedState(if (brightness < 0f) 0.6f else brightness)
    Box(
        modifier.pointerInput(Unit) {
                var value = startState.value
                detectVerticalDragGestures(
                    onDragStart = {
                        value = startState.value.coerceIn(0.05f, 1f)
                        changeState.value(value)
                    },
                    onVerticalDrag = { _, drag ->
                        value = (value - drag / 500f).coerceIn(0.05f, 1f)
                        changeState.value(value)
                    },
                    onDragEnd = { finishState.value() },
                    onDragCancel = { finishState.value() }
                )
            }
    )
}

@Composable
private fun SpeedEdge(
    rate: Float,
    onChange: (Float) -> Unit,
    onFinish: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val changeState = rememberUpdatedState(onChange)
    val finishState = rememberUpdatedState(onFinish)
    val startState = rememberUpdatedState(rate)
    Box(
        modifier.pointerInput(Unit) {
                var value = startState.value
                detectHorizontalDragGestures(
                    onDragStart = {
                        value = startState.value
                        changeState.value(value)
                    },
                    onHorizontalDrag = { _, drag ->
                        value = (value + drag / 280f).coerceIn(
                            TTSConfig.MIN_SPEECH_RATE,
                            TTSConfig.MAX_SPEECH_RATE
                        )
                        changeState.value(value)
                    },
                    onDragEnd = { finishState.value(value) },
                    onDragCancel = { finishState.value(value) }
                )
            }
    )
}
