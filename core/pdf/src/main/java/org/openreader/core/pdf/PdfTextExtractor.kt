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
import kotlinx.coroutines.yield
import org.openreader.core.model.ParagraphData
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class PdfTextExtractor(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    fun extract(uri: Uri): Flow<ParagraphData> = flow {
        ensureInitialized(context)
        val file = materialize(context, uri)
        extractFromFile(file).collect { emit(it) }
    }.flowOn(ioDispatcher)

    fun extractFromFile(
        file: File,
        onProgress: (page: Int, total: Int) -> Unit = { _, _ -> }
    ): Flow<ParagraphData> = flow {
        ensureInitialized(context)
        PDDocument.load(file).use { document ->
            document.resourceCache = DiscardingResourceCache()
            val stripper = PDFTextStripper().apply {
                sortByPosition = false
            }
            val total = document.numberOfPages
            onProgress(0, total)
            val sampleCount = minOf(total, HEADER_SAMPLE_PAGES)
            val edgeLines = ArrayList<List<String>>(sampleCount)
            for (page in 1..sampleCount) {
                edgeLines += pageEdgeLines(stripper, document, page)
                yield()
            }
            val repeated = ParagraphNormalizer.detectRepeatedLines(edgeLines)
            var nextId = 0
            var carry = ""
            for (page in 1..total) {
                onProgress(page, total)
                val raw = pageText(stripper, document, page)
                val chunk = ParagraphNormalizer.processPage(
                    rawText = raw,
                    repeated = repeated,
                    carry = carry
                )
                chunk.paragraphs.forEach { text ->
                    emit(ParagraphData(id = nextId, text = text, pageNumber = page))
                    nextId += 1
                }
                carry = chunk.carry
                yield()
            }
            if (carry.isNotBlank()) {
                emit(
                    ParagraphData(
                        id = nextId,
                        text = carry.trim(),
                        pageNumber = document.numberOfPages
                    )
                )
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

    private fun pageEdgeLines(
        stripper: PDFTextStripper,
        document: PDDocument,
        page: Int
    ): List<String> {
        val lines = ParagraphNormalizer.pageLines(pageText(stripper, document, page))
        if (lines.isEmpty()) return emptyList()
        return (lines.take(2) + lines.takeLast(2)).distinct()
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
