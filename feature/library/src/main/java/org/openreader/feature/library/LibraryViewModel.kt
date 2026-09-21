package org.openreader.feature.library

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.openreader.core.database.DocumentIdHasher
import org.openreader.core.database.ProgressRepository
import org.openreader.core.model.LibraryDocument
import org.openreader.core.model.ReadingProgress

class LibraryViewModel(
    private val progressRepository: ProgressRepository,
    private val hasher: DocumentIdHasher
) : ViewModel() {

    val documents: StateFlow<List<LibraryDocument>> = progressRepository
        .observeLibrary()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun importDocument(context: Context, uri: Uri, displayName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            persistReadPermission(context.contentResolver, uri)
            val hash = context.contentResolver.openInputStream(uri)?.use { hasher.hash(it) }
                ?: return@launch
            val existing = progressRepository.getProgress(hash)
            val progress = existing?.copy(
                fileName = displayName,
                lastReadTimestamp = System.currentTimeMillis()
            ) ?: ReadingProgress(
                fileHash = hash,
                fileName = displayName,
                paragraphIndex = 0,
                charOffset = 0,
                totalParagraphs = 0
            )
            progressRepository.saveProgress(progress, uri.toString())
        }
    }

    fun importTree(context: Context, treeUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            persistReadPermission(context.contentResolver, treeUri)
            listPdfsInTree(context.contentResolver, treeUri).forEach { doc ->
                persistReadPermission(context.contentResolver, doc.uri)
                importDocument(context, doc.uri, doc.displayName)
            }
        }
    }

    fun remove(fileHash: String) {
        viewModelScope.launch(Dispatchers.IO) {
            progressRepository.remove(fileHash)
        }
    }

    class Factory(
        private val progressRepository: ProgressRepository,
        private val hasher: DocumentIdHasher
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LibraryViewModel(progressRepository, hasher) as T
        }
    }
}
