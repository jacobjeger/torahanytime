package com.torahanytime.audio.ui.browse

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.torahanytime.audio.data.api.ApiClient
import com.torahanytime.audio.data.model.Lecture
import com.torahanytime.audio.ui.common.ErrorRetryState
import com.torahanytime.audio.ui.common.InfiniteScrollHandler
import com.torahanytime.audio.ui.common.LectureItem
import com.torahanytime.audio.ui.common.PaginationLoadingItem
import com.torahanytime.audio.ui.theme.TATBlue
import com.torahanytime.audio.ui.theme.TATTextSecondary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TopicLecturesViewModel : ViewModel() {
    private val api = ApiClient.api
    private val pageSize = 30

    private val _lectures = MutableStateFlow<List<Lecture>>(emptyList())
    val lectures: StateFlow<List<Lecture>> = _lectures

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    private val _loadingMore = MutableStateFlow(false)
    val loadingMore: StateFlow<Boolean> = _loadingMore

    private val _hasMore = MutableStateFlow(true)
    val hasMore: StateFlow<Boolean> = _hasMore

    private val _topicName = MutableStateFlow("")
    val topicName: StateFlow<String> = _topicName

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private var currentOffset = 0
    private val seenIds = mutableSetOf<Int>()

    fun load(topicName: String) {
        _topicName.value = topicName
        currentOffset = 0
        seenIds.clear()
        _hasMore.value = true
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val response = api.searchLectures(filter = topicName, limit = pageSize, start = 0)
                val newLectures = response.items?.mapNotNull { item ->
                    item.data?.toLecture(item.imgUrl)
                }?.filter { it.isShort != true && seenIds.add(it.id) } ?: emptyList()
                _lectures.value = newLectures
                currentOffset = pageSize
                _hasMore.value = newLectures.size >= pageSize
            } catch (e: Exception) {
                _error.value = "Failed to load lectures"
            }
            _loading.value = false
        }
    }

    fun retry() { load(_topicName.value) }

    fun loadMore() {
        if (_loadingMore.value || !_hasMore.value) return
        viewModelScope.launch {
            _loadingMore.value = true
            try {
                val response = api.searchLectures(
                    filter = _topicName.value,
                    limit = pageSize,
                    start = currentOffset
                )
                val newLectures = response.items?.mapNotNull { item ->
                    item.data?.toLecture(item.imgUrl)
                }?.filter { it.isShort != true && seenIds.add(it.id) } ?: emptyList()

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
fun TopicLecturesScreen(
    topicName: String,
    onBack: () -> Unit,
    onLectureClick: (Lecture) -> Unit,
    vm: TopicLecturesViewModel = viewModel()
) {
    LaunchedEffect(topicName) { vm.load(topicName) }

    val lectures by vm.lectures.collectAsState()
    val loading by vm.loading.collectAsState()
    val loadingMore by vm.loadingMore.collectAsState()
    val hasMore by vm.hasMore.collectAsState()
    val name by vm.topicName.collectAsState()
    val error by vm.error.collectAsState()
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
                title = {
                    Column {
                        Text(name, fontWeight = FontWeight.Bold, maxLines = 1)
                        if (lectures.isNotEmpty()) {
                            Text(
                                "${lectures.size} lectures loaded",
                                fontSize = 11.sp,
                                color = TATTextSecondary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.focusable()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        when {
            loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TATBlue)
            }
            error != null -> ErrorRetryState(
                message = error ?: "Something went wrong",
                onRetry = { vm.retry() },
                modifier = Modifier.padding(padding)
            )
            lectures.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No lectures found for this topic", color = TATTextSecondary)
            }
            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                items(lectures, key = { it.id }) { lecture ->
                    LectureItem(lecture = lecture, onClick = { onLectureClick(lecture) })
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
