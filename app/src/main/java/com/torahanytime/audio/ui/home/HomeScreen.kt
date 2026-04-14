package com.torahanytime.audio.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.torahanytime.audio.data.api.ApiClient
import com.torahanytime.audio.data.model.Lecture
import com.torahanytime.audio.data.model.Speaker
import com.torahanytime.audio.ui.common.LectureItem
import com.torahanytime.audio.ui.theme.TATBlue
import com.torahanytime.audio.ui.theme.TATBrowseAllText
import com.torahanytime.audio.ui.theme.TATTextSecondary
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class HomeSection(
    val title: String,
    val lectures: List<Lecture>
)

class HomeViewModel : ViewModel() {
    private val api = ApiClient.api

    private val _sections = MutableStateFlow<List<HomeSection>>(emptyList())
    val sections: StateFlow<List<HomeSection>> = _sections

    private val _featuredSpeakers = MutableStateFlow<List<Speaker>>(emptyList())
    val featuredSpeakers: StateFlow<List<Speaker>> = _featuredSpeakers

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // Well-known popular speaker IDs on TorahAnytime
    private val popularSpeakerIds = listOf(162, 287, 386, 61, 166, 289, 1227, 80, 371, 164)

    init {
        loadHome()
    }

    fun loadHome() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            val allSections = mutableListOf<HomeSection>()

            try {
                // Load multiple speaker lectures in parallel
                val deferredLectures = popularSpeakerIds.map { speakerId ->
                    async {
                        try {
                            api.getSpeakerLectures(speakerId, limit = 10, offset = 0)
                                .lecture
                                ?.filter { it.isShort != true && it.displayActive != false }
                                ?: emptyList()
                        } catch (_: Exception) {
                            emptyList()
                        }
                    }
                }

                // Load speakers list for featured row
                val deferredSpeakers = async {
                    try {
                        api.getSpeakers(limit = 30, offset = 0)
                            .speakers.values.flatten()
                            .filter { it.lectureCount > 50 }
                            .sortedByDescending { it.lectureCount }
                            .take(15)
                    } catch (_: Exception) {
                        emptyList()
                    }
                }

                // Load parasha topic lectures via search
                val deferredParasha = async {
                    try {
                        val currentParasha = getCurrentParasha()
                        if (currentParasha != null) {
                            val result = api.searchLectures(filter = currentParasha, limit = 10)
                            result.items?.mapNotNull { item ->
                                item.data?.toLecture(item.imgUrl)
                            }?.filter { it.isShort != true } ?: emptyList()
                        } else emptyList()
                    } catch (_: Exception) {
                        emptyList()
                    }
                }

                // Await all results
                val allLectureResults = deferredLectures.awaitAll()
                val speakers = deferredSpeakers.await()
                val parashaLectures = deferredParasha.await()

                _featuredSpeakers.value = speakers

                // Build "Recent Lectures" from all speaker lectures, sorted by date
                val recentLectures = allLectureResults
                    .flatten()
                    .sortedByDescending { it.dateCreated ?: it.dateRecorded }
                    .distinctBy { it.id }
                    .take(20)

                if (recentLectures.isNotEmpty()) {
                    allSections.add(HomeSection("RECENT LECTURES", recentLectures))
                }

                // Build parasha section
                if (parashaLectures.isNotEmpty()) {
                    val parashaName = getCurrentParasha() ?: "Parashat Hashavua"
                    allSections.add(HomeSection("PARASHAT $parashaName".uppercase(), parashaLectures.take(10)))
                }

                // Build "Popular" section from the highest lecture-count speakers
                val popularLectures = allLectureResults
                    .flatten()
                    .sortedByDescending { it.duration ?: 0 }
                    .distinctBy { it.id }
                    .filter { it.id !in recentLectures.map { r -> r.id } }
                    .take(15)

                if (popularLectures.isNotEmpty()) {
                    allSections.add(HomeSection("POPULAR SHIURIM", popularLectures))
                }

                _sections.value = allSections

                if (allSections.isEmpty()) {
                    _error.value = "No lectures found. Check your connection."
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = "Failed to load. Tap to retry."
            }
            _loading.value = false
        }
    }

    private fun getCurrentParasha(): String? {
        // Simple mapping based on approximate Jewish calendar cycle
        // In a real app this would use a Hebrew calendar library
        val parashaNames = listOf(
            "Tazria", "Metzora", "Acharei Mot", "Kedoshim",
            "Emor", "Behar", "Bechukotai", "Bamidbar",
            "Naso", "Behaalotecha", "Shelach", "Korach"
        )
        val weekOfYear = java.util.Calendar.getInstance().get(java.util.Calendar.WEEK_OF_YEAR)
        return parashaNames.getOrNull(weekOfYear % parashaNames.size)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLectureClick: (Lecture) -> Unit,
    onSpeakerClick: ((Int) -> Unit)? = null,
    vm: HomeViewModel = viewModel()
) {
    val sections by vm.sections.collectAsState()
    val speakers by vm.featuredSpeakers.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()

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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = TATBlue)
                    Spacer(Modifier.height(12.dp))
                    Text("Loading shiurim\u2026", color = TATTextSecondary, fontSize = 13.sp)
                }
            }
        } else if (error != null && sections.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(error ?: "", color = TATTextSecondary)
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { vm.loadHome() },
                        modifier = Modifier.focusable()
                    ) {
                        Text("Retry", color = TATBlue)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                // Featured speakers horizontal row
                if (speakers.isNotEmpty()) {
                    item {
                        Text(
                            "FEATURED SPEAKERS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = TATBrowseAllText,
                            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp)
                        )
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(speakers, key = { it.id }) { speaker ->
                                SpeakerChip(
                                    speaker = speaker,
                                    onClick = { onSpeakerClick?.invoke(speaker.id) }
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }

                // Content sections
                sections.forEach { section ->
                    item {
                        Text(
                            section.title,
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = TATBrowseAllText,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                    items(section.lectures, key = { "${section.title}_${it.id}" }) { lecture ->
                        LectureItem(
                            lecture = lecture,
                            onClick = { onLectureClick(lecture) }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }
    }
}

@Composable
private fun SpeakerChip(
    speaker: Speaker,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(72.dp)
            .focusable()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = speaker.photoUrl,
            contentDescription = speaker.fullName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(28.dp))
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = speaker.nameLast,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
