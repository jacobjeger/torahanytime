package com.torahanytime.audio.data.local.dao

import androidx.room.*
import com.torahanytime.audio.data.local.entity.Bookmark
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun getAll(): Flow<List<Bookmark>>

    @Query("SELECT * FROM bookmarks WHERE lectureId = :lectureId ORDER BY position ASC")
    fun getByLecture(lectureId: Int): Flow<List<Bookmark>>

    @Query("SELECT COUNT(*) FROM bookmarks WHERE lectureId = :lectureId")
    fun getCountForLecture(lectureId: Int): Flow<Int>

    @Insert
    suspend fun insert(bookmark: Bookmark)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("DELETE FROM bookmarks WHERE lectureId = :lectureId")
    suspend fun deleteByLecture(lectureId: Int)

    @Query("DELETE FROM bookmarks")
    suspend fun deleteAll()
}
