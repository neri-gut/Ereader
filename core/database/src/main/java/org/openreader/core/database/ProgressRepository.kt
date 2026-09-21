package org.openreader.core.database

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.openreader.core.model.LibraryDocument
import org.openreader.core.model.ReadingProgress

class ProgressRepository(
    private val dao: ReadingProgressDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    fun observeLibrary(): Flow<List<LibraryDocument>> =
        dao.observeLibrary().map { rows -> rows.map { it.toLibraryDocument() } }

    suspend fun getProgress(fileHash: String): ReadingProgress? = withContext(ioDispatcher) {
        dao.getByHash(fileHash)?.toProgress()
    }

    suspend fun getDocument(fileHash: String): LibraryDocument? = withContext(ioDispatcher) {
        dao.getByHash(fileHash)?.toLibraryDocument()
    }

    suspend fun saveProgress(progress: ReadingProgress, contentUri: String) =
        withContext(ioDispatcher) {
            val favorite = dao.getByHash(progress.fileHash)?.isFavorite ?: false
            dao.upsert(ReadingProgressEntity.from(progress, contentUri, favorite))
        }

    suspend fun setFavorite(fileHash: String, favorite: Boolean) = withContext(ioDispatcher) {
        dao.setFavorite(fileHash, favorite)
    }

    suspend fun remove(fileHash: String) = withContext(ioDispatcher) {
        dao.deleteByHash(fileHash)
    }
}
