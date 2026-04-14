package com.torahanytime.audio.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Lecture(
    val id: Int,
    val title: String,
    val slug: String? = null,
    @Json(name = "title_rtl") val titleRtl: Boolean = false,
    val duration: Int? = 0,
    @Json(name = "date_recorded") val dateRecorded: String? = null,
    @Json(name = "date_created") val dateCreated: String? = null,
    val speaker: Int? = 0,
    @Json(name = "speaker_name_first") val speakerNameFirst: String? = null,
    @Json(name = "speaker_name_last") val speakerNameLast: String? = null,
    @Json(name = "language_name") val languageName: String? = null,
    val ladies: Boolean? = false,
    @Json(name = "is_short") val isShort: Boolean? = false,
    @Json(name = "private") val isPrivate: Boolean? = false,
    @Json(name = "no_download") val noDownload: Boolean? = null,
    @Json(name = "display_active") val displayActive: Boolean? = true,
    val categories: List<Category>? = null,
    val subcategories: List<Category>? = null,
    @Json(name = "thumbnail_url") val thumbnailUrl: String? = null,
    @Json(name = "mp3_url") val mp3Url: String? = null,
    @Json(name = "mp4_url") val mp4Url: String? = null,
    @Json(name = "m3u8_url") val m3u8Url: String? = null,
    @Json(name = "published_by_name") val publishedByName: String? = null,
    val org: Int? = null,
    val category: Int? = null,
    val subcategory: Int? = null,
    val language: Int? = null,
    @Json(name = "phone_system_short_extension") val phoneSystemShortExtension: String? = null,
    @Json(name = "is_only_watchable_by_female") val isOnlyWatchableByFemale: Boolean? = false,
    @Json(name = "is_only_listenable_by_female") val isOnlyListenableByFemale: Boolean? = false,
    @Json(name = "is_only_discoverable_by_female") val isOnlyDiscoverableByFemale: Boolean? = false
) {
    val speakerFullName: String get() = "${speakerNameFirst ?: ""} ${speakerNameLast ?: ""}".trim()
    val durationFormatted: String get() {
        val dur = duration ?: 0
        val hours = dur / 3600
        val minutes = (dur % 3600) / 60
        val seconds = dur % 60
        return if (hours > 0) String.format("%d:%02d:%02d", hours, minutes, seconds)
        else String.format("%d:%02d", minutes, seconds)
    }
}

@JsonClass(generateAdapter = true)
data class Category(
    val id: Int,
    val name: String,
    @Json(name = "english_name") val englishName: String? = null
)

@JsonClass(generateAdapter = true)
data class LecturesResponse(
    val lecture: List<Lecture>? = null
)

@JsonClass(generateAdapter = true)
data class SeriesLecturesResponse(
    @Json(name = "series_lectures") val seriesLectures: Map<String, Lecture>? = null
)
