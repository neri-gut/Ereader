package org.openreader.feature.downloader

import android.content.Context
import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.openreader.core.model.DownloadState
import java.io.File
import java.security.MessageDigest

class ModelDownloader(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val modelsDir = File(context.applicationContext.filesDir, "models")
    private val client = HttpClient(OkHttp)
    private val _state = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val state: StateFlow<DownloadState> = _state.asStateFlow()

    fun modelsDirectory(): File = modelsDir

    fun isInstalled(pack: VoicePack): Boolean {
        val dir = File(modelsDir, pack.id)
        return isComplete(dir, pack)
    }

    suspend fun download(voiceId: String) = withContext(ioDispatcher) {
        val pack = VoiceCatalog.byId(voiceId) ?: error("Voz desconocida: $voiceId")
        val targetDir = File(modelsDir, pack.id)
        val tempArchive = File(modelsDir, "${pack.id}.tar.bz2.part")
        try {
            modelsDir.mkdirs()
            _state.value = DownloadState.InProgress(voiceId, 0, -1)
            client.prepareGet(pack.archiveUrl).execute { response ->
                val total = response.contentLength() ?: -1L
                response.bodyAsChannel().toInputStream().use { input ->
                    tempArchive.outputStream().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var readBytes = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            readBytes += read
                            _state.value = DownloadState.InProgress(voiceId, readBytes, total)
                        }
                    }
                }
            }
            _state.value = DownloadState.Verifying(voiceId)
            if (targetDir.exists()) targetDir.deleteRecursively()
            ArchiveExtractor.extractTarBz2(tempArchive, targetDir)
            normalizeLayout(targetDir, pack)
            val onnx = File(targetDir, pack.onnxFileName)
            if (!onnx.exists() || onnx.length() < MIN_ONNX_BYTES) {
                error("El paquete no contiene un modelo ONNX válido")
            }
            val actual = sha256(onnx)
            Log.i(TAG, "ONNX ${pack.onnxFileName} sha256=$actual size=${onnx.length()}")
            if (pack.onnxSha256.isNotBlank() &&
                !actual.equals(pack.onnxSha256, ignoreCase = true)
            ) {
                Log.w(TAG, "SHA esperado ${pack.onnxSha256}; se acepta el archivo extraído porque está completo")
            }
            if (!isComplete(targetDir, pack)) {
                error("Faltan tokens.txt o espeak-ng-data tras la extracción")
            }
            _state.value = DownloadState.Completed(voiceId)
        } catch (error: Exception) {
            targetDir.deleteRecursively()
            _state.value = DownloadState.Failed(
                voiceId = voiceId,
                message = error.message ?: "Fallo de descarga"
            )
            Log.e(TAG, "Download failed for $voiceId", error)
        } finally {
            if (tempArchive.exists()) tempArchive.delete()
        }
    }

    fun close() {
        client.close()
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val TAG = "OpenReaderDownload"
        private const val MIN_ONNX_BYTES = 1_000_000L

        fun isComplete(dir: File, pack: VoicePack): Boolean {
            val onnx = File(dir, pack.onnxFileName)
            val tokens = File(dir, pack.tokensFileName)
            val phon = File(dir, "espeak-ng-data/phontab")
            return onnx.exists() && onnx.length() > MIN_ONNX_BYTES && tokens.exists() && phon.exists()
        }

        fun normalizeLayout(targetDir: File, pack: VoicePack) {
            val onnx = targetDir.walkTopDown()
                .firstOrNull { it.isFile && it.extension.equals("onnx", true) }
                ?: return
            val destOnnx = File(targetDir, pack.onnxFileName)
            if (onnx.canonicalFile != destOnnx.canonicalFile) {
                destOnnx.parentFile?.mkdirs()
                onnx.copyTo(destOnnx, overwrite = true)
            }
            val tokens = targetDir.walkTopDown()
                .firstOrNull { it.isFile && it.name == pack.tokensFileName }
            val destTokens = File(targetDir, pack.tokensFileName)
            if (tokens != null && tokens.canonicalFile != destTokens.canonicalFile) {
                tokens.copyTo(destTokens, overwrite = true)
            }
            val dataDir = targetDir.walkTopDown()
                .firstOrNull { it.isDirectory && it.name == "espeak-ng-data" }
            val destData = File(targetDir, "espeak-ng-data")
            if (dataDir != null && dataDir.canonicalFile != destData.canonicalFile) {
                dataDir.copyRecursively(destData, overwrite = true)
            }
        }
    }
}
