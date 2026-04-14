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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.torahanytime.audio.data.model.Speaker
import com.torahanytime.audio.data.repository.SpeakerRepository
import com.torahanytime.audio.ui.common.SpeakerItem
import com.torahanytime.audio.ui.theme.TATBlue
import com.torahanytime.audio.ui.theme.TATBrowseAllText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SpeakerListViewModel : ViewModel() {
    private val repo = SpeakerRepository()

    private val _speakers = MutableStateFlow<Map<String, List<Speaker>>>(emptyMap())
    val speakers: StateFlow<Map<String, List<Speaker>>> = _speakers

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private var offset = 0
    private var totalSpeakers = Int.MAX_VALUE

    init {
        loadMore()
    }

    fun loadMore() {
        if (_loading.value || offset >= totalSpeakers) return
        viewModelScope.launch {
            _loading.value = true
            try {
                val response = repo.getSpeakers(offset = offset)
                totalSpeakers = response.totalSpeakers
                val current = _speakers.value.toMutableMap()
                response.speakers.forEach { (letter, list) ->
                    current[letter] = (current[letter] ?: emptyList()) + list
                }
                _speakers.value = current.toSortedMap()
                offset += response.limit
            } catch (e: Exception) {
                e.printStackTrace()
            }
            _loading.value = false
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Speakers", fontWeight = FontWeight.Bold) },
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
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            speakers.forEach { (letter, speakerList) ->
                item {
                    Text(
                        text = letter,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = TATBlue,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                items(speakerList, key = { it.id }) { speaker ->
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

            if (loading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = TATBlue, modifier = Modifier.size(24.dp))
                    }
                }
            } else if (speakers.isNotEmpty()) {
                item {
                    TextButton(
                        onClick = { vm.loadMore() },
                        modifier = Modifier.fillMaxWidth().focusable()
                    ) {
                        Text("Load More", color = TATBlue)
                    }
                }
            }
        }
    }
}
