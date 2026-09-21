package org.openreader.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ReadingProgressEntity::class],
    version = 1,
    exportSchema = false
)
abstract class OpenReaderDatabase : RoomDatabase() {
    abstract fun readingProgressDao(): ReadingProgressDao

    companion object {
        fun create(context: Context): OpenReaderDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                OpenReaderDatabase::class.java,
                "openreader.db"
            ).build()
    }
}
