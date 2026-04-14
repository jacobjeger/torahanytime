package com.torahanytime.audio.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Topic(
    val id: String,
    val slug: String? = null,
    val text: String,
    @Json(name = "img_url") val imgUrl: String? = null,
    val data: TopicData? = null
)

@JsonClass(generateAdapter = true)
data class TopicData(
    val id: Int,
    val name: String,
    val parent: Int? = null,
    val order: Int? = null,
    val slug: String? = null,
    @Json(name = "lecture_count") val lectureCount: Int = 0,
    @Json(name = "display_active") val displayActive: Boolean = true
)

@JsonClass(generateAdapter = true)
data class TopicsResponse(
    val count: Int,
    val total: Int,
    val items: List<Topic>
)
