package com.torahanytime.audio.ui.browse

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.torahanytime.audio.data.model.Topic
import com.torahanytime.audio.data.model.TopicData
import com.torahanytime.audio.data.repository.TopicRepository
import com.torahanytime.audio.ui.theme.TATBlue
import com.torahanytime.audio.ui.theme.TATBrowseAllText
import com.torahanytime.audio.ui.theme.TATTextSecondary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Pre-defined main categories (parent=6 is the root)
private val mainTopics = listOf(
    Topic(id = "topics_23", text = "Parasha/Torah Portion", data = TopicData(id = 23, name = "Parasha/Torah Portion", lectureCount = 57186)),
    Topic(id = "topics_126", text = "Halacha/Jewish Law", data = TopicData(id = 126, name = "Halacha/Jewish Law", lectureCount = 21941)),
    Topic(id = "topics_8", text = "Holidays", data = TopicData(id = 8, name = "Holidays", lectureCount = 29959)),
    Topic(id = "topics_104", text = "Mussar/Self Improvement", data = TopicData(id = 104, name = "Mussar/Self Improvement", lectureCount = 7915)),
    Topic(id = "topics_107", text = "Tefillah/Prayer", data = TopicData(id = 107, name = "Tefillah/Prayer", lectureCount = 4902)),
    Topic(id = "topics_148", text = "Emuna/Faith", data = TopicData(id = 148, name = "Emuna", lectureCount = 4259)),
    Topic(id = "topics_7", text = "Shabbat", data = TopicData(id = 7, name = "Shabbat", lectureCount = 1705)),
    Topic(id = "topics_201", text = "Halachic Principles/Lomdut", data = TopicData(id = 201, name = "Halachic Principles/Lomdut", lectureCount = 2380)),
    Topic(id = "topics_108", text = "Tehillim/Psalms", data = TopicData(id = 108, name = "Tehillim/Psalms", lectureCount = 955)),
    Topic(id = "topics_162", text = "Eretz Yisrael", data = TopicData(id = 162, name = "Eretz Yisrael", lectureCount = 213)),
    Topic(id = "topics_278", text = "Chessed", data = TopicData(id = 278, name = "Chessed", lectureCount = 329)),
    Topic(id = "topics_286", text = "TorahAnytime Testimonials", data = TopicData(id = 286, name = "TorahAnytime Testimonials", lectureCount = 103))
)

class TopicListViewModel : ViewModel() {
    private val repo = TopicRepository()

    private val _topics = MutableStateFlow<List<Topic>>(mainTopics)
    val topics: StateFlow<List<Topic>> = _topics

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    private var searchJob: Job? = null

    fun search(q: String) {
        _query.value = q
        searchJob?.cancel()
        if (q.isBlank()) {
            _topics.value = mainTopics
            _isSearching.value = false
            return
        }
        _isSearching.value = true
        searchJob = viewModelScope.launch {
            delay(300)
            _loading.value = true
            try {
                val results = repo.searchTopics(q)
                _topics.value = if (results.isNotEmpty()) results else mainTopics.filter {
                    it.text.contains(q, ignoreCase = true)
                }
            } catch (e: Exception) {
                _topics.value = mainTopics.filter { it.text.contains(q, ignoreCase = true) }
            }
            _loading.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicListScreen(
    onBack: () -> Unit,
    onTopicClick: (Topic) -> Unit,
    vm: TopicListViewModel = viewModel()
) {
    val topics by vm.topics.collectAsState()
    val loading by vm.loading.collectAsState()
    val query by vm.query.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Topics", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.focusable()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { vm.search(it) },
                placeholder = { Text("Search topics\u2026") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .focusable(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            if (loading) {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TATBlue, modifier = Modifier.size(24.dp))
                }
            }

            LazyColumn {
                items(topics, key = { it.id }) { topic ->
                    TopicRow(topic = topic, onClick = { onTopicClick(topic) })
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TopicRow(topic: Topic, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = topic.text,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = if (isFocused) TATBlue else MaterialTheme.colorScheme.onSurface
            )
            topic.data?.let {
                Text(
                    "${it.lectureCount} lectures",
                    fontSize = 12.sp,
                    color = TATTextSecondary
                )
            }
        }
    }
}
