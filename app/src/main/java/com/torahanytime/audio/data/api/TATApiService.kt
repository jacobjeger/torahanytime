package com.torahanytime.audio.data.api

import com.torahanytime.audio.data.model.*
import retrofit2.http.*

interface TATApiService {

    @GET("search/speakers/alphabet")
    suspend fun getSpeakers(
        @Query("include-guest") includeGuest: Boolean = true,
        @Query("limit") limit: Int = 30,
        @Query("offset") offset: Int = 0
    ): SpeakersResponse

    @GET("speakers/{id}")
    suspend fun getSpeakerDetail(
        @Path("id") speakerId: Int,
        @Query("limit") limit: Int = 30
    ): Speaker

    @GET("speakers/{id}/lectures")
    suspend fun getSpeakerLectures(
        @Path("id") speakerId: Int,
        @Query("project_id") projectId: Int = 1,
        @Query("limit") limit: Int = 150,
        @Query("offset") offset: Int = 0
    ): LecturesResponse

    @GET("topics")
    suspend fun getAllTopics(
        @Query("project_id") projectId: Int = 1
    ): AllTopicsResponse

    @GET("search/topics")
    suspend fun searchTopics(
        @Query("filter") filter: String,
        @Query("limit") limit: Int = 30,
        @Query("project_id") projectId: Int = 1,
        @Query("start") start: Int = 0
    ): TopicsResponse

    @GET("series/{id}")
    suspend fun getSeriesDetail(
        @Path("id") seriesId: Int,
        @Query("limit") limit: Int = 30
    ): Series

    @GET("series/{id}/lectures")
    suspend fun getSeriesLectures(
        @Path("id") seriesId: Int,
        @Query("limit") limit: Int = 30,
        @Query("offset") offset: Int = 0,
        @Query("project_id") projectId: Int = 1
    ): SeriesLecturesResponse

    @POST("lectures/getViewCounts")
    suspend fun getViewCounts(@Body lectureIds: List<Int>): Map<String, Int>

    @GET("users/lectures/{id}/comments")
    suspend fun getLectureComments(@Path("id") lectureId: Int): List<Any>

    @GET("lectures/{id}/dedications")
    suspend fun getLectureDedications(
        @Path("id") lectureId: Int,
        @Query("date") date: String
    ): Any

    @GET("pins")
    suspend fun getPinnedContent(
        @Query("end_time.gte") endTimeGte: String,
        @Query("limit") limit: Int = 30,
        @Query("list_type") listType: String = "speaker",
        @Query("start_time.lte") startTimeLte: String,
        @Query("with_lectures") withLectures: Boolean = true
    ): List<Pin>

    @GET("dedication-slots/homepage")
    suspend fun getHomepageDedications(@Query("date") date: String): Any

    @GET("sponsor-dedications")
    suspend fun getSponsorDedications(): Any

    @GET("search/lectures")
    suspend fun searchLectures(
        @Query("filter") filter: String,
        @Query("limit") limit: Int = 20,
        @Query("project_id") projectId: Int = 1,
        @Query("start") start: Int = 0
    ): SearchLecturesResponse

    // Auth
    @POST("login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @GET("users/profile")
    suspend fun getUserProfile(): UserProfile

    @GET("lectures/watch-later-with-total")
    suspend fun getWatchLater(
        @Query("limit") limit: Int = 1000,
        @Query("offset") offset: Int = 0
    ): WatchLaterResponse

    @GET("users/follow")
    suspend fun getFollowedSpeakers(): FollowingResponse

    @GET("playlist")
    suspend fun getPlaylists(
        @Query("limit") limit: Int = 30,
        @Query("offset") offset: Int = 0,
        @Query("lecture-details") lectureDetails: Boolean = false
    ): PlaylistsResponse

    // Favorites
    @POST("users/lectures/favorite")
    suspend fun favoriteLecture(@Body body: Map<String, Int>): Any

    @POST("users/lectures/unfavorite")
    suspend fun unfavoriteLecture(@Body body: Map<String, Int>): Any

    // Follow/Unfollow speakers
    @POST("users/follow")
    suspend fun followSpeaker(@Body body: Map<String, Int>): Any

    @POST("users/unfollow")
    suspend fun unfollowSpeaker(@Body body: Map<String, Int>): Any
}
