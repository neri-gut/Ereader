package org.openreader.core.tts

import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

class NeuralTtsEngine(
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) : SpeechSynthesizer {
    private val mutex = Mutex()
    private var tts: OfflineTts? = null
    private var loadedDir: File? = null
    @Volatile
    var speakerId: Int = 0

    suspend fun prepare(modelDir: File) {
        mutex.withLock {
            if (loadedDir == modelDir && tts != null) return
            releaseLocked()
            val onnx = modelDir.listFiles()?.firstOrNull { it.extension == "onnx" }
                ?: error("No se encontró el modelo ONNX en ${modelDir.absolutePath}")
            val tokens = File(modelDir, "tokens.txt")
            val dataDir = File(modelDir, "espeak-ng-data")
            require(tokens.exists()) { "Falta tokens.txt" }
            require(File(dataDir, "phontab").exists()) { "Falta espeak-ng-data/phontab" }
            val config = OfflineTtsConfig(
                model = OfflineTtsModelConfig(
                    vits = OfflineTtsVitsModelConfig(
                        model = onnx.absolutePath,
                        tokens = tokens.absolutePath,
                        dataDir = dataDir.absolutePath
                    ),
                    numThreads = 2,
                    debug = false,
                    provider = "cpu"
                )
            )
            tts = OfflineTts(config = config)
            loadedDir = modelDir
        }
    }

    override suspend fun synthesize(
        paragraphIndex: Int,
        text: String,
        speed: Float
    ): PcmBuffer = withContext(defaultDispatcher) {
        mutex.withLock {
            val engine = tts ?: error("Motor neuronal no inicializado")
            val audio = engine.generate(
                text = text,
                sid = speakerId.coerceAtLeast(0),
                speed = speed.coerceIn(0.5f, 3.0f)
            )
            PcmBuffer(
                paragraphIndex = paragraphIndex,
                text = text,
                pcm16 = floatToPcm16(audio.samples),
                sampleRate = audio.sampleRate
            )
        }
    }

    override fun release() {
        tts?.release()
        tts = null
        loadedDir = null
    }

    private fun releaseLocked() {
        tts?.release()
        tts = null
        loadedDir = null
    }

    private fun floatToPcm16(samples: FloatArray): ShortArray {
        val out = ShortArray(samples.size)
        for (i in samples.indices) {
            val clipped = samples[i].coerceIn(-1f, 1f)
            out[i] = (clipped * Short.MAX_VALUE).roundToInt().toShort()
        }
        return out
    }
}
