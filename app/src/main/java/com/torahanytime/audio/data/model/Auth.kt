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
    val photo: String? = null
)
