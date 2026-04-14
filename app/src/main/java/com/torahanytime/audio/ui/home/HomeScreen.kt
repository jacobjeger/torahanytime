package com.torahanytime.audio.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.torahanytime.audio.data.model.Lecture
import com.torahanytime.audio.data.repository.ContentCache
import com.torahanytime.audio.data.repository.SpeakerCache
import com.torahanytime.audio.ui.common.CategoryTile
import com.torahanytime.audio.ui.common.LectureItem
import com.torahanytime.audio.ui.theme.TATBlue
import com.torahanytime.audio.ui.theme.TATBrowseAllText
import com.torahanytime.audio.ui.theme.TATOrange
import com.torahanytime.audio.ui.theme.TATTextSecondary
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    init {
        viewModelScope.launch { SpeakerCache.ensureLoaded() }
        viewModelScope.launch { ContentCache.ensureRecentLoaded() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLectureClick: (Lecture) -> Unit,
    onSpeakerClick: ((Int) -> Unit)? = null,
    onNavigateToSpeakers: (() -> Unit)? = null,
    onNavigateToTopics: (() -> Unit)? = null,
    onNavigateToSeries: (() -> Unit)? = null,
    onNavigateToSearch: (() -> Unit)? = null,
    onNavigateToParasha: (() -> Unit)? = null,
    vm: HomeViewModel = viewModel()
) {
    val lectures by ContentCache.recentLectures.collectAsState()
    val loading by ContentCache.recentLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TorahAnytime", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        if (loading && lectures.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = TATBlue)
                    Spacer(Modifier.height(12.dp))
                    Text("Loading shiurim\u2026", color = TATTextSecondary, fontSize = 13.sp)
                }
            }
        } else {
            val listState = rememberLazyListState()
            val coroutineScope = rememberCoroutineScope()

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // Category tiles grid
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CategoryTile(
                                label = "Recent\nLectures",
                                icon = Icons.Filled.AccessTime,
                                iconTint = TATBlue,
                                onClick = { coroutineScope.launch { listState.animateScrollToItem(4) } },
                                modifier = Modifier.weight(1f)
                            )
                            CategoryTile(
                                label = "Speakers",
                                icon = Icons.Filled.Person,
                                iconTint = TATOrange,
                                onClick = { onNavigateToSpeakers?.invoke() },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CategoryTile(
                                label = "Topics",
                                icon = Icons.Outlined.BookmarkBorder,
                                iconTint = Color(0xFF2E7D32),
                                onClick = { onNavigateToTopics?.invoke() },
                                modifier = Modifier.weight(1f)
                            )
                            CategoryTile(
                                label = "Series",
                                icon = Icons.Outlined.List,
                                iconTint = Color(0xFF7B1FA2),
                                onClick = { onNavigateToSeries?.invoke() },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CategoryTile(
                                label = "Search",
                                icon = Icons.Filled.Search,
                                iconTint = TATBlue,
                                onClick = { onNavigateToSearch?.invoke() },
                                modifier = Modifier.weight(1f)
                            )
                            CategoryTile(
                                label = "Parashat\nHashavua",
                                icon = Icons.Filled.MenuBook,
                                iconTint = TATOrange,
                                onClick = { onNavigateToParasha?.invoke() },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Recent Lectures header
                item {
                    Text(
                        "RECENT LECTURES",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold),
                        color = TATBrowseAllText,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }

                items(lectures, key = { it.id }) { lecture ->
                    LectureItem(lecture = lecture, onClick = { onLectureClick(lecture) })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                }
            }
        }
    }
}
