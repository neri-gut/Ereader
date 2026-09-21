package org.openreader.core.database

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.security.MessageDigest

/**
 * Identificador unívoco de un PDF: SHA-256 de los primeros 8 MB
 * (o del archivo completo si es menor). Independiente de la ruta/URI.
 */
class DocumentIdHasher(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun hash(input: InputStream, fileSize: Long = -1L): String = withContext(ioDispatcher) {
        hashBlocking(input, fileSize)
    }

    companion object {
        const val MAX_BYTES: Int = 8 * 1024 * 1024

        fun hashBlocking(input: InputStream, fileSize: Long = -1L): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var remaining = MAX_BYTES
            while (remaining > 0) {
                val toRead = minOf(buffer.size, remaining)
                val read = input.read(buffer, 0, toRead)
                if (read == -1) break
                digest.update(buffer, 0, read)
                remaining -= read
            }
            if (fileSize > 0) {
                var size = fileSize
                repeat(8) {
                    digest.update((size and 0xFF).toByte())
                    size = size ushr 8
                }
            }
            return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
        }
    }
}
