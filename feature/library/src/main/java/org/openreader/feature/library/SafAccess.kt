package org.openreader.feature.library

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns

data class SafDocument(
    val uri: Uri,
    val displayName: String
)

fun persistReadPermission(resolver: ContentResolver, uri: Uri) {
    val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    runCatching { resolver.takePersistableUriPermission(uri, flags) }
}

fun queryDisplayName(resolver: ContentResolver, uri: Uri): String {
    resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) return cursor.getString(index)
        }
    }
    return uri.lastPathSegment ?: "documento.pdf"
}

fun listPdfsInTree(resolver: ContentResolver, treeUri: Uri): List<SafDocument> {
    val treeId = DocumentsContract.getTreeDocumentId(treeUri)
    val children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, treeId)
    val result = mutableListOf<SafDocument>()
    resolver.query(
        children,
        arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        ),
        null,
        null,
        null
    )?.use { cursor ->
        val idIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
        val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        val mimeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
        while (cursor.moveToNext()) {
            val mime = if (mimeIndex >= 0) cursor.getString(mimeIndex).orEmpty() else ""
            val name = if (nameIndex >= 0) cursor.getString(nameIndex).orEmpty() else ""
            val isPdf = mime.equals("application/pdf", ignoreCase = true) ||
                name.endsWith(".pdf", ignoreCase = true)
            if (!isPdf) continue
            val docId = cursor.getString(idIndex)
            val uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
            result += SafDocument(uri, name.ifBlank { "documento.pdf" })
        }
    }
    return result
}
