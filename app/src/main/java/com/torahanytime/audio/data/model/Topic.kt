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

@JsonClass(generateAdapter = true)
data class AllTopicsResponse(
    val topics: List<ApiCategory> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ApiCategory(
    val id: Int,
    val name: String,
    val slug: String? = null,
    val lectures: Int = 0,
    val speakers: Int = 0,
    @Json(name = "display_active") val displayActive: Boolean = true,
    @Json(name = "subCategory") val subCategories: List<ApiCategory> = emptyList()
) {
    /** Convert to a Topic for the UI */
    fun toTopic(): Topic = Topic(
        id = "topics_$id",
        text = name,
        slug = slug,
        data = TopicData(
            id = id,
            name = name,
            slug = slug,
            lectureCount = lectures,
            displayActive = displayActive
        )
    )
}
