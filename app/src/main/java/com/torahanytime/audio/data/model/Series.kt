package com.torahanytime.audio.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Series(
    val id: Int,
    val title: String,
    @Json(name = "speaker_id") val speakerId: Int? = null,
    @Json(name = "category_id") val categoryId: Int? = null,
    @Json(name = "language_id") val languageId: Int? = null,
    @Json(name = "display_active") val displayActive: Boolean = true,
    val completed: Boolean = false,
    val image: String? = null,
    @Json(name = "female_only") val femaleOnly: Boolean = false,
    @Json(name = "lecture_count") val lectureCount: Int = 0,
    @Json(name = "phone_system_short_extension") val phoneSystemShortExtension: String? = null
)
