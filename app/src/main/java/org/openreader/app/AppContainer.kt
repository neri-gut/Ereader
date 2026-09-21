package org.openreader.app

import android.content.Context
import kotlinx.coroutines.Dispatchers
import org.openreader.core.database.DocumentIdHasher
import org.openreader.core.database.OpenReaderDatabase
import org.openreader.core.database.PreferencesRepository
import org.openreader.core.database.ProgressRepository
import org.openreader.core.pdf.PdfTextExtractor
import org.openreader.core.tts.TtsController
import org.openreader.feature.downloader.DownloaderViewModel
import org.openreader.feature.downloader.ModelDownloader
import org.openreader.feature.library.LibraryViewModel
import org.openreader.feature.reader.ReaderViewModel

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val ioDispatcher = Dispatchers.IO
    private val defaultDispatcher = Dispatchers.Default

    private val database = OpenReaderDatabase.create(appContext)
    val progressRepository = ProgressRepository(database.readingProgressDao(), ioDispatcher)
    val preferencesRepository = PreferencesRepository(appContext, ioDispatcher)
    val documentIdHasher = DocumentIdHasher(ioDispatcher)
    val pdfTextExtractor = PdfTextExtractor(appContext, ioDispatcher)
    val modelDownloader = ModelDownloader(appContext, ioDispatcher)
    val ttsController = TtsController(appContext, ioDispatcher, defaultDispatcher).also { controller ->
        controller.setModelsDir(modelDownloader.modelsDirectory())
    }

    val libraryFactory = LibraryViewModel.Factory(progressRepository, documentIdHasher)
    val readerFactory = ReaderViewModel.Factory(
        extractor = pdfTextExtractor,
        hasher = documentIdHasher,
        progressRepository = progressRepository,
        preferencesRepository = preferencesRepository,
        ttsController = ttsController,
        modelDownloader = modelDownloader
    )
    val downloaderFactory = DownloaderViewModel.Factory(modelDownloader)
}
