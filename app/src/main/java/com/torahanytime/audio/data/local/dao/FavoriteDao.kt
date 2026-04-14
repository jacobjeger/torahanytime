package com.torahanytime.audio.data.local.dao

import androidx.room.*
import com.torahanytime.audio.data.local.entity.FavoriteLecture
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorite_lectures ORDER BY favoritedAt DESC")
    fun getAll(): Flow<List<FavoriteLecture>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_lectures WHERE lectureId = :lectureId)")
    fun isFavorite(lectureId: Int): Flow<Boolean>

    @Upsert
    suspend fun upsert(favorite: FavoriteLecture)

    @Query("DELETE FROM favorite_lectures WHERE lectureId = :lectureId")
    suspend fun delete(lectureId: Int)

    @Query("SELECT * FROM favorite_lectures WHERE syncedWithServer = 0")
    suspend fun getUnsynced(): List<FavoriteLecture>

    @Query("UPDATE favorite_lectures SET syncedWithServer = 1 WHERE lectureId = :lectureId")
    suspend fun markSynced(lectureId: Int)
}
