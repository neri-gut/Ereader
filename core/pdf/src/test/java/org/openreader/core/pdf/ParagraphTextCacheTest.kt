package org.openreader.core.pdf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.openreader.core.model.ParagraphData
import java.nio.file.Files

class ParagraphTextCacheTest {

    @Test
    fun roundTripKeepsNewlinesAndIds() {
        val root = Files.createTempDirectory("openreader-extracts").toFile()
        val cache = ParagraphTextCache(root)
        val hash = "a".repeat(64)
        val original = ParagraphCheckpoint(
            paragraphs = listOf(
                ParagraphData(0, "Primera línea\nsigue", 1),
                ParagraphData(1, "Página dos", 2)
            ),
            nextPage = 3,
            carry = "sigue\nen la otra",
            repeated = setOf("openreader"),
            done = false
        )
        cache.save(hash, original)
        assertEquals(original, cache.load(hash))
    }

    @Test
    fun versionOneCacheIsDiscarded() {
        val root = Files.createTempDirectory("openreader-extracts").toFile()
        val hash = "d".repeat(64)
        val file = root.resolve("$hash.txt")
        root.mkdirs()
        file.writeText("openreader-paragraphs 2\n0\t1\tviejo\n")
        val cache = ParagraphTextCache(root)
        assertNull(cache.load(hash))
        assertEquals(false, file.exists())
    }

    @Test
    fun missingHashReturnsNull() {
        val root = Files.createTempDirectory("openreader-extracts").toFile()
        val cache = ParagraphTextCache(root)
        assertNull(cache.load("b".repeat(64)))
    }

    @Test
    fun corruptFileIsDiscarded() {
        val root = Files.createTempDirectory("openreader-extracts").toFile()
        val cache = ParagraphTextCache(root)
        val hash = "c".repeat(64)
        val file = root.resolve("$hash.txt")
        root.mkdirs()
        file.writeText("no-es-el-formato\n")
        assertNull(cache.load(hash))
        assertEquals(false, file.exists())
    }
}
