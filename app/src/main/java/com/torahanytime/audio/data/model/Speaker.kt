package com.torahanytime.audio.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Speaker(
    val id: Int,
    @Json(name = "name_first") val nameFirst: String,
    @Json(name = "name_last") val nameLast: String,
    @Json(name = "title_text") val titleText: String? = null,
    @Json(name = "title_short") val titleShort: String? = null,
    val slug: String? = null,
    val photo: String? = null,
    val desc: String? = null,
    val female: Boolean? = false,
    @Json(name = "is_guest") val isGuest: Boolean? = false,
    @Json(name = "lecture_count") val lectureCount: Int? = 0,
    @Json(name = "short_link") val shortLink: String? = null,
    @Json(name = "no_download") val noDownload: Boolean? = false,
    @Json(name = "display_active") val displayActive: Boolean? = true,
    @Json(name = "view_female_level") val viewFemaleLevel: Int? = 0,
    @Json(name = "dial_in_ext") val dialInExt: Int? = null,
    @Json(name = "phone_system_short_extension") val phoneSystemShortExtension: String? = null
) {
    val fullName: String get() = "${titleShort ?: ""} $nameFirst $nameLast".trim()
    val photoUrl: String? get() = photo?.let {
        "https://images.weserv.nl/?url=https://torahanytime-files.sfo2.digitaloceanspaces.com/assets/flash/speakers/$it&w=200&h=200&fit=cover"
    }
}

@JsonClass(generateAdapter = true)
data class SpeakersResponse(
    val totalSpeakers: Int,
    val limit: Int,
    val offset: Int,
    val speakers: Map<String, List<Speaker>>
)
