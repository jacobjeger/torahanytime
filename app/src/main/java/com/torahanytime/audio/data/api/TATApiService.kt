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
}
