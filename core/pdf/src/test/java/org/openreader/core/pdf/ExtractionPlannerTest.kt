package org.openreader.core.pdf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtractionPlannerTest {

    @Test
    fun headerSampleReadsEachPageOnce() {
        var reads = 0
        val sampled = ExtractionPlanner.repeatedFromSamples(3) { page ->
            reads += 1
            "OpenReader\nCuerpo de la página $page.\n$page"
        }
        assertEquals(3, reads)
        assertEquals(3, sampled.rawPages.size)
        assertTrue(sampled.repeatedLines.contains("openreader"))
        assertEquals("OpenReader\nCuerpo de la página 1.\n1", sampled.rawPages.first())
    }
}
