package org.openreader.core.tts

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.openreader.core.model.AudioState
import org.openreader.core.model.ParagraphData
import org.openreader.core.model.TTSConfig
import org.openreader.core.model.TTSEngineType
import java.io.File

class TtsController(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val neural = NeuralTtsEngine(defaultDispatcher)
    private val sink = AudioTrackSink()
    private val neuralQueue = TtsQueueManager(
        synthesizer = neural,
        sink = sink,
        defaultDispatcher = defaultDispatcher,
        ioDispatcher = ioDispatcher
    )
    private val systemEngine = SystemTtsEngine(context, ioDispatcher)

    private val _state = MutableStateFlow<AudioState>(AudioState.Idle)
    val state: StateFlow<AudioState> = _state.asStateFlow()

    private var paragraphs: List<ParagraphData> = emptyList()
    private var config: TTSConfig = TTSConfig()
    private var modelsDir: File = File(context.filesDir, "models")

    init {
        systemEngine.setCallbacks(
            onState = { _state.value = it },
            onParagraphFinished = { finished ->
                if (config.engineType != TTSEngineType.SYSTEM) return@setCallbacks
                val prefetchIndex = finished + 2
                if (prefetchIndex < paragraphs.size) {
                    systemEngine.enqueue(paragraphs[prefetchIndex], prefetchIndex)
                } else if (finished + 1 >= paragraphs.size) {
                    _state.value = AudioState.Idle
                }
            }
        )
        scope.launch {
            neuralQueue.state.collect { neuralState ->
                if (config.engineType == TTSEngineType.SHERPA_ONNX_PIPER) {
                    _state.value = neuralState
                }
            }
        }
    }

    fun setModelsDir(dir: File) {
        modelsDir = dir
    }

    fun updateParagraphs(value: List<ParagraphData>) {
        paragraphs = value
        neuralQueue.updateParagraphs(value)
    }

    fun updateConfig(value: TTSConfig) {
        config = value
        neuralQueue.setSpeechRate(value.speechRate)
        systemEngine.applyConfig(value)
    }

    fun play(startIndex: Int) {
        when (config.engineType) {
            TTSEngineType.SYSTEM -> {
                scope.launch {
                    systemEngine.awaitReadyOrThrow()
                    systemEngine.applyConfig(config)
                    systemEngine.speakQueue(paragraphs, startIndex, prefetchNext = true)
                }
            }
            TTSEngineType.SHERPA_ONNX_PIPER -> {
                scope.launch {
                    val dir = File(modelsDir, config.selectedVoiceId)
                    neural.prepare(dir)
                    neuralQueue.setSpeechRate(config.speechRate)
                    neuralQueue.play(startIndex)
                }
            }
        }
    }

    fun pause() {
        when (config.engineType) {
            TTSEngineType.SYSTEM -> systemEngine.pause()
            TTSEngineType.SHERPA_ONNX_PIPER -> neuralQueue.pause()
        }
    }

    fun resume() {
        when (config.engineType) {
            TTSEngineType.SYSTEM -> {
                val index = when (val current = _state.value) {
                    is AudioState.Paused -> current.paragraphIndex
                    is AudioState.Playing -> current.paragraphIndex
                    else -> 0
                }
                play(index)
            }
            TTSEngineType.SHERPA_ONNX_PIPER -> neuralQueue.resume()
        }
    }

    fun skipNext() {
        val index = currentIndex() + 1
        if (index < paragraphs.size) play(index)
    }

    fun skipPrevious() {
        val index = (currentIndex() - 1).coerceAtLeast(0)
        play(index)
    }

    fun stop() {
        systemEngine.stop()
        neuralQueue.stop()
        _state.value = AudioState.Idle
    }

    fun release() {
        stop()
        systemEngine.release()
        neuralQueue.release()
    }

    fun currentIndex(): Int = when (val current = _state.value) {
        is AudioState.Playing -> current.paragraphIndex
        is AudioState.Paused -> current.paragraphIndex
        is AudioState.Synthesizing -> current.paragraphIndex
        else -> 0
    }

    fun isNeuralModelReady(voiceId: String): Boolean {
        val dir = File(modelsDir, voiceId)
        return File(dir, "tokens.txt").exists() &&
            dir.listFiles()?.any { it.extension == "onnx" } == true &&
            File(dir, "espeak-ng-data/phontab").exists()
    }
}
