package com.torahanytime.audio.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.torahanytime.audio.data.local.dao.*
import com.torahanytime.audio.data.local.entity.*

@Database(
    entities = [
        ListeningHistory::class,
        FavoriteLecture::class,
        DownloadedLecture::class,
        SearchHistoryEntry::class,
        QueueItem::class,
        Bookmark::class
    ],
    version = 2,
    exportSchema = false
)
abstract class TATDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun downloadDao(): DownloadDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun queueDao(): QueueDao
    abstract fun bookmarkDao(): BookmarkDao

    companion object {
        @Volatile
        private var INSTANCE: TATDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS bookmarks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        lectureId INTEGER NOT NULL,
                        position INTEGER NOT NULL,
                        note TEXT NOT NULL DEFAULT '',
                        lectureTitle TEXT NOT NULL DEFAULT '',
                        speakerName TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }

        fun getInstance(context: Context): TATDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TATDatabase::class.java,
                    "tat_audio.db"
                )
                .addMigrations(MIGRATION_1_2)
                .build().also {
                    INSTANCE = it
                }
            }
        }
    }
}
