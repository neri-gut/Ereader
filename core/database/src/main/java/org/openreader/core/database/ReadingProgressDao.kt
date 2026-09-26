package org.openreader.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingProgressDao {
    @Upsert
    suspend fun upsert(entity: ReadingProgressEntity)

    @Query("SELECT * FROM reading_progress WHERE file_hash = :fileHash LIMIT 1")
    suspend fun getByHash(fileHash: String): ReadingProgressEntity?

    @Query("SELECT * FROM reading_progress ORDER BY last_read_timestamp DESC")
    fun observeLibrary(): Flow<List<ReadingProgressEntity>>

    @Query("DELETE FROM reading_progress WHERE file_hash = :fileHash")
    suspend fun deleteByHash(fileHash: String)

    @Query("UPDATE reading_progress SET is_favorite = :favorite WHERE file_hash = :fileHash")
    suspend fun setFavorite(fileHash: String, favorite: Boolean)
}
