package com.torahanytime.audio.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.torahanytime.audio.TATApplication
import com.torahanytime.audio.data.api.ApiClient
import com.torahanytime.audio.data.local.entity.SearchHistoryEntry
import com.torahanytime.audio.data.model.Lecture
import com.torahanytime.audio.data.model.Speaker
import com.torahanytime.audio.data.model.Topic
import com.torahanytime.audio.data.repository.SpeakerCache
import com.torahanytime.audio.data.repository.TopicRepository
import androidx.compose.foundation.lazy.LazyRow
import com.torahanytime.audio.ui.common.ErrorRetryState
import com.torahanytime.audio.ui.common.InfiniteScrollHandler
import com.torahanytime.audio.ui.common.LectureItem
import com.torahanytime.audio.ui.common.PaginationLoadingItem
import com.torahanytime.audio.ui.common.SpeakerItem
import com.torahanytime.audio.ui.theme.TATBlue
import com.torahanytime.audio.ui.theme.TATBrowseAllText
import com.torahanytime.audio.ui.theme.TATTextSecondary
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {
    private val topicRepo = TopicRepository()
    private val api = ApiClient.api
    private val searchHistoryDao = TATApplication.db.searchHistoryDao()
    private val lecturePageSize = 20

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _speakers = MutableStateFlow<List<Speaker>>(emptyList())
    val speakers: StateFlow<List<Speaker>> = _speakers

    private val _topics = MutableStateFlow<List<Topic>>(emptyList())
    val topics: StateFlow<List<Topic>> = _topics

    private val _lectures = MutableStateFlow<List<Lecture>>(emptyList())
    val lectures: StateFlow<List<Lecture>> = _lectures

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _loadingMoreLectures = MutableStateFlow(false)
    val loadingMoreLectures: StateFlow<Boolean> = _loadingMoreLectures

    private val _hasMoreLectures = MutableStateFlow(false)
    val hasMoreLectures: StateFlow<Boolean> = _hasMoreLectures

    private val _languageFilter = MutableStateFlow("All")
    val languageFilter: StateFlow<String> = _languageFilter

    private val _durationFilter = MutableStateFlow("Any")
    val durationFilter: StateFlow<String> = _durationFilter

    // Unfiltered lectures for local filtering
    private val _allLectures = mutableListOf<Lecture>()

    val searchHistory = searchHistoryDao.getRecent()

    private var searchJob: Job? = null
    private var lectureOffset = 0
    private val seenLectureIds = mutableSetOf<Int>()

    fun updateQuery(q: String) {
        _query.value = q
        searchJob?.cancel()
        if (q.length < 2) {
            _speakers.value = emptyList()
            _topics.value = emptyList()
            _lectures.value = emptyList()
            _hasMoreLectures.value = false
            return
        }
        searchJob = viewModelScope.launch {
            delay(300)
            _loading.value = true
            _error.value = null
            lectureOffset = 0
            seenLectureIds.clear()
            try {
                val deferredSpeakers = async {
                    try {
                        SpeakerCache.searchSpeakers(q).take(10)
                    } catch (_: Exception) { emptyList() }
                }
                val deferredTopics = async {
                    try { topicRepo.searchTopics(q) } catch (_: Exception) { emptyList() }
                }
                val deferredLectures = async {
                    try {
                        api.searchLectures(filter = q, limit = lecturePageSize, start = 0)
                            .items?.mapNotNull { it.data?.toLecture(it.imgUrl) }
                            ?.filter { seenLectureIds.add(it.id) }
                            ?: emptyList()
                    } catch (_: Exception) { emptyList() }
                }
                _speakers.value = deferredSpeakers.await()
                _topics.value = deferredTopics.await()
                val newLectures = deferredLectures.await()
                _allLectures.clear()
                _allLectures.addAll(newLectures)
                applyFilters()
                lectureOffset = lecturePageSize
                _hasMoreLectures.value = newLectures.size >= lecturePageSize

                // Only save to history after results are fetched and query is meaningful
                if (q.length >= 3) {
                    searchHistoryDao.upsert(SearchHistoryEntry(query = q))
                }
            } catch (e: Exception) {
                _error.value = "Search failed. Check your connection."
            }
            _loading.value = false
        }
    }

    fun loadMoreLectures() {
        if (_loadingMoreLectures.value || !_hasMoreLectures.value) return
        val q = _query.value
        if (q.length < 2) return
        viewModelScope.launch {
            _loadingMoreLectures.value = true
            try {
                val newLectures = api.searchLectures(filter = q, limit = lecturePageSize, start = lectureOffset)
                    .items?.mapNotNull { it.data?.toLecture(it.imgUrl) }
                    ?.filter { seenLectureIds.add(it.id) }
                    ?: emptyList()

                if (newLectures.isEmpty()) {
                    _hasMoreLectures.value = false
                } else {
                    _allLectures.addAll(newLectures)
                    applyFilters()
                    lectureOffset += lecturePageSize
                    _hasMoreLectures.value = newLectures.size >= lecturePageSize / 2
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            _loadingMoreLectures.value = false
        }
    }

    fun deleteSearchHistory(query: String) {
        viewModelScope.launch { searchHistoryDao.delete(query) }
    }

    fun clearSearchHistory() {
        viewModelScope.launch { searchHistoryDao.deleteAll() }
    }

    fun setLanguageFilter(language: String) {
        _languageFilter.value = language
        applyFilters()
    }

    fun setDurationFilter(duration: String) {
        _durationFilter.value = duration
        applyFilters()
    }

    private fun applyFilters() {
        var filtered = _allLectures.toList()
        val lang = _languageFilter.value
        if (lang != "All") {
            filtered = filtered.filter { it.languageName.equals(lang, ignoreCase = true) }
        }
        val dur = _durationFilter.value
        filtered = when (dur) {
            "Under 15 min" -> filtered.filter { (it.duration ?: 0) < 900 }
            "15-30 min" -> filtered.filter { val d = it.duration ?: 0; d in 900..1800 }
            "30-60 min" -> filtered.filter { val d = it.duration ?: 0; d in 1800..3600 }
            "Over 60 min" -> filtered.filter { (it.duration ?: 0) > 3600 }
            else -> filtered
        }
        _lectures.value = filtered
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onSpeakerClick: (Int) -> Unit,
    onTopicClick: (Topic) -> Unit,
    onLectureClick: ((Lecture) -> Unit)? = null,
    vm: SearchViewModel = viewModel()
) {
    val query by vm.query.collectAsState()
    val speakers by vm.speakers.collectAsState()
    val topics by vm.topics.collectAsState()
    val lectures by vm.lectures.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    val languageFilter by vm.languageFilter.collectAsState()
    val durationFilter by vm.durationFilter.collectAsState()
    val loadingMoreLectures by vm.loadingMoreLectures.collectAsState()
    var showLanguagePicker by remember { mutableStateOf(false) }
    var showDurationPicker by remember { mutableStateOf(false) }
    val languageOptions = listOf("All", "English", "Hebrew", "Yiddish")
    val durationOptions = listOf("Any", "Under 15 min", "15-30 min", "30-60 min", "Over 60 min")
    val hasMoreLectures by vm.hasMoreLectures.collectAsState()
    val searchHistory by vm.searchHistory.collectAsState(initial = emptyList())
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    InfiniteScrollHandler(
        listState = listState,
        isLoadingMore = loadingMoreLectures,
        hasMorePages = hasMoreLectures,
        onLoadMore = { vm.loadMoreLectures() }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { vm.updateQuery(it) },
                    placeholder = { Text("Search your next Torah class\u2026", fontSize = 14.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .focusRequester(focusRequester),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )
            }

            // Filter chips
            if (query.length >= 2) {
                item {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = languageFilter != "All",
                                onClick = { showLanguagePicker = true },
                                label = { Text(if (languageFilter == "All") "Language" else languageFilter, fontSize = 12.sp) },
                                modifier = Modifier.focusable()
                            )
                        }
                        item {
                            FilterChip(
                                selected = durationFilter != "Any",
                                onClick = { showDurationPicker = true },
                                label = { Text(if (durationFilter == "Any") "Duration" else durationFilter, fontSize = 12.sp) },
                                modifier = Modifier.focusable()
                            )
                        }
                    }
                }
                item {
                    Text(
                        "* Language  # Duration",
                        fontSize = 10.sp,
                        color = TATTextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // Show search history when no active search
            if (query.length < 2 && searchHistory.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionHeader("RECENT SEARCHES")
                        TextButton(onClick = { vm.clearSearchHistory() }) {
                            Text("Clear", color = TATBlue, fontSize = 12.sp)
                        }
                    }
                }
                items(searchHistory, key = { "hist_${it.query}" }) { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { vm.updateQuery(entry.query) }
                            .focusable()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            tint = TATTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            entry.query,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        IconButton(
                            onClick = { vm.deleteSearchHistory(entry.query) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove",
                                tint = TATTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                }
            }

            if (loading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = TATBlue, modifier = Modifier.size(24.dp))
                    }
                }
            }

            // Speakers section
            if (speakers.isNotEmpty()) {
                item {
                    SectionHeader("SPEAKERS")
                }
                items(speakers, key = { "speaker_${it.id}" }) { speaker ->
                    SpeakerItem(speaker = speaker, onClick = { onSpeakerClick(speaker.id) })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                }
            }

            // Topics section
            if (topics.isNotEmpty()) {
                item {
                    SectionHeader("TOPICS")
                }
                items(topics, key = { "topic_${it.id}" }) { topic ->
                    ListItem(
                        headlineContent = { Text(topic.text) },
                        supportingContent = {
                            topic.data?.let { Text("${it.lectureCount} lectures", color = TATTextSecondary) }
                        },
                        modifier = Modifier
                            .focusable()
                            .clickable { onTopicClick(topic) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                }
            }

            // Lectures section
            if (lectures.isNotEmpty()) {
                item {
                    SectionHeader("LECTURES (${lectures.size})")
                }
                items(lectures, key = { "lecture_${it.id}" }) { lecture ->
                    LectureItem(lecture = lecture, onClick = { onLectureClick?.invoke(lecture) })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                }
                if (loadingMoreLectures) {
                    item { PaginationLoadingItem() }
                }
            }

            // Error state
            if (error != null) {
                item {
                    ErrorRetryState(
                        message = error ?: "Something went wrong",
                        onRetry = { vm.updateQuery(query) },
                        modifier = Modifier.height(200.dp)
                    )
                }
            }

            // No results
            if (query.length >= 2 && !loading && error == null && speakers.isEmpty() && topics.isEmpty() && lectures.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No results found", color = TATTextSecondary)
                    }
                }
            }
        }

        if (showLanguagePicker) {
            AlertDialog(
                onDismissRequest = { showLanguagePicker = false },
                title = { Text("Filter by Language") },
                text = {
                    Column {
                        languageOptions.forEach { lang ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusable()
                                    .clickable {
                                        vm.setLanguageFilter(lang)
                                        showLanguagePicker = false
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = lang == languageFilter,
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(selectedColor = TATBlue)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(lang)
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = { TextButton(onClick = { showLanguagePicker = false }) { Text("Cancel") } }
            )
        }

        if (showDurationPicker) {
            AlertDialog(
                onDismissRequest = { showDurationPicker = false },
                title = { Text("Filter by Duration") },
                text = {
                    Column {
                        durationOptions.forEach { dur ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusable()
                                    .clickable {
                                        vm.setDurationFilter(dur)
                                        showDurationPicker = false
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = dur == durationFilter,
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(selectedColor = TATBlue)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(dur)
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = { TextButton(onClick = { showDurationPicker = false }) { Text("Cancel") } }
            )
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
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}
