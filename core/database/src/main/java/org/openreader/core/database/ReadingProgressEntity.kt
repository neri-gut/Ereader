package org.openreader.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.openreader.core.model.LibraryDocument
import org.openreader.core.model.ReadingProgress

@Entity(tableName = "reading_progress")
data class ReadingProgressEntity(
    @PrimaryKey
    @ColumnInfo(name = "file_hash")
    val fileHash: String,
    @ColumnInfo(name = "file_name")
    val fileName: String,
    @ColumnInfo(name = "content_uri")
    val contentUri: String,
    @ColumnInfo(name = "paragraph_index")
    val paragraphIndex: Int,
    @ColumnInfo(name = "char_offset")
    val charOffset: Int,
    @ColumnInfo(name = "total_paragraphs")
    val totalParagraphs: Int,
    @ColumnInfo(name = "last_read_timestamp")
    val lastReadTimestamp: Long
) {
    fun toProgress(): ReadingProgress = ReadingProgress(
        fileHash = fileHash,
        fileName = fileName,
        paragraphIndex = paragraphIndex,
        charOffset = charOffset,
        totalParagraphs = totalParagraphs,
        lastReadTimestamp = lastReadTimestamp
    )

    fun toLibraryDocument(): LibraryDocument = LibraryDocument(
        fileHash = fileHash,
        fileName = fileName,
        contentUri = contentUri,
        lastOpenedTimestamp = lastReadTimestamp,
        paragraphIndex = paragraphIndex,
        totalParagraphs = totalParagraphs
    )

    companion object {
        fun from(
            progress: ReadingProgress,
            contentUri: String
        ): ReadingProgressEntity = ReadingProgressEntity(
            fileHash = progress.fileHash,
            fileName = progress.fileName,
            contentUri = contentUri,
            paragraphIndex = progress.paragraphIndex,
            charOffset = progress.charOffset,
            totalParagraphs = progress.totalParagraphs,
            lastReadTimestamp = progress.lastReadTimestamp
        )
    }
}
