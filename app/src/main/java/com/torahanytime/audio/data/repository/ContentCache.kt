package com.torahanytime.audio.data.repository

import com.torahanytime.audio.data.api.ApiClient
import com.torahanytime.audio.data.model.Lecture
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Singleton cache for lecture content. Pre-loads recent lectures on startup
 * and caches speaker lecture lists.
 */
object ContentCache {
    private val api = ApiClient.api
    private val mutex = Mutex()

    // Recent lectures for home screen
    private val _recentLectures = MutableStateFlow<List<Lecture>>(emptyList())
    val recentLectures: StateFlow<List<Lecture>> = _recentLectures

    private val _recentLoading = MutableStateFlow(false)
    val recentLoading: StateFlow<Boolean> = _recentLoading

    // Per-speaker lecture cache
    private val speakerLecturesCache = mutableMapOf<Int, List<Lecture>>()
    private val speakerLecturesMutex = Mutex()

    private val popularSpeakerIds = listOf(162, 287, 386, 61, 166, 289, 1227, 80, 371, 164)

    private var recentLoaded = false

    suspend fun ensureRecentLoaded() {
        if (recentLoaded) return
        mutex.withLock {
            if (recentLoaded) return
            recentLoaded = true
        }
        loadRecent()
    }

    private suspend fun loadRecent() {
        _recentLoading.value = true
        try {
            coroutineScope {
                val deferredLectures = popularSpeakerIds.map { speakerId ->
                    async {
                        try {
                            val lectures = api.getSpeakerLectures(speakerId, limit = 8, offset = 0)
                                .lecture
                                ?.filter { it.isShort != true && it.displayActive != false }
                                ?: emptyList()
                            // Cache per-speaker too
                            speakerLecturesMutex.withLock {
                                speakerLecturesCache[speakerId] = lectures
                            }
                            lectures
                        } catch (_: Exception) { emptyList() }
                    }
                }
                val allLectures = deferredLectures.awaitAll().flatten()
                    .sortedByDescending { it.dateCreated ?: it.dateRecorded }
                    .distinctBy { it.id }
                    .take(30)
                _recentLectures.value = allLectures
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _recentLoading.value = false
    }

    suspend fun getSpeakerLectures(speakerId: Int): List<Lecture> {
        // Check cache first
        speakerLecturesMutex.withLock {
            speakerLecturesCache[speakerId]?.let { return it }
        }
        // Fetch and cache
        return try {
            val lectures = api.getSpeakerLectures(speakerId, limit = 150, offset = 0)
                .lecture
                ?.filter { it.isShort != true && it.displayActive != false }
                ?: emptyList()
            speakerLecturesMutex.withLock {
                speakerLecturesCache[speakerId] = lectures
            }
            lectures
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
