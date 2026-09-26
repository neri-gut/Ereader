package org.openreader.core.pdf

import org.openreader.core.model.ParagraphData

data class PageText(
    val pageNumber: Int,
    val rawText: String
)

data class PageChunk(
    val paragraphs: List<String>,
    val carry: String
)

/**
 * Convierte texto crudo de PDF en párrafos continuos:
 * elimina encabezados/pies repetidos, números de página, une líneas
 * partidas y aplica de-hyphenation.
 */
object ParagraphNormalizer {

    fun normalize(pages: List<PageText>): List<ParagraphData> {
        if (pages.isEmpty()) return emptyList()
        val repeated = detectRepeatedLines(pages.map { pageLines(it.rawText) })
        val result = mutableListOf<ParagraphData>()
        var carry = ""
        pages.forEach { page ->
            val chunk = processPage(page.rawText, repeated, carry)
            chunk.paragraphs.forEach { text ->
                result += ParagraphData(
                    id = result.size,
                    text = text,
                    pageNumber = page.pageNumber
                )
            }
            carry = chunk.carry
        }
        if (carry.isNotBlank()) {
            result += ParagraphData(
                id = result.size,
                text = carry.trim(),
                pageNumber = pages.last().pageNumber
            )
        }
        return result
    }

    fun detectRepeatedLines(pageLines: List<List<String>>): Set<String> {
        if (pageLines.size < 2) return emptySet()
        val candidates = mutableMapOf<String, Int>()
        pageLines.forEach { lines ->
            val edges = (lines.take(2) + lines.takeLast(2)).toSet()
            edges.forEach { line ->
                val key = canonicalize(line)
                if (key.length >= 3) {
                    candidates[key] = (candidates[key] ?: 0) + 1
                }
            }
        }
        val threshold = maxOf(2, (pageLines.size * 0.4f).toInt())
        return candidates.filterValues { it >= threshold }.keys
    }

    fun processPage(rawText: String, repeated: Set<String>, carry: String): PageChunk {
        val blocks = rawText.split(PARAGRAPH_BREAK).filter { it.isNotBlank() }
        if (blocks.isEmpty()) {
            val pending = carry.trim()
            return if (pending.isBlank() || !endsSentence(pending)) {
                PageChunk(emptyList(), pending)
            } else {
                PageChunk(listOf(pending), "")
            }
        }
        val emitted = mutableListOf<String>()
        var pending = carry
        blocks.forEachIndexed { index, block ->
            val lines = pageLines(block)
                .filterNot { line -> isPageNumber(line) || canonicalize(line) in repeated }
            val joined = joinLines(lines)
            val combined = when {
                pending.isBlank() -> joined
                joined.isBlank() -> pending
                else -> mergeCarry(pending, joined)
            }
            val parts = splitParagraphs(combined)
            val lastBlock = index == blocks.lastIndex
            if (!lastBlock) {
                emitted += parts
                pending = ""
            } else if (parts.isEmpty()) {
                pending = ""
            } else if (endsSentence(parts.last())) {
                emitted += parts
                pending = ""
            } else {
                emitted += parts.dropLast(1)
                pending = parts.last()
            }
        }
        return PageChunk(emitted, pending)
    }

    fun joinLines(lines: List<String>): String {
        if (lines.isEmpty()) return ""
        val builder = StringBuilder()
        var i = 0
        while (i < lines.size) {
            var current = lines[i].trim()
            while (i + 1 < lines.size) {
                val next = lines[i + 1].trim()
                val dehyphenated = dehyphenate(current, next)
                if (dehyphenated != null) {
                    current = dehyphenated
                    i++
                    continue
                }
                if (shouldJoinWrappedLine(current, next)) {
                    current = "$current $next"
                    i++
                    continue
                }
                break
            }
            if (builder.isNotEmpty()) builder.append('\n')
            builder.append(current)
            i++
        }
        return builder.toString()
    }

    fun dehyphenate(current: String, next: String): String? {
        if (!current.endsWith("-") || current.length < 2 || next.isEmpty()) return null
        val last = current[current.lastIndex - 1]
        val firstNext = next.first()
        if (!last.isLetter() || !firstNext.isLetter() || !firstNext.isLowerCase()) return null
        return current.dropLast(1) + next
    }

    fun splitParagraphs(text: String): List<String> =
        text.split(PARAGRAPH_BREAK)
            .map { it.replace('\n', ' ').replace(MULTI_SPACE, " ").trim() }
            .filter { it.isNotEmpty() }

    fun edgeLines(rawText: String): List<String> {
        val lines = pageLines(rawText)
        if (lines.isEmpty()) return emptyList()
        return (lines.take(2) + lines.takeLast(2)).distinct()
    }

    internal fun pageLines(rawText: String): List<String> =
        rawText.split('\n').map { it.trim() }.filter { it.isNotEmpty() }

    internal fun isPageNumber(line: String): Boolean =
        PAGE_NUMBER.matches(line.trim())

    private fun endsSentence(text: String): Boolean =
        text.isNotBlank() && SENTENCE_END.containsMatchIn(text.trim())

    private fun shouldJoinWrappedLine(current: String, next: String): Boolean {
        if (current.isEmpty() || next.isEmpty()) return false
        if (SENTENCE_END.containsMatchIn(current)) return false
        val start = next.first()
        return start.isLowerCase() || start == ',' || start == ';' || start == ':'
    }

    private fun mergeCarry(carry: String, joined: String): String {
        val first = joined.substringBefore('\n')
        val rest = joined.substringAfter('\n', missingDelimiterValue = "")
        val dehyphenated = dehyphenate(carry, first)
        val mergedHead = when {
            dehyphenated != null -> dehyphenated
            shouldJoinWrappedLine(carry, first) -> "$carry $first"
            else -> "$carry\n\n$first"
        }
        return if (rest.isEmpty()) mergedHead else "$mergedHead\n$rest"
    }

    private fun canonicalize(line: String): String =
        line.lowercase().replace(MULTI_SPACE, " ").trim()

    private val MULTI_SPACE = Regex("\\s+")
    private val PARAGRAPH_BREAK = Regex("\\n\\s*\\n+")
    private val SENTENCE_END = Regex("""[.!?…»"”']$""")
    private val PAGE_NUMBER = Regex("""^(?:página|page)?\s*-?\s*\d+\s*-?$""", RegexOption.IGNORE_CASE)
}
