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
import com.torahanytime.audio.data.model.Speaker
import com.torahanytime.audio.data.repository.SpeakerCache
import com.torahanytime.audio.ui.common.SpeakerItem
import com.torahanytime.audio.ui.theme.TATBlue
import kotlinx.coroutines.launch

class SpeakerListViewModel : ViewModel() {
    init {
        viewModelScope.launch { SpeakerCache.ensureLoaded() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeakerListScreen(
    onBack: () -> Unit,
    onSpeakerClick: (Int) -> Unit,
    vm: SpeakerListViewModel = viewModel()
) {
    val speakers by SpeakerCache.speakers.collectAsState()
    val loading by SpeakerCache.loading.collectAsState()
    val totalCount by SpeakerCache.totalCount.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Speakers", fontWeight = FontWeight.Bold)
                        if (loading) {
                            Text("Loading\u2026 ($totalCount so far)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else if (totalCount > 0) {
                            Text("$totalCount speakers", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        if (speakers.isEmpty() && loading) {
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
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                                color = TATBlue,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        items(speakerList, key = { "speaker_${it.id}" }) { speaker ->
                            SpeakerItem(speaker = speaker, onClick = { onSpeakerClick(speaker.id) })
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        }
                    }
                }
                if (loading) {
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
