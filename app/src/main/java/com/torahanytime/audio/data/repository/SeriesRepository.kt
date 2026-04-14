package com.torahanytime.audio.data.repository

import com.torahanytime.audio.data.api.ApiClient
import com.torahanytime.audio.data.model.Series

class SeriesRepository {
    private val api = ApiClient.api

    suspend fun getSeriesDetail(seriesId: Int): Series {
        return api.getSeriesDetail(seriesId)
    }
}
