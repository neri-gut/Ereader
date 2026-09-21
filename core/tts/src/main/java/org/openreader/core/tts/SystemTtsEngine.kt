package org.openreader.core.tts

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.openreader.core.model.AudioState
import org.openreader.core.model.ParagraphData
import org.openreader.core.model.TTSConfig
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Motor TTS nativo. Encola el párrafo N y el N+1 con QUEUE_ADD
 * para cubrir el requisito de precarga sin PCM intermedio.
 */
class SystemTtsEngine(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val appContext = context.applicationContext
    private val ready = CompletableDeferred<Boolean>()
    private val utteranceSeq = AtomicInteger(0)
    @Volatile
    private var currentIndex: Int = 0
    @Volatile
    private var currentText: String = ""
    @Volatile
    private var onState: (AudioState) -> Unit = {}
    @Volatile
    private var onParagraphFinished: (Int) -> Unit = {}

    private val tts: TextToSpeech = TextToSpeech(appContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.getDefault()
            ready.complete(true)
        } else {
            ready.complete(false)
        }
    }

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                val index = parseIndex(utteranceId) ?: currentIndex
                currentIndex = index
                onState(
                    AudioState.Playing(
                        paragraphIndex = index,
                        startCharOffset = 0,
                        endCharOffset = 0
                    )
                )
            }

            override fun onDone(utteranceId: String?) {
                val finished = parseIndex(utteranceId) ?: currentIndex
                onParagraphFinished(finished)
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                onState(AudioState.Error("Error en TTS del sistema"))
            }

            override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                val index = parseIndex(utteranceId) ?: currentIndex
                onState(
                    AudioState.Playing(
                        paragraphIndex = index,
                        startCharOffset = start,
                        endCharOffset = end
                    )
                )
            }
        })
    }

    suspend fun ensureReady(): Boolean = withContext(ioDispatcher) { ready.await() }

    fun setCallbacks(
        onState: (AudioState) -> Unit,
        onParagraphFinished: (Int) -> Unit
    ) {
        this.onState = onState
        this.onParagraphFinished = onParagraphFinished
    }

    fun applyConfig(config: TTSConfig) {
        tts.setSpeechRate(config.speechRate.coerceIn(TTSConfig.MIN_SPEECH_RATE, TTSConfig.MAX_SPEECH_RATE))
        tts.setPitch(config.pitch)
        if (config.selectedVoiceId.startsWith("system:")) {
            val name = config.selectedVoiceId.removePrefix("system:")
            val match = tts.voices?.firstOrNull { it.name == name }
            if (match != null) {
                tts.voice = match
                tts.language = match.locale
            }
        } else {
            tts.language = Locale.getDefault()
        }
    }

    fun installedVoices(): List<android.speech.tts.Voice> {
        if (!ready.isCompleted) return emptyList()
        return tts.voices?.toList().orEmpty()
    }

    fun speakQueue(paragraphs: List<ParagraphData>, startIndex: Int, prefetchNext: Boolean) {
        if (paragraphs.isEmpty()) return
        val start = startIndex.coerceIn(0, paragraphs.lastIndex)
        currentIndex = start
        currentText = paragraphs[start].text
        tts.stop()
        speak(paragraphs[start].text, TextToSpeech.QUEUE_FLUSH, start)
        if (prefetchNext && start + 1 <= paragraphs.lastIndex) {
            enqueue(paragraphs[start + 1], start + 1)
        }
    }

    fun enqueue(paragraph: ParagraphData, index: Int) {
        speak(paragraph.text, TextToSpeech.QUEUE_ADD, index)
    }

    fun pause() {
        tts.stop()
        onState(AudioState.Paused(currentIndex))
    }

    fun stop() {
        tts.stop()
        onState(AudioState.Idle)
    }

    fun release() {
        tts.stop()
        tts.shutdown()
    }

    fun advanceTo(index: Int) {
        currentIndex = index
    }

    private fun speak(text: String, queueMode: Int, index: Int) {
        val id = "p-$index-${utteranceSeq.incrementAndGet()}"
        val params = Bundle()
        tts.speak(text, queueMode, params, id)
    }

    private fun parseIndex(utteranceId: String?): Int? {
        if (utteranceId == null || !utteranceId.startsWith("p-")) return null
        return utteranceId.substringAfter("p-").substringBefore("-").toIntOrNull()
    }

    suspend fun awaitReadyOrThrow() {
        val ok = ensureReady()
        if (!ok) error("TTS del sistema no disponible")
    }

    suspend fun waitInit(): Unit = suspendCancellableCoroutine { cont ->
        if (ready.isCompleted) {
            cont.resume(Unit)
            return@suspendCancellableCoroutine
        }
        ready.invokeOnCompletion { error ->
            if (error != null) cont.resumeWithException(error) else cont.resume(Unit)
        }
    }
}
