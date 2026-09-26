package org.openreader.core.pdf

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.openreader.core.model.ParagraphData
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class PdfTextExtractor(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    fun extract(uri: Uri): Flow<ParagraphData> = flow {
        ensureInitialized(context)
        val file = materialize(context, uri)
        extractFromFile(file).collect { emit(it) }
    }.flowOn(ioDispatcher)

    fun extractFromFile(
        file: File,
        startPage: Int = 1,
        startId: Int = 0,
        initialCarry: String = "",
        knownRepeated: Set<String>? = null,
        onProgress: (page: Int, total: Int) -> Unit = { _, _ -> },
        onCheckpoint: (nextPage: Int, carry: String, repeated: Set<String>) -> Unit = { _, _, _ -> }
    ): Flow<ParagraphData> = flow {
        ensureInitialized(context)
        PDDocument.load(file).use { document ->
            document.resourceCache = DiscardingResourceCache()
            val stripper = PDFTextStripper().apply {
                sortByPosition = false
                paragraphEnd = "\n"
            }
            val total = document.numberOfPages
            val begin = startPage.coerceIn(1, total + 1)
            onProgress(begin - 1, total)
            if (begin > total) {
                if (initialCarry.isNotBlank()) {
                    emit(
                        ParagraphData(
                            id = startId,
                            text = initialCarry.trim(),
                            pageNumber = total
                        )
                    )
                }
                onCheckpoint(total + 1, "", knownRepeated.orEmpty())
                return@flow
            }
            val repeated: Set<String>
            val prefetched: List<String>
            if (knownRepeated != null || begin > 1) {
                repeated = knownRepeated.orEmpty()
                prefetched = emptyList()
            } else {
                val sampleCount = minOf(total, HEADER_SAMPLE_PAGES)
                val sampled = ExtractionPlanner.repeatedFromSamples(sampleCount) { page ->
                    pageText(stripper, document, page)
                }
                repeated = sampled.repeatedLines
                prefetched = sampled.rawPages
            }
            var nextId = startId
            var carry = initialCarry
            suspend fun emitPage(page: Int, raw: String) {
                onProgress(page, total)
                val chunk = withContext(defaultDispatcher) {
                    ParagraphNormalizer.processPage(
                        rawText = raw,
                        repeated = repeated,
                        carry = carry
                    )
                }
                chunk.paragraphs.forEach { text ->
                    emit(ParagraphData(id = nextId, text = text, pageNumber = page))
                    nextId += 1
                }
                carry = chunk.carry
                onCheckpoint(page + 1, carry, repeated)
                yield()
            }
            if (prefetched.isNotEmpty()) {
                prefetched.forEachIndexed { index, raw ->
                    emitPage(index + 1, raw)
                }
                for (page in (prefetched.size + 1)..total) {
                    emitPage(page, pageText(stripper, document, page))
                }
            } else {
                for (page in begin..total) {
                    emitPage(page, pageText(stripper, document, page))
                }
            }
            if (carry.isNotBlank()) {
                emit(
                    ParagraphData(
                        id = nextId,
                        text = carry.trim(),
                        pageNumber = total
                    )
                )
                onCheckpoint(total + 1, "", repeated)
            }
        }
    }.flowOn(ioDispatcher)

    private fun pageText(stripper: PDFTextStripper, document: PDDocument, page: Int): String {
        return try {
            stripper.startPage = page
            stripper.endPage = page
            stripper.getText(document)
        } catch (oom: OutOfMemoryError) {
            System.gc()
            ""
        } catch (_: Exception) {
            ""
        }
    }

    companion object {
        private const val HEADER_SAMPLE_PAGES = 12
        private val initialized = AtomicBoolean(false)

        fun ensureInitialized(context: Context) {
            if (initialized.compareAndSet(false, true)) {
                PDFBoxResourceLoader.init(context.applicationContext)
            }
        }

        fun materialize(context: Context, uri: Uri): File {
            val dir = File(context.cacheDir, "docs").apply { mkdirs() }
            val dest = File(dir, "doc-${uri.toString().hashCode().toUInt().toString(16)}.pdf")
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().buffered().use { output -> input.copyTo(output) }
            } ?: error("No se pudo abrir el documento")
            return dest
        }
    }
}
