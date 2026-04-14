package com.torahanytime.audio.data.repository

import com.torahanytime.audio.data.api.ApiClient
import com.torahanytime.audio.data.model.Speaker
import com.torahanytime.audio.data.model.SpeakersResponse

class SpeakerRepository {
    private val api = ApiClient.api

    suspend fun getSpeakers(offset: Int = 0, limit: Int = 30): SpeakersResponse {
        return api.getSpeakers(offset = offset, limit = limit)
    }

    suspend fun getSpeakerDetail(speakerId: Int): Speaker {
        return api.getSpeakerDetail(speakerId)
    }
}
