package com.torahanytime.audio.data.repository

import com.torahanytime.audio.data.api.ApiClient
import com.torahanytime.audio.data.model.Topic

class TopicRepository {
    private val api = ApiClient.api

    suspend fun searchTopics(query: String): List<Topic> {
        val response = api.searchTopics(filter = query)
        return response.items
    }
}
