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
import com.torahanytime.audio.data.model.Speaker
import com.torahanytime.audio.data.repository.SpeakerRepository
import com.torahanytime.audio.ui.common.SpeakerItem
import com.torahanytime.audio.ui.theme.TATBlue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SpeakerListViewModel : ViewModel() {
    private val repo = SpeakerRepository()

    private val _speakers = MutableStateFlow<Map<String, List<Speaker>>>(emptyMap())
    val speakers: StateFlow<Map<String, List<Speaker>>> = _speakers

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _initialLoading = MutableStateFlow(true)
    val initialLoading: StateFlow<Boolean> = _initialLoading

    private var offset = 0
    private var totalSpeakers = Int.MAX_VALUE
    private var allLoaded = false

    init {
        loadAll()
    }

    private fun loadAll() {
        viewModelScope.launch {
            _initialLoading.value = true
            _loading.value = true
            try {
                // Load all pages until we have all speakers
                while (offset < totalSpeakers && !allLoaded) {
                    val response = repo.getSpeakers(offset = offset, limit = 30)
                    totalSpeakers = response.totalSpeakers
                    val current = _speakers.value.toMutableMap()
                    var newCount = 0
                    response.speakers.forEach { (letter, list) ->
                        if (list.isNotEmpty()) {
                            current[letter] = (current[letter] ?: emptyList()) + list
                            newCount += list.size
                        }
                    }
                    _speakers.value = current.toSortedMap()
                    offset += response.limit

                    // Show first page immediately
                    if (_initialLoading.value) {
                        _initialLoading.value = false
                    }

                    if (newCount == 0 || offset >= totalSpeakers) {
                        allLoaded = true
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            _loading.value = false
            _initialLoading.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeakerListScreen(
    onBack: () -> Unit,
    onSpeakerClick: (Int) -> Unit,
    vm: SpeakerListViewModel = viewModel()
) {
    val speakers by vm.speakers.collectAsState()
    val loading by vm.loading.collectAsState()
    val initialLoading by vm.initialLoading.collectAsState()

    val totalCount = speakers.values.sumOf { it.size }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Speakers", fontWeight = FontWeight.Bold)
                        if (loading && !initialLoading) {
                            Text(
                                "Loading\u2026 ($totalCount so far)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else if (!loading) {
                            Text(
                                "$totalCount speakers",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.focusable()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        if (initialLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TATBlue)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                speakers.forEach { (letter, speakerList) ->
                    if (speakerList.isNotEmpty()) {
                        item(key = "header_$letter") {
                            Text(
                                text = letter,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold, fontSize = 18.sp
                                ),
                                color = TATBlue,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        items(speakerList, key = { "speaker_${it.id}" }) { speaker ->
                            SpeakerItem(
                                speaker = speaker,
                                onClick = { onSpeakerClick(speaker.id) }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        }
                    }
                }

                if (loading && !initialLoading) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = TATBlue, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
    }
}
