package com.torahanytime.audio.ui.search

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.torahanytime.audio.data.model.Speaker
import com.torahanytime.audio.data.model.Topic
import com.torahanytime.audio.data.repository.SpeakerRepository
import com.torahanytime.audio.data.repository.TopicRepository
import com.torahanytime.audio.ui.common.SpeakerItem
import com.torahanytime.audio.ui.theme.TATBlue
import com.torahanytime.audio.ui.theme.TATBrowseAllText
import com.torahanytime.audio.ui.theme.TATTextSecondary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {
    private val speakerRepo = SpeakerRepository()
    private val topicRepo = TopicRepository()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _speakers = MutableStateFlow<List<Speaker>>(emptyList())
    val speakers: StateFlow<List<Speaker>> = _speakers

    private val _topics = MutableStateFlow<List<Topic>>(emptyList())
    val topics: StateFlow<List<Topic>> = _topics

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private var searchJob: Job? = null

    fun updateQuery(q: String) {
        _query.value = q
        searchJob?.cancel()
        if (q.length < 2) {
            _speakers.value = emptyList()
            _topics.value = emptyList()
            return
        }
        searchJob = viewModelScope.launch {
            delay(300) // debounce
            _loading.value = true
            try {
                val speakerResponse = speakerRepo.getSpeakers(limit = 30)
                _speakers.value = speakerResponse.speakers.values.flatten().filter { speaker ->
                    speaker.fullName.contains(q, ignoreCase = true)
                }.take(10)
                _topics.value = topicRepo.searchTopics(q)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            _loading.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onSpeakerClick: (Int) -> Unit,
    onTopicClick: (Topic) -> Unit,
    vm: SearchViewModel = viewModel()
) {
    val query by vm.query.collectAsState()
    val speakers by vm.speakers.collectAsState()
    val topics by vm.topics.collectAsState()
    val loading by vm.loading.collectAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.focusable()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { vm.updateQuery(it) },
                        placeholder = { Text("Search classes\u2026", fontSize = 14.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 16.dp)
                            .focusRequester(focusRequester),
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium
                    )
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
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (speakers.isNotEmpty()) {
                    item {
                        Text(
                            "SPEAKERS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = TATBrowseAllText,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(speakers, key = { it.id }) { speaker ->
                        SpeakerItem(speaker = speaker, onClick = { onSpeakerClick(speaker.id) })
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    }
                }

                if (topics.isNotEmpty()) {
                    item {
                        Text(
                            "TOPICS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = TATBrowseAllText,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(topics, key = { it.id }) { topic ->
                        ListItem(
                            headlineContent = { Text(topic.text) },
                            supportingContent = {
                                topic.data?.let { Text("${it.lectureCount} lectures", color = TATTextSecondary) }
                            },
                            modifier = Modifier.focusable()
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    }
                }

                if (query.length >= 2 && speakers.isEmpty() && topics.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No results found", color = TATTextSecondary)
                        }
                    }
                }
            }
        }
    }
}
