package org.openreader.core.model

/**
 * Entrada de la biblioteca local. El [contentUri] es un URI persistente de SAF.
 */
data class LibraryDocument(
    val fileHash: String,
    val fileName: String,
    val contentUri: String,
    val lastOpenedTimestamp: Long,
    val paragraphIndex: Int = 0,
    val totalParagraphs: Int = 0,
    val isFavorite: Boolean = false
)
