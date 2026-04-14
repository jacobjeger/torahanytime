package com.torahanytime.audio.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LoginRequest(
    val email: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class LoginResponse(
    val status: String? = null,
    val message: String? = null,
    val id: String? = null,
    val email: String? = null,
    val token: String? = null,
    val expiration: Long? = null
)

@JsonClass(generateAdapter = true)
data class UserProfile(
    val id: Int? = null,
    val email: String? = null,
    @Json(name = "first_name") val firstName: String? = null,
    @Json(name = "last_name") val lastName: String? = null,
    @Json(name = "screen_name") val screenName: String? = null,
    val photo: String? = null,
    @Json(name = "is_female") val isFemale: Boolean? = false
)

@JsonClass(generateAdapter = true)
data class WatchLaterResponse(
    val lectures: List<WatchLaterLecture>? = null
)

@JsonClass(generateAdapter = true)
data class WatchLaterLecture(
    val id: Int? = null,
    val title: String? = null,
    val thumbnail: String? = null,
    @Json(name = "date_recorded") val dateRecorded: String? = null,
    val speaker: Int? = null,
    val duration: Int? = null,
    @Json(name = "speaker_name_first") val speakerNameFirst: String? = null,
    @Json(name = "speaker_name_last") val speakerNameLast: String? = null,
    @Json(name = "speaker_title_short") val speakerTitleShort: String? = null
) {
    fun toLecture(): Lecture {
        val thumbUrl = if (!thumbnail.isNullOrEmpty()) {
            "https://www.torahanytime.com/i/$thumbnail"
        } else null
        return Lecture(
            id = id ?: 0,
            title = title ?: "",
            duration = duration,
            dateRecorded = dateRecorded,
            speaker = speaker,
            speakerNameFirst = speakerNameFirst,
            speakerNameLast = speakerNameLast,
            thumbnailUrl = thumbUrl
        )
    }
}

@JsonClass(generateAdapter = true)
data class FollowingResponse(
    val speakers: Map<String, FollowedSpeaker>? = null,
    val topics: Map<String, Any>? = null,
    val series: Map<String, Any>? = null
)

@JsonClass(generateAdapter = true)
data class FollowedSpeaker(
    val id: Int? = null,
    @Json(name = "name_first") val nameFirst: String? = null,
    @Json(name = "name_last") val nameLast: String? = null,
    val title: String? = null,
    val image: String? = null,
    val emailNotifications: Boolean? = null,
    val showInFollowTab: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class PlaylistsResponse(
    val playlists: Map<String, Any>? = null,
    val sortIds: List<Any>? = null
)
