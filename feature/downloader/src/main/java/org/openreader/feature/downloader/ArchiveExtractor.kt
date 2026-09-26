package org.openreader.feature.downloader

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.File
import java.io.FileInputStream

object ArchiveExtractor {
    fun extractTarBz2(archive: File, destination: File) {
        destination.mkdirs()
        FileInputStream(archive).use { fileStream ->
            BZip2CompressorInputStream(fileStream).use { bzip ->
                TarArchiveInputStream(bzip).use { tar ->
                    var entry: TarArchiveEntry? = tar.nextEntry
                    while (entry != null) {
                        val relative = stripRoot(entry.name)
                        if (relative.isBlank()) {
                            entry = tar.nextEntry
                            continue
                        }
                        val outFile = File(destination, relative).canonicalFile
                        if (!outFile.path.startsWith(destination.canonicalPath)) {
                            error("Ruta inválida en el archivo: ${entry.name}")
                        }
                        if (entry.isDirectory) {
                            outFile.mkdirs()
                        } else {
                            outFile.parentFile?.mkdirs()
                            outFile.outputStream().use { output -> tar.copyTo(output) }
                        }
                        entry = tar.nextEntry
                    }
                }
            }
        }
    }

    private fun stripRoot(name: String): String {
        val normalized = name.replace('\\', '/').trimStart('/')
        val slash = normalized.indexOf('/')
        return if (slash >= 0) normalized.substring(slash + 1) else normalized
    }
}
