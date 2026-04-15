package com.torahanytime.audio.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.torahanytime.audio.TATApplication
import com.torahanytime.audio.data.api.ApiClient
import com.torahanytime.audio.data.api.AuthManager
import com.torahanytime.audio.data.local.entity.ListeningHistory
import com.torahanytime.audio.data.model.Lecture
import com.torahanytime.audio.data.repository.ContentCache
import com.torahanytime.audio.data.repository.SpeakerCache
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.torahanytime.audio.ui.common.CategoryTile
import com.torahanytime.audio.ui.common.ErrorRetryState
import com.torahanytime.audio.ui.common.LectureItem
import com.torahanytime.audio.ui.theme.TATBlue
import com.torahanytime.audio.ui.theme.TATBrowseAllText
import com.torahanytime.audio.ui.theme.TATOrange
import com.torahanytime.audio.ui.theme.TATTextSecondary
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val api = ApiClient.api
    private val historyDao = TATApplication.db.historyDao()

    private val _newToday = MutableStateFlow<List<Lecture>>(emptyList())
    val newToday: StateFlow<List<Lecture>> = _newToday

    private val _dafYomi = MutableStateFlow<List<Lecture>>(emptyList())
    val dafYomi: StateFlow<List<Lecture>> = _dafYomi

    private val _continueListening = MutableStateFlow<List<ListeningHistory>>(emptyList())
    val continueListening: StateFlow<List<ListeningHistory>> = _continueListening

    private val _trending = MutableStateFlow<List<Lecture>>(emptyList())
    val trending: StateFlow<List<Lecture>> = _trending

    private val _followedSpeakerLectures = MutableStateFlow<List<Lecture>>(emptyList())
    val followedSpeakerLectures: StateFlow<List<Lecture>> = _followedSpeakerLectures

    init {
        viewModelScope.launch { SpeakerCache.ensureLoaded() }
        viewModelScope.launch { ContentCache.ensureRecentLoaded() }
        viewModelScope.launch { loadDiscovery() }
        // Observe continue listening
        viewModelScope.launch {
            historyDao.getRecent(10).collect { history ->
                _continueListening.value = history.filter { entry ->
                    entry.position > 0 &&
                    entry.duration > 0 &&
                    entry.position < entry.duration * 1000L * 0.95
                }.take(5)
            }
        }
    }

    suspend fun loadDiscovery() {
        viewModelScope.launch {
            val deferredDaf = async { loadDafYomi() }
            val deferredNew = async { loadNewToday() }
            val deferredTrending = async { loadTrending() }
            val deferredFollowed = async { loadFollowedContent() }
            deferredDaf.await()
            deferredNew.await()
            deferredTrending.await()
            deferredFollowed.await()
        }
    }

    private suspend fun loadDafYomi() {
        try {
            val response = api.searchLectures(filter = "Daf Yomi", limit = 5)
            _dafYomi.value = response.items?.mapNotNull { it.data?.toLecture(it.imgUrl) } ?: emptyList()
        } catch (_: Exception) {}
    }

    private suspend fun loadNewToday() {
        try {
            val recent = ContentCache.recentLectures.value
            if (recent.isNotEmpty()) {
                _newToday.value = recent
                    .sortedByDescending { it.dateCreated ?: it.dateRecorded }
                    .take(10)
            }
        } catch (_: Exception) {}
    }

    private suspend fun loadTrending() {
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            val now = Calendar.getInstance()
            val endTime = dateFormat.format(now.time)
            now.add(Calendar.DAY_OF_YEAR, -7)
            val startTime = dateFormat.format(now.time)

            val pins = api.getPinnedContent(
                endTimeGte = endTime,
                startTimeLte = startTime,
                limit = 10
            )
            val lectures = pins.flatMap { it.lectures ?: emptyList() }
                .distinctBy { it.id }
                .take(5)
            if (lectures.isNotEmpty()) {
                _trending.value = lectures
            }
        } catch (_: Exception) {
            // Fallback: use most recent lectures as "trending"
        }
    }

    private suspend fun loadFollowedContent() {
        if (AuthManager.getToken() == null) return
        try {
            val following = api.getFollowedSpeakers()
            val speakerIds = following.speakers?.values?.mapNotNull { it.id }?.take(10) ?: return
            if (speakerIds.isEmpty()) return

            coroutineScope {
                val allLectures = speakerIds.map { id ->
                    async {
                        try {
                            api.getSpeakerLectures(id, limit = 3, offset = 0)
                                .lecture
                                ?.filter { it.isShort != true && it.displayActive != false }
                                ?: emptyList()
                        } catch (_: Exception) { emptyList<Lecture>() }
                    }
                }.map { it.await() }.flatten()
                    .distinctBy { it.id }
                    .sortedByDescending { it.dateCreated ?: it.dateRecorded }
                    .take(10)

                _followedSpeakerLectures.value = allLectures
            }
        } catch (_: Exception) {}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLectureClick: (Lecture) -> Unit,
    onSpeakerClick: ((Int) -> Unit)? = null,
    onNavigateToSpeakers: (() -> Unit)? = null,
    onNavigateToTopics: (() -> Unit)? = null,
    onNavigateToSeries: (() -> Unit)? = null,
    onNavigateToSearch: (() -> Unit)? = null,
    onNavigateToParasha: (() -> Unit)? = null,
    onAddToQueue: ((Lecture) -> Unit)? = null,
    vm: HomeViewModel = viewModel()
) {
    val lectures by ContentCache.recentLectures.collectAsState()
    val loading by ContentCache.recentLoading.collectAsState()
    val contentError by ContentCache.error.collectAsState()
    val scope = rememberCoroutineScope()
    val newToday by vm.newToday.collectAsState()
    val dafYomi by vm.dafYomi.collectAsState()
    val continueListening by vm.continueListening.collectAsState()
    val trending by vm.trending.collectAsState()
    val followedLectures by vm.followedSpeakerLectures.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TorahAnytime", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        if (loading && lectures.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = TATBlue)
                    Spacer(Modifier.height(12.dp))
                    Text("Loading shiurim\u2026", color = TATTextSecondary, fontSize = 13.sp)
                }
            }
        } else if (contentError != null && lectures.isEmpty()) {
            ErrorRetryState(
                message = contentError ?: "Something went wrong",
                onRetry = {
                    ContentCache.retryRecent()
                    scope.launch { ContentCache.ensureRecentLoaded() }
                },
                modifier = Modifier.padding(padding)
            )
        } else {
            val listState = rememberLazyListState()
            val coroutineScope = rememberCoroutineScope()
            var isRefreshing by remember { mutableStateOf(false) }

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    ContentCache.retryRecent()
                    coroutineScope.launch {
                        ContentCache.ensureRecentLoaded()
                        vm.loadDiscovery()
                        isRefreshing = false
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // Category tiles grid
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CategoryTile(
                                label = "Recent\nLectures",
                                icon = Icons.Filled.AccessTime,
                                iconTint = TATBlue,
                                onClick = { coroutineScope.launch { listState.animateScrollToItem(4) } },
                                modifier = Modifier.weight(1f)
                            )
                            CategoryTile(
                                label = "Speakers",
                                icon = Icons.Filled.Person,
                                iconTint = TATOrange,
                                onClick = { onNavigateToSpeakers?.invoke() },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CategoryTile(
                                label = "Topics",
                                icon = Icons.Outlined.BookmarkBorder,
                                iconTint = Color(0xFF2E7D32),
                                onClick = { onNavigateToTopics?.invoke() },
                                modifier = Modifier.weight(1f)
                            )
                            CategoryTile(
                                label = "Series",
                                icon = Icons.Outlined.List,
                                iconTint = Color(0xFF7B1FA2),
                                onClick = { onNavigateToSeries?.invoke() },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CategoryTile(
                                label = "Search",
                                icon = Icons.Filled.Search,
                                iconTint = TATBlue,
                                onClick = { onNavigateToSearch?.invoke() },
                                modifier = Modifier.weight(1f)
                            )
                            CategoryTile(
                                label = "Parashat\nHashavua",
                                icon = Icons.Filled.MenuBook,
                                iconTint = TATOrange,
                                onClick = { onNavigateToParasha?.invoke() },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // FROM SPEAKERS YOU FOLLOW section (only when logged in)
                if (followedLectures.isNotEmpty()) {
                    item {
                        SectionHeader("FROM SPEAKERS YOU FOLLOW")
                    }
                    items(followedLectures, key = { "followed_${it.id}" }) { lecture ->
                        LectureItem(
                            lecture = lecture,
                            onClick = { onLectureClick(lecture) },
                            onAddToQueue = onAddToQueue?.let { { it(lecture) } }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    }
                }

                // CONTINUE LISTENING section
                if (continueListening.isNotEmpty()) {
                    item {
                        SectionHeader("CONTINUE LISTENING")
                    }
                    items(continueListening, key = { "continue_${it.lectureId}" }) { history ->
                        val lecture = Lecture(
                            id = history.lectureId,
                            title = history.title,
                            speakerNameFirst = history.speakerName.split(" ").firstOrNull(),
                            speakerNameLast = history.speakerName.split(" ").drop(1).joinToString(" "),
                            mp3Url = history.mp3Url,
                            thumbnailUrl = history.thumbnailUrl,
                            duration = history.duration,
                            languageName = history.languageName
                        )
                        Column {
                            LectureItem(
                                lecture = lecture,
                                onClick = { onLectureClick(lecture) },
                                onAddToQueue = onAddToQueue?.let { { it(lecture) } }
                            )
                            // Progress bar showing how far they got
                            val progressFraction = if (history.duration > 0) {
                                (history.position.toFloat() / (history.duration * 1000f)).coerceIn(0f, 1f)
                            } else 0f
                            LinearProgressIndicator(
                                progress = { progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .height(3.dp),
                                color = TATBlue,
                                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    }
                }

                // TRENDING section
                if (trending.isNotEmpty()) {
                    item {
                        SectionHeader("TRENDING")
                    }
                    items(trending, key = { "trend_${it.id}" }) { lecture ->
                        LectureItem(
                            lecture = lecture,
                            onClick = { onLectureClick(lecture) },
                            onAddToQueue = onAddToQueue?.let { { it(lecture) } }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    }
                }

                // NEW TODAY section
                if (newToday.isNotEmpty()) {
                    item {
                        SectionHeader("NEW TODAY")
                    }
                    items(newToday.take(5), key = { "new_${it.id}" }) { lecture ->
                        LectureItem(
                        lecture = lecture,
                        onClick = { onLectureClick(lecture) },
                        onAddToQueue = onAddToQueue?.let { { it(lecture) } }
                    )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    }
                }

                // DAF YOMI section
                if (dafYomi.isNotEmpty()) {
                    item {
                        SectionHeader("DAF YOMI")
                    }
                    items(dafYomi, key = { "daf_${it.id}" }) { lecture ->
                        LectureItem(
                        lecture = lecture,
                        onClick = { onLectureClick(lecture) },
                        onAddToQueue = onAddToQueue?.let { { it(lecture) } }
                    )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    }
                }

                // Recent Lectures header
                item {
                    SectionHeader("RECENT LECTURES")
                }

                items(lectures, key = { it.id }) { lecture ->
                    LectureItem(
                        lecture = lecture,
                        onClick = { onLectureClick(lecture) },
                        onAddToQueue = onAddToQueue?.let { { it(lecture) } }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                }
            }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelSmall.copy(
            letterSpacing = 1.sp,
            fontWeight = FontWeight.SemiBold
        ),
        color = TATBrowseAllText,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
    )
}
