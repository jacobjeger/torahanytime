package com.torahanytime.audio.ui.browse

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.torahanytime.audio.data.model.Lecture
import com.torahanytime.audio.data.model.Speaker
import com.torahanytime.audio.data.repository.ContentCache
import com.torahanytime.audio.data.repository.SpeakerCache
import com.torahanytime.audio.data.repository.SpeakerRepository
import com.torahanytime.audio.ui.common.LectureItem
import com.torahanytime.audio.ui.theme.TATBlue
import com.torahanytime.audio.ui.theme.TATTextSecondary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SpeakerDetailViewModel : ViewModel() {
    private val _speaker = MutableStateFlow<Speaker?>(null)
    val speaker: StateFlow<Speaker?> = _speaker

    private val _lectures = MutableStateFlow<List<Lecture>>(emptyList())
    val lectures: StateFlow<List<Lecture>> = _lectures

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    fun load(speakerId: Int) {
        viewModelScope.launch {
            _loading.value = true
            try {
                // Try to get speaker from cache first
                val cached = SpeakerCache.speakers.value.values.flatten().find { it.id == speakerId }
                _speaker.value = cached
                // Load lectures from cache
                _lectures.value = ContentCache.getSpeakerLectures(speakerId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            _loading.value = false
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
        } else {
            LazyColumn(
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
                            "${lectures.size} lectures",
                            color = TATTextSecondary,
                            fontSize = 13.sp
                        )
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
            }
        }
    }
}
