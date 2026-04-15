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
import com.torahanytime.audio.data.model.Series
import com.torahanytime.audio.ui.common.ErrorRetryState
import com.torahanytime.audio.ui.common.InfiniteScrollHandler
import com.torahanytime.audio.ui.common.LectureItem
import com.torahanytime.audio.ui.common.PaginationLoadingItem
import com.torahanytime.audio.ui.theme.TATBlue
import com.torahanytime.audio.ui.theme.TATTextSecondary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SeriesDetailViewModel : ViewModel() {
    private val api = ApiClient.api
    private val pageSize = 50

    private val _series = MutableStateFlow<Series?>(null)
    val series: StateFlow<Series?> = _series

    private val _lectures = MutableStateFlow<List<Lecture>>(emptyList())
    val lectures: StateFlow<List<Lecture>> = _lectures

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    private val _loadingMore = MutableStateFlow(false)
    val loadingMore: StateFlow<Boolean> = _loadingMore

    private val _hasMore = MutableStateFlow(true)
    val hasMore: StateFlow<Boolean> = _hasMore

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private var currentOffset = 0
    private var lastSeriesId = 0
    private val seenIds = mutableSetOf<Int>()

    fun load(seriesId: Int) {
        lastSeriesId = seriesId
        currentOffset = 0
        seenIds.clear()
        _hasMore.value = true
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                // Load series detail
                val seriesDetail = try { api.getSeriesDetail(seriesId) } catch (_: Exception) { null }
                _series.value = seriesDetail

                // Load first page of lectures
                val response = api.getSeriesLectures(seriesId, limit = pageSize, offset = 0)
                val newLectures = response.seriesLectures?.values?.toList()
                    ?.filter { it.displayActive != false && seenIds.add(it.id) }
                    ?.sortedByDescending { it.dateCreated ?: it.dateRecorded }
                    ?: emptyList()
                _lectures.value = newLectures
                currentOffset = pageSize
                _hasMore.value = newLectures.size >= pageSize
            } catch (e: Exception) {
                _error.value = "Failed to load series"
            }
            _loading.value = false
        }
    }

    fun retry() { load(lastSeriesId) }

    fun loadMore() {
        if (_loadingMore.value || !_hasMore.value) return
        val seriesId = _series.value?.id ?: return
        viewModelScope.launch {
            _loadingMore.value = true
            try {
                val response = api.getSeriesLectures(seriesId, limit = pageSize, offset = currentOffset)
                val newLectures = response.seriesLectures?.values?.toList()
                    ?.filter { it.displayActive != false && seenIds.add(it.id) }
                    ?.sortedByDescending { it.dateCreated ?: it.dateRecorded }
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
fun SeriesDetailScreen(
    seriesId: Int,
    onBack: () -> Unit,
    onLectureClick: (Lecture) -> Unit,
    vm: SeriesDetailViewModel = viewModel()
) {
    LaunchedEffect(seriesId) { vm.load(seriesId) }

    val series by vm.series.collectAsState()
    val lectures by vm.lectures.collectAsState()
    val loading by vm.loading.collectAsState()
    val loadingMore by vm.loadingMore.collectAsState()
    val hasMore by vm.hasMore.collectAsState()
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
                        Text(
                            series?.title ?: "Series",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1
                        )
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        when {
            loading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = TATBlue)
            }
            error != null -> ErrorRetryState(
                message = error ?: "Something went wrong",
                onRetry = { vm.retry() },
                modifier = Modifier.padding(padding)
            )
            lectures.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No lectures in this series", color = TATTextSecondary)
            }
            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
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
