package com.torahanytime.audio.data.local.dao

import androidx.room.*
import com.torahanytime.audio.data.local.entity.ListeningHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM listening_history ORDER BY lastPlayedAt DESC")
    fun getAll(): Flow<List<ListeningHistory>>

    @Query("SELECT * FROM listening_history ORDER BY lastPlayedAt DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<ListeningHistory>>

    @Query("SELECT * FROM listening_history WHERE lectureId = :lectureId")
    suspend fun getByLectureId(lectureId: Int): ListeningHistory?

    @Query("SELECT * FROM listening_history WHERE lectureId = :lectureId")
    fun getByLectureIdFlow(lectureId: Int): Flow<ListeningHistory?>

    @Upsert
    suspend fun upsert(entry: ListeningHistory)

    @Query("DELETE FROM listening_history WHERE lectureId = :lectureId")
    suspend fun delete(lectureId: Int)

    @Query("DELETE FROM listening_history")
    suspend fun deleteAll()
}
