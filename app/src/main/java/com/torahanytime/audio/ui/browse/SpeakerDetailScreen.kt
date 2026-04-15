package com.torahanytime.audio.ui.browse

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.torahanytime.audio.data.api.ApiClient
import com.torahanytime.audio.data.api.AuthManager
import com.torahanytime.audio.data.model.Lecture
import com.torahanytime.audio.data.model.Speaker
import com.torahanytime.audio.data.repository.SpeakerCache
import com.torahanytime.audio.ui.common.ErrorRetryState
import com.torahanytime.audio.ui.common.InfiniteScrollHandler
import com.torahanytime.audio.ui.common.LectureItem
import com.torahanytime.audio.ui.common.PaginationLoadingItem
import com.torahanytime.audio.ui.theme.TATBlue
import com.torahanytime.audio.ui.theme.TATTextSecondary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SpeakerDetailViewModel : ViewModel() {
    private val api = ApiClient.api
    private val pageSize = 50

    private val _speaker = MutableStateFlow<Speaker?>(null)
    val speaker: StateFlow<Speaker?> = _speaker

    private val _lectures = MutableStateFlow<List<Lecture>>(emptyList())
    val lectures: StateFlow<List<Lecture>> = _lectures

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    private val _loadingMore = MutableStateFlow(false)
    val loadingMore: StateFlow<Boolean> = _loadingMore

    private val _hasMore = MutableStateFlow(true)
    val hasMore: StateFlow<Boolean> = _hasMore

    private val _isFollowing = MutableStateFlow(false)
    val isFollowing: StateFlow<Boolean> = _isFollowing

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private var currentOffset = 0
    private var lastSpeakerId = 0
    private val seenIds = mutableSetOf<Int>()

    fun load(speakerId: Int) {
        lastSpeakerId = speakerId
        currentOffset = 0
        seenIds.clear()
        _hasMore.value = true
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                // Get speaker from cache
                val cached = SpeakerCache.speakers.value.values.flatten().find { it.id == speakerId }
                _speaker.value = cached

                // Load first page directly from API (bypass ContentCache for pagination)
                val response = api.getSpeakerLectures(speakerId, limit = pageSize, offset = 0)
                val newLectures = response.lecture
                    ?.filter { it.isShort != true && it.displayActive != false && seenIds.add(it.id) }
                    ?: emptyList()
                _lectures.value = newLectures
                currentOffset = pageSize
                _hasMore.value = newLectures.size >= pageSize

                // Check follow state if logged in
                if (AuthManager.getToken() != null) {
                    try {
                        val following = api.getFollowedSpeakers()
                        _isFollowing.value = following.speakers?.values?.any { it.id == speakerId } == true
                    } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                _error.value = "Failed to load speaker"
            }
            _loading.value = false
        }
    }

    fun retry() { load(lastSpeakerId) }

    fun toggleFollow() {
        val speakerId = _speaker.value?.id ?: return
        viewModelScope.launch {
            try {
                if (_isFollowing.value) {
                    api.unfollowSpeaker(mapOf("speaker_id" to speakerId))
                    _isFollowing.value = false
                } else {
                    api.followSpeaker(mapOf("speaker_id" to speakerId))
                    _isFollowing.value = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadMore() {
        if (_loadingMore.value || !_hasMore.value) return
        val speakerId = _speaker.value?.id ?: return
        viewModelScope.launch {
            _loadingMore.value = true
            try {
                val response = api.getSpeakerLectures(speakerId, limit = pageSize, offset = currentOffset)
                val newLectures = response.lecture
                    ?.filter { it.isShort != true && it.displayActive != false && seenIds.add(it.id) }
                    ?: emptyList()

                if (newLectures.isEmpty()) {
                    _hasMore.value = false
                } else {
                    _lectures.value = _lectures.value + newLectures
                    currentOffset += pageSize
                    _hasMore.value = newLectures.size >= pageSize / 2
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            _loadingMore.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeakerDetailScreen(
    speakerId: Int,
    onBack: () -> Unit,
    onLectureClick: (Lecture) -> Unit,
    vm: SpeakerDetailViewModel = viewModel()
) {
    LaunchedEffect(speakerId) { vm.load(speakerId) }

    val speaker by vm.speaker.collectAsState()
    val lectures by vm.lectures.collectAsState()
    val loading by vm.loading.collectAsState()
    val loadingMore by vm.loadingMore.collectAsState()
    val hasMore by vm.hasMore.collectAsState()
    val isFollowing by vm.isFollowing.collectAsState()
    val error by vm.error.collectAsState()
    val isLoggedIn by AuthManager.isLoggedIn.collectAsState()
    val listState = rememberLazyListState()

    InfiniteScrollHandler(
        listState = listState,
        isLoadingMore = loadingMore,
        hasMorePages = hasMore,
        onLoadMore = { vm.loadMore() }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(speaker?.fullName ?: "Speaker", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.focusable()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TATBlue)
            }
        } else if (error != null) {
            ErrorRetryState(
                message = error ?: "Something went wrong",
                onRetry = { vm.retry() },
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                // Speaker header
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AsyncImage(
                            model = speaker?.photoUrl,
                            contentDescription = speaker?.fullName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(80.dp).clip(CircleShape)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            speaker?.fullName ?: "",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            "${lectures.size} lectures loaded",
                            color = TATTextSecondary,
                            fontSize = 13.sp
                        )
                        if (isLoggedIn) {
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = { vm.toggleFollow() },
                                colors = if (isFollowing) {
                                    ButtonDefaults.outlinedButtonColors()
                                } else {
                                    ButtonDefaults.buttonColors(containerColor = TATBlue)
                                },
                                border = if (isFollowing) {
                                    ButtonDefaults.outlinedButtonBorder(true)
                                } else null,
                                modifier = Modifier.focusable()
                            ) {
                                Text(if (isFollowing) "Following" else "Follow")
                            }
                        }
                    }
                    HorizontalDivider()
                }

                // Lectures
                items(lectures, key = { it.id }) { lecture ->
                    LectureItem(
                        lecture = lecture,
                        onClick = { onLectureClick(lecture) }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                }

                if (loadingMore) {
                    item { PaginationLoadingItem() }
                }
            }
        }
    }
}
