package com.torahanytime.audio.ui.library

import androidx.compose.foundation.clickable
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.torahanytime.audio.data.api.ApiClient
import com.torahanytime.audio.data.model.FollowedSpeaker
import com.torahanytime.audio.ui.common.ErrorRetryState
import com.torahanytime.audio.ui.theme.TATBlue
import com.torahanytime.audio.ui.theme.TATTextSecondary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FollowingViewModel : ViewModel() {
    private val api = ApiClient.api
    private val _speakers = MutableStateFlow<List<FollowedSpeaker>>(emptyList())
    val speakers: StateFlow<List<FollowedSpeaker>> = _speakers
    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init { load() }

    fun load() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val response = api.getFollowedSpeakers()
                _speakers.value = response.speakers?.values?.toList() ?: emptyList()
            } catch (e: Exception) {
                _error.value = "Failed to load. Sign in required."
            }
            _loading.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowingScreen(
    onBack: () -> Unit,
    onSpeakerClick: (Int) -> Unit,
    vm: FollowingViewModel = viewModel()
) {
    val speakers by vm.speakers.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Following", fontWeight = FontWeight.Bold) },
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
                onRetry = { vm.load() },
                modifier = Modifier.padding(padding)
            )
            speakers.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Not following any speakers yet", color = TATTextSecondary)
            }
            else -> LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(speakers, key = { it.id ?: 0 }) { speaker ->
                    val photoUrl = speaker.image?.let {
                        "https://images.weserv.nl/?url=https://torahanytime-files.sfo2.digitaloceanspaces.com/assets/flash/speakers/$it&w=200&h=200&fit=cover"
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusable()
                            .clickable { speaker.id?.let { onSpeakerClick(it) } }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(48.dp).clip(CircleShape)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "${speaker.title ?: ""} ${speaker.nameFirst ?: ""} ${speaker.nameLast ?: ""}".trim(),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                }
            }
        }
    }
}
