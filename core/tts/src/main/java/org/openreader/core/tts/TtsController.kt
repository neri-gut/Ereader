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
    @Volatile
    private var activeEngine: TTSEngineType = TTSEngineType.SYSTEM

    init {
        systemEngine.setCallbacks(
            onState = { audio ->
                if (activeEngine == TTSEngineType.SYSTEM) {
                    _state.value = audio
                }
            },
            onParagraphFinished = { finished ->
                if (activeEngine != TTSEngineType.SYSTEM) return@setCallbacks
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
                if (activeEngine == TTSEngineType.SHERPA_ONNX_PIPER) {
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
        val engineChanged = value.engineType != config.engineType ||
            value.selectedVoiceId != config.selectedVoiceId
        if (engineChanged) stop()
        config = value
        neuralQueue.setSpeechRate(value.speechRate)
        systemEngine.applyConfig(value)
    }

    fun play(startIndex: Int) {
        stop()
        when (config.engineType) {
            TTSEngineType.SHERPA_ONNX_PIPER -> {
                if (!isNeuralModelReady(config.selectedVoiceId)) {
                    _state.value = AudioState.Error("Descarga una voz neuronal en Voces antes de usarla")
                    return
                }
                activeEngine = TTSEngineType.SHERPA_ONNX_PIPER
                scope.launch {
                    try {
                        val dir = File(modelsDir, config.selectedVoiceId)
                        neural.prepare(dir)
                        neuralQueue.setSpeechRate(config.speechRate)
                        neuralQueue.play(startIndex)
                    } catch (error: Throwable) {
                        _state.value = AudioState.Error(error.message ?: "Fallo del motor neuronal")
                    }
                }
            }
            TTSEngineType.SYSTEM -> playSystem(startIndex)
        }
    }

    private fun playSystem(startIndex: Int) {
        activeEngine = TTSEngineType.SYSTEM
        scope.launch {
            try {
                systemEngine.awaitReadyOrThrow()
                systemEngine.applyConfig(config)
                systemEngine.speakQueue(paragraphs, startIndex, prefetchNext = true)
            } catch (error: Throwable) {
                _state.value = AudioState.Error(error.message ?: "TTS del sistema no disponible")
            }
        }
    }

    fun pause() {
        when (activeEngine) {
            TTSEngineType.SYSTEM -> systemEngine.pause()
            TTSEngineType.SHERPA_ONNX_PIPER -> neuralQueue.pause()
        }
    }

    fun resume() {
        when (activeEngine) {
            TTSEngineType.SYSTEM -> {
                val index = when (val current = _state.value) {
                    is AudioState.Paused -> current.paragraphIndex
                    is AudioState.Playing -> current.paragraphIndex
                    else -> 0
                }
                playSystem(index)
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

    suspend fun awaitSystemReady() {
        systemEngine.ensureReady()
    }

    fun systemVoices(): List<org.openreader.core.model.VoiceModel> {
        return systemEngine.installedVoices()
            .sortedWith(compareBy({ it.locale.toLanguageTag() }, { it.name }))
            .map { voice ->
                org.openreader.core.model.VoiceModel(
                    id = "system:${voice.name}",
                    name = voice.name.substringAfterLast(":").ifBlank { voice.name },
                    languageCode = voice.locale.toLanguageTag(),
                    engineType = TTSEngineType.SYSTEM,
                    isDownloaded = true
                )
            }
    }

    fun currentEngine(): TTSEngineType = activeEngine
}
