package org.openreader.core.pdf

/**
 * La muestra de encabezados sale del texto que ya se leyó.
 * [readPage] se llama una vez por página de la muestra.
 */
internal object ExtractionPlanner {
    fun repeatedFromSamples(
        sampleCount: Int,
        readPage: (page: Int) -> String
    ): SampledPages {
        if (sampleCount <= 0) {
            return SampledPages(emptyList(), emptySet())
        }
        val raws = ArrayList<String>(sampleCount)
        val edges = ArrayList<List<String>>(sampleCount)
        for (page in 1..sampleCount) {
            val raw = readPage(page)
            raws += raw
            edges += ParagraphNormalizer.edgeLines(raw)
        }
        return SampledPages(raws, ParagraphNormalizer.detectRepeatedLines(edges))
    }
}

internal data class SampledPages(
    val rawPages: List<String>,
    val repeatedLines: Set<String>
)
