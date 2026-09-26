package org.openreader.core.pdf

import org.openreader.core.model.ParagraphData
import java.io.File

data class ParagraphCheckpoint(
    val paragraphs: List<ParagraphData>,
    val nextPage: Int,
    val carry: String,
    val repeated: Set<String>,
    val done: Boolean
)

/**
 * Párrafos ya normalizados, un archivo por hash de documento.
 * Un checkpoint incompleto permite seguir en [ParagraphCheckpoint.nextPage].
 */
class ParagraphTextCache(private val root: File) {

    fun load(fileHash: String): ParagraphCheckpoint? {
        val file = fileFor(fileHash) ?: return null
        if (!file.isFile) return null
        return try {
            file.bufferedReader().use { reader ->
                val header = reader.readLine()
                if (header != HEADER) {
                    file.delete()
                    return null
                }
                var nextPage = 1
                var done = false
                var carry = ""
                val repeated = linkedSetOf<String>()
                while (true) {
                    val line = reader.readLine() ?: run {
                        file.delete()
                        return null
                    }
                    if (line == BODY) break
                    when {
                        line.startsWith("nextPage=") ->
                            nextPage = line.substringAfter("=").toIntOrNull() ?: 1
                        line.startsWith("done=") -> done = line.substringAfter("=") == "1"
                        line.startsWith("carry=") -> carry = unescape(line.substringAfter("="))
                        line.startsWith("repeated=") -> {
                            val value = unescape(line.substringAfter("="))
                            if (value.isNotEmpty()) repeated += value
                        }
                    }
                }
                val paragraphs = mutableListOf<ParagraphData>()
                while (true) {
                    val line = reader.readLine() ?: break
                    val parsed = decode(line) ?: run {
                        file.delete()
                        return null
                    }
                    paragraphs += parsed
                }
                ParagraphCheckpoint(
                    paragraphs = paragraphs,
                    nextPage = nextPage.coerceAtLeast(1),
                    carry = carry,
                    repeated = repeated,
                    done = done
                )
            }
        } catch (_: Exception) {
            file.delete()
            null
        }
    }

    fun save(fileHash: String, checkpoint: ParagraphCheckpoint) {
        val file = fileFor(fileHash) ?: return
        root.mkdirs()
        val tmp = File(root, "${file.name}.tmp")
        tmp.bufferedWriter().use { writer ->
            writer.append(HEADER)
            writer.append('\n')
            writer.append("nextPage=")
            writer.append(checkpoint.nextPage.coerceAtLeast(1).toString())
            writer.append('\n')
            writer.append("done=")
            writer.append(if (checkpoint.done) "1" else "0")
            writer.append('\n')
            writer.append("carry=")
            writer.append(escape(checkpoint.carry))
            writer.append('\n')
            checkpoint.repeated.forEach { line ->
                writer.append("repeated=")
                writer.append(escape(line))
                writer.append('\n')
            }
            writer.append(BODY)
            writer.append('\n')
            checkpoint.paragraphs.forEach { paragraph ->
                writer.append(paragraph.id.toString())
                writer.append('\t')
                writer.append(paragraph.pageNumber.toString())
                writer.append('\t')
                writer.append(escape(paragraph.text))
                writer.append('\n')
            }
        }
        if (file.exists() && !file.delete()) {
            tmp.delete()
            return
        }
        if (!tmp.renameTo(file)) {
            tmp.copyTo(file, overwrite = true)
            tmp.delete()
        }
    }

    private fun fileFor(fileHash: String): File? {
        if (!HASH.matches(fileHash)) return null
        return File(root, "$fileHash.txt")
    }

    private fun decode(line: String): ParagraphData? {
        val first = line.indexOf('\t')
        if (first <= 0) return null
        val second = line.indexOf('\t', first + 1)
        if (second <= first) return null
        val id = line.substring(0, first).toIntOrNull() ?: return null
        val page = line.substring(first + 1, second).toIntOrNull() ?: return null
        return ParagraphData(id = id, text = unescape(line.substring(second + 1)), pageNumber = page)
    }

    private fun escape(text: String): String =
        text.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "").replace("\t", " ")

    private fun unescape(text: String): String {
        val out = StringBuilder(text.length)
        var escaped = false
        text.forEach { char ->
            if (escaped) {
                out.append(if (char == 'n') '\n' else char)
                escaped = false
            } else if (char == '\\') {
                escaped = true
            } else {
                out.append(char)
            }
        }
        if (escaped) out.append('\\')
        return out.toString()
    }

    companion object {
        private const val HEADER = "openreader-paragraphs 3"
        private const val BODY = "---"
        private val HASH = Regex("^[0-9a-fA-F]{16,128}$")
    }
}
