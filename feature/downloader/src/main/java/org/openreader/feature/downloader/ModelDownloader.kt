package org.openreader.feature.downloader

import android.content.Context
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
        val onnx = File(dir, pack.onnxFileName)
        val tokens = File(dir, pack.tokensFileName)
        val phon = File(dir, "espeak-ng-data/phontab")
        return onnx.exists() && tokens.exists() && phon.exists()
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
            val onnx = File(targetDir, pack.onnxFileName)
            if (!onnx.exists()) error("El paquete no contiene ${pack.onnxFileName}")
            val actual = sha256(onnx)
            if (!actual.equals(pack.onnxSha256, ignoreCase = true)) {
                targetDir.deleteRecursively()
                error("SHA-256 no coincide para ${pack.onnxFileName}")
            }
            _state.value = DownloadState.Completed(voiceId)
        } catch (error: Exception) {
            targetDir.deleteRecursively()
            _state.value = DownloadState.Failed(
                voiceId = voiceId,
                message = error.message ?: "Fallo de descarga"
            )
            throw error
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
}
