package com.torahanytime.audio.data.repository

import com.torahanytime.audio.data.api.ApiClient
import com.torahanytime.audio.data.model.Lecture

class LectureRepository {
    private val api = ApiClient.api

    suspend fun getSpeakerLectures(speakerId: Int, offset: Int = 0, limit: Int = 150): List<Lecture> {
        val response = api.getSpeakerLectures(speakerId, offset = offset, limit = limit)
        return (response.lecture ?: emptyList()).filter { it.isShort != true && it.displayActive != false }
    }

    suspend fun getSeriesLectures(seriesId: Int, offset: Int = 0, limit: Int = 30): List<Lecture> {
        val response = api.getSeriesLectures(seriesId, offset = offset, limit = limit)
        return (response.seriesLectures?.values?.toList() ?: emptyList()).filter { it.isShort != true }
    }
}
