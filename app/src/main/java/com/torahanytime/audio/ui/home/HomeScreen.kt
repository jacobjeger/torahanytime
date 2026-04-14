package com.torahanytime.audio.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.torahanytime.audio.data.model.Lecture
import com.torahanytime.audio.data.model.Speaker
import com.torahanytime.audio.data.repository.LectureRepository
import com.torahanytime.audio.data.repository.SpeakerRepository
import com.torahanytime.audio.ui.common.LectureItem
import com.torahanytime.audio.ui.theme.TATBlue
import com.torahanytime.audio.ui.theme.TATBrowseAllText
import com.torahanytime.audio.ui.theme.TATTextSecondary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val speakerRepo = SpeakerRepository()
    private val lectureRepo = LectureRepository()

    private val _featuredLectures = MutableStateFlow<List<Lecture>>(emptyList())
    val featuredLectures: StateFlow<List<Lecture>> = _featuredLectures

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    init {
        loadHome()
    }

    private fun loadHome() {
        viewModelScope.launch {
            _loading.value = true
            try {
                // Load first page of speakers and get lectures from a few popular ones
                val speakersResp = speakerRepo.getSpeakers(limit = 30, offset = 0)
                val allSpeakers = speakersResp.speakers.values.flatten()

                // Get lectures from first few speakers that have content
                val lectures = mutableListOf<Lecture>()
                for (speaker in allSpeakers.sortedByDescending { it.lectureCount }.take(5)) {
                    try {
                        val speakerLectures = lectureRepo.getSpeakerLectures(speaker.id, limit = 10)
                        lectures.addAll(speakerLectures)
                    } catch (_: Exception) {}
                    if (lectures.size >= 20) break
                }
                _featuredLectures.value = lectures
                    .sortedByDescending { it.dateCreated }
                    .take(30)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            _loading.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLectureClick: (Lecture) -> Unit,
    vm: HomeViewModel = viewModel()
) {
    val lectures by vm.featuredLectures.collectAsState()
    val loading by vm.loading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("TorahAnytime", fontWeight = FontWeight.Bold, fontSize = 20.sp)
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
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                item {
                    Text(
                        "RECENT LECTURES",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = TATBrowseAllText,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }

                if (lectures.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("Loading lectures\u2026", color = TATTextSecondary)
                        }
                    }
                }

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
