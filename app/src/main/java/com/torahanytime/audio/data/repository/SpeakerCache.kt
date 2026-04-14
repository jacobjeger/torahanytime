package com.torahanytime.audio.data.repository

import com.torahanytime.audio.data.api.ApiClient
import com.torahanytime.audio.data.model.Speaker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Singleton cache for all speakers. Loaded once, shared across the app.
 * Survives ViewModel destruction (e.g. navigating away and back).
 */
object SpeakerCache {
    private val api = ApiClient.api
    private val mutex = Mutex()

    private val _speakers = MutableStateFlow<Map<String, List<Speaker>>>(emptyMap())
    val speakers: StateFlow<Map<String, List<Speaker>>> = _speakers

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _totalCount = MutableStateFlow(0)
    val totalCount: StateFlow<Int> = _totalCount

    private var loaded = false
    private var loadingStarted = false

    val isLoaded: Boolean get() = loaded

    suspend fun ensureLoaded() {
        if (loaded || loadingStarted) return
        mutex.withLock {
            if (loaded || loadingStarted) return
            loadingStarted = true
        }
        loadAll()
    }

    private suspend fun loadAll() {
        _loading.value = true
        var offset = 0
        var totalSpeakers = Int.MAX_VALUE
        try {
            while (offset < totalSpeakers) {
                val response = api.getSpeakers(offset = offset, limit = 30)
                totalSpeakers = response.totalSpeakers
                val current = _speakers.value.toMutableMap()
                var newCount = 0
                response.speakers.forEach { (letter, list) ->
                    if (list.isNotEmpty()) {
                        current[letter] = (current[letter] ?: emptyList()) + list
                        newCount += list.size
                    }
                }
                _speakers.value = current.toSortedMap()
                _totalCount.value = current.values.sumOf { it.size }
                offset += response.limit
                if (newCount == 0) break
            }
            loaded = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _loading.value = false
    }

    fun searchSpeakers(query: String): List<Speaker> {
        return _speakers.value.values.flatten().filter {
            it.fullName.contains(query, ignoreCase = true)
        }
    }

    fun getSpeakerById(id: Int): Speaker? {
        return _speakers.value.values.flatten().find { it.id == id }
    }
}
