package com.torahanytime.audio.data.repository

import com.torahanytime.audio.TATApplication
import com.torahanytime.audio.data.api.ApiClient
import com.torahanytime.audio.data.api.AuthManager
import com.torahanytime.audio.data.local.entity.FavoriteLecture
import com.torahanytime.audio.data.model.Lecture
import kotlinx.coroutines.flow.Flow

object FavoriteRepository {
    private val dao by lazy { TATApplication.db.favoriteDao() }
    private val api = ApiClient.api

    fun getAll(): Flow<List<FavoriteLecture>> = dao.getAll()

    fun isFavorite(lectureId: Int): Flow<Boolean> = dao.isFavorite(lectureId)

    suspend fun toggleFavorite(lecture: Lecture) {
        val isFav = dao.isFavoriteSync(lecture.id)

        if (isFav) {
            dao.delete(lecture.id)
            if (AuthManager.getToken() != null) {
                try { api.unfavoriteLecture(mapOf("lecture_id" to lecture.id)) } catch (_: Exception) {}
            }
        } else {
            dao.upsert(
                FavoriteLecture(
                    lectureId = lecture.id,
                    title = lecture.title,
                    speakerName = lecture.speakerFullName,
                    thumbnailUrl = lecture.thumbnailUrl,
                    mp3Url = lecture.mp3Url,
                    duration = lecture.duration ?: 0,
                    languageName = lecture.languageName,
                    syncedWithServer = AuthManager.getToken() != null
                )
            )
            if (AuthManager.getToken() != null) {
                try { api.favoriteLecture(mapOf("lecture_id" to lecture.id)) } catch (_: Exception) {}
            }
        }
    }
}
