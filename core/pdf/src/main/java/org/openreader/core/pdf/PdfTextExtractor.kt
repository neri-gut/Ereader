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
import org.openreader.core.model.ParagraphData
import java.util.concurrent.atomic.AtomicBoolean

class PdfTextExtractor(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    fun extract(uri: Uri): Flow<ParagraphData> = flow {
        ensureInitialized(context)
        context.contentResolver.openInputStream(uri)?.use { input ->
            PDDocument.load(input).use { document ->
                val stripper = PDFTextStripper().apply {
                    sortByPosition = true
                }
                val edgeLines = ArrayList<List<String>>(document.numberOfPages)
                for (page in 1..document.numberOfPages) {
                    stripper.startPage = page
                    stripper.endPage = page
                    val lines = ParagraphNormalizer.pageLines(stripper.getText(document))
                    edgeLines += (lines.take(2) + lines.takeLast(2))
                }
                val repeated = ParagraphNormalizer.detectRepeatedLines(edgeLines)
                var nextId = 0
                var carry = ""
                for (page in 1..document.numberOfPages) {
                    stripper.startPage = page
                    stripper.endPage = page
                    val chunk = ParagraphNormalizer.processPage(
                        rawText = stripper.getText(document),
                        repeated = repeated,
                        carry = carry
                    )
                    chunk.paragraphs.forEach { text ->
                        emit(ParagraphData(id = nextId, text = text, pageNumber = page))
                        nextId += 1
                    }
                    carry = chunk.carry
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
        } ?: error("No se pudo abrir el documento")
    }.flowOn(ioDispatcher)

    companion object {
        private val initialized = AtomicBoolean(false)

        fun ensureInitialized(context: Context) {
            if (initialized.compareAndSet(false, true)) {
                PDFBoxResourceLoader.init(context.applicationContext)
            }
        }
    }
}
