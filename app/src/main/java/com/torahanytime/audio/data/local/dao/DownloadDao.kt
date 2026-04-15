package com.torahanytime.audio.data.local.dao

import androidx.room.*
import com.torahanytime.audio.data.local.entity.DownloadedLecture
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloaded_lectures ORDER BY downloadedAt DESC")
    fun getAll(): Flow<List<DownloadedLecture>>

    @Query("SELECT * FROM downloaded_lectures WHERE lectureId = :lectureId")
    suspend fun getByLectureId(lectureId: Int): DownloadedLecture?

    @Query("SELECT EXISTS(SELECT 1 FROM downloaded_lectures WHERE lectureId = :lectureId)")
    fun isDownloaded(lectureId: Int): Flow<Boolean>

    @Upsert
    suspend fun upsert(download: DownloadedLecture)

    @Query("DELETE FROM downloaded_lectures WHERE lectureId = :lectureId")
    suspend fun delete(lectureId: Int)

    @Query("SELECT SUM(fileSizeBytes) FROM downloaded_lectures")
    suspend fun getTotalSize(): Long?

    @Query("SELECT * FROM downloaded_lectures ORDER BY downloadedAt ASC")
    suspend fun getAllSync(): List<DownloadedLecture>

    @Query("SELECT COUNT(*) FROM downloaded_lectures")
    suspend fun getCount(): Int
}
