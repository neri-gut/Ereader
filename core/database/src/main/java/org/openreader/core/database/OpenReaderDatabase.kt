package org.openreader.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ReadingProgressEntity::class],
    version = 2,
    exportSchema = false
)
abstract class OpenReaderDatabase : RoomDatabase() {
    abstract fun readingProgressDao(): ReadingProgressDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE reading_progress ADD COLUMN is_favorite INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        fun create(context: Context): OpenReaderDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                OpenReaderDatabase::class.java,
                "openreader.db"
            ).addMigrations(MIGRATION_1_2).build()
    }
}
