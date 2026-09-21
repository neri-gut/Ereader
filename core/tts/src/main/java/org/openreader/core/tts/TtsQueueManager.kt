package org.openreader.core.tts

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.openreader.core.model.AudioState
import org.openreader.core.model.ParagraphData

/**
 * Cola N+1: un Channel de capacidad 2 precarga el siguiente párrafo
 * mientras AudioTrack reproduce el actual.
 */
class TtsQueueManager(
    private val synthesizer: SpeechSynthesizer,
    private val sink: AudioSink,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val _state = MutableStateFlow<AudioState>(AudioState.Idle)
    val state: StateFlow<AudioState> = _state

    @Volatile
    private var speechRate: Float = 1.0f
    @Volatile
    private var paused: Boolean = false
    private var playbackJob: Job? = null
    private var paragraphs: List<ParagraphData> = emptyList()

    fun updateParagraphs(value: List<ParagraphData>) {
        paragraphs = value
    }

    fun setSpeechRate(rate: Float) {
        speechRate = rate.coerceIn(0.5f, 3.0f)
    }

    fun play(startIndex: Int) {
        val items = paragraphs
        if (items.isEmpty()) return
        paused = false
        stopInternal(keepState = false)
        val start = startIndex.coerceIn(0, items.lastIndex)
        playbackJob = scope.launch {
            val channel = Channel<PcmBuffer>(capacity = CHANNEL_CAPACITY)
            val producer = launch(defaultDispatcher) {
                try {
                    for (index in start until items.size) {
                        val paragraph = items[index]
                        if (paragraph.text.isBlank()) continue
                        _state.value = AudioState.Synthesizing(index)
                        val buffer = synthesizer.synthesize(index, paragraph.text, speechRate)
                        channel.send(buffer)
                    }
                } catch (_: CancellationException) {
                    // cancelled by stop/skip
                } finally {
                    channel.close()
                }
            }
            try {
                for (buffer in channel) {
                    while (paused) {
                        delay(40)
                    }
                    _state.value = AudioState.Playing(
                        paragraphIndex = buffer.paragraphIndex,
                        startCharOffset = 0,
                        endCharOffset = 0
                    )
                    sink.play(buffer) { startChar, endChar ->
                        _state.value = AudioState.Playing(
                            paragraphIndex = buffer.paragraphIndex,
                            startCharOffset = startChar,
                            endCharOffset = endChar
                        )
                    }
                }
                _state.value = AudioState.Idle
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.value = AudioState.Error(error.message ?: "Error de síntesis")
            } finally {
                producer.cancel()
                channel.close()
            }
        }
    }

    fun pause() {
        val current = _state.value
        val index = when (current) {
            is AudioState.Playing -> current.paragraphIndex
            is AudioState.Synthesizing -> current.paragraphIndex
            is AudioState.Paused -> return
            else -> return
        }
        paused = true
        sink.pause()
        _state.value = AudioState.Paused(index)
    }

    fun resume() {
        val current = _state.value
        if (current is AudioState.Paused) {
            paused = false
            sink.resumePlayback()
            _state.value = AudioState.Playing(
                paragraphIndex = current.paragraphIndex,
                startCharOffset = 0,
                endCharOffset = 0
            )
        }
    }

    fun skipTo(index: Int) {
        play(index)
    }

    fun stop() {
        stopInternal(keepState = false)
        _state.value = AudioState.Idle
    }

    fun release() {
        stop()
        sink.release()
        synthesizer.release()
    }

    private fun stopInternal(keepState: Boolean) {
        playbackJob?.cancel()
        playbackJob = null
        sink.stop()
        if (!keepState) {
            // state assigned by caller
        }
    }

    companion object {
        const val CHANNEL_CAPACITY = 2
    }
}
