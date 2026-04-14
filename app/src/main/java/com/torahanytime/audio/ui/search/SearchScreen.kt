package com.torahanytime.audio.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.torahanytime.audio.ui.common.LectureItem
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

    val searchHistory = searchHistoryDao.getRecent()

    private var searchJob: Job? = null

    fun updateQuery(q: String) {
        _query.value = q
        searchJob?.cancel()
        if (q.length < 2) {
            _speakers.value = emptyList()
            _topics.value = emptyList()
            _lectures.value = emptyList()
            return
        }
        searchJob = viewModelScope.launch {
            delay(300)
            _loading.value = true

            // Save to search history
            searchHistoryDao.upsert(SearchHistoryEntry(query = q))

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
                        api.searchLectures(filter = q, limit = 15)
                            .items?.mapNotNull { it.data?.toLecture(it.imgUrl) }
                            ?: emptyList()
                    } catch (_: Exception) { emptyList() }
                }
                _speakers.value = deferredSpeakers.await()
                _topics.value = deferredTopics.await()
                _lectures.value = deferredLectures.await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            _loading.value = false
        }
    }

    fun deleteSearchHistory(query: String) {
        viewModelScope.launch { searchHistoryDao.delete(query) }
    }

    fun clearSearchHistory() {
        viewModelScope.launch { searchHistoryDao.deleteAll() }
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
    val searchHistory by vm.searchHistory.collectAsState(initial = emptyList())
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
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
                    SectionHeader("LECTURES")
                }
                items(lectures, key = { "lecture_${it.id}" }) { lecture ->
                    LectureItem(lecture = lecture, onClick = { onLectureClick?.invoke(lecture) })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                }
            }

            // No results
            if (query.length >= 2 && !loading && speakers.isEmpty() && topics.isEmpty() && lectures.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No results found", color = TATTextSecondary)
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
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}
