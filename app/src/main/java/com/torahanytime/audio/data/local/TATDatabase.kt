package com.torahanytime.audio.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.torahanytime.audio.data.local.dao.*
import com.torahanytime.audio.data.local.entity.*

@Database(
    entities = [
        ListeningHistory::class,
        FavoriteLecture::class,
        DownloadedLecture::class,
        SearchHistoryEntry::class,
        QueueItem::class
    ],
    version = 1,
    exportSchema = false
)
abstract class TATDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun downloadDao(): DownloadDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun queueDao(): QueueDao

    companion object {
        @Volatile
        private var INSTANCE: TATDatabase? = null

        fun getInstance(context: Context): TATDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TATDatabase::class.java,
                    "tat_audio.db"
                ).fallbackToDestructiveMigration().build().also {
                    INSTANCE = it
                }
            }
        }
    }
}
