package com.torahanytime.audio.ui.browse

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.torahanytime.audio.data.api.ApiClient
import com.torahanytime.audio.data.model.Lecture
import com.torahanytime.audio.data.model.Series
import com.torahanytime.audio.ui.common.LectureItem
import com.torahanytime.audio.ui.theme.TATBlue
import com.torahanytime.audio.ui.theme.TATTextSecondary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SeriesDetailViewModel : ViewModel() {
    private val api = ApiClient.api

    private val _series = MutableStateFlow<Series?>(null)
    val series: StateFlow<Series?> = _series

    private val _lectures = MutableStateFlow<List<Lecture>>(emptyList())
    val lectures: StateFlow<List<Lecture>> = _lectures

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    fun load(seriesId: Int) {
        viewModelScope.launch {
            _loading.value = true
            try {
                // Load series detail and lectures in parallel
                val seriesDetail = try { api.getSeriesDetail(seriesId) } catch (_: Exception) { null }
                _series.value = seriesDetail

                val response = api.getSeriesLectures(seriesId, limit = 100)
                _lectures.value = response.seriesLectures?.values?.toList()
                    ?.filter { it.displayActive != false }
                    ?.sortedByDescending { it.dateCreated ?: it.dateRecorded }
                    ?: emptyList()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            _loading.value = false
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
                                "${lectures.size} lectures",
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
            lectures.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No lectures in this series", color = TATTextSecondary)
            }
            else -> LazyColumn(
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
            }
        }
    }
}
