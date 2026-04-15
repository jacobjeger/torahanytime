package com.torahanytime.audio.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
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
import coil.compose.AsyncImage
import com.torahanytime.audio.TATApplication
import com.torahanytime.audio.data.local.entity.ListeningHistory
import com.torahanytime.audio.data.model.Lecture
import com.torahanytime.audio.ui.theme.TATBlue
import com.torahanytime.audio.ui.theme.TATTextSecondary
import com.torahanytime.audio.util.formatDuration
import kotlinx.coroutines.launch
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onLectureClick: (Lecture) -> Unit
) {
    val history by TATApplication.db.historyDao().getAll().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var showClearConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.focusable()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (history.isNotEmpty()) {
                        TextButton(
                            onClick = { showClearConfirm = true },
                            modifier = Modifier.focusable()
                        ) {
                            Text("Clear All", color = TATBlue, fontSize = 12.sp)
                        }
                    }
                }
            )
        }
    ) { padding ->

        if (showClearConfirm) {
            AlertDialog(
                onDismissRequest = { showClearConfirm = false },
                title = { Text("Clear History") },
                text = { Text("Remove all listening history?") },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch { TATApplication.db.historyDao().deleteAll() }
                        showClearConfirm = false
                    }) { Text("Clear", color = Color.Red) }
                },
                dismissButton = {
                    TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
                }
            )
        }
        if (history.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No listening history yet", color = TATTextSecondary)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(history, key = { it.lectureId }) { entry ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart) {
                                scope.launch { TATApplication.db.historyDao().delete(entry.lectureId) }
                                true
                            } else false
                        }
                    )
                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            val color by animateColorAsState(
                                if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart)
                                    Color.Red.copy(alpha = 0.15f)
                                else Color.Transparent,
                                label = "swipeBg"
                            )
                            Box(
                                modifier = Modifier.fillMaxSize().background(color).padding(horizontal = 20.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(Icons.Default.Delete, "Remove", tint = Color.Red.copy(alpha = 0.7f))
                            }
                        },
                        enableDismissFromStartToEnd = false,
                        enableDismissFromEndToStart = true
                    ) {
                        HistoryItem(
                            entry = entry,
                            onClick = {
                                onLectureClick(Lecture(
                                    id = entry.lectureId,
                                    title = entry.title,
                                    speakerNameFirst = entry.speakerName.split(" ").firstOrNull(),
                                    speakerNameLast = entry.speakerName.split(" ").drop(1).joinToString(" "),
                                    mp3Url = entry.mp3Url,
                                    thumbnailUrl = entry.thumbnailUrl,
                                    duration = entry.duration,
                                    languageName = entry.languageName
                                ))
                            },
                            onDelete = { scope.launch { TATApplication.db.historyDao().delete(entry.lectureId) } }
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                }
            }
        }
    }
}

@Composable
private fun HistoryItem(entry: ListeningHistory, onClick: () -> Unit, onDelete: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .focusable()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (entry.thumbnailUrl != null) {
            AsyncImage(
                model = entry.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp))
            )
            Spacer(Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.title, fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                "${entry.speakerName} \u00b7 ${formatDuration(entry.duration)}",
                fontSize = 12.sp, color = TATTextSecondary
            )
            if (entry.position > 0 && entry.duration > 0) {
                val pct = ((entry.position.toFloat() / 1000f) / entry.duration.toFloat()).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { pct },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp).height(3.dp),
                    color = TATBlue,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            }
        }
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(32.dp).focusable()
        ) {
            Icon(Icons.Default.Close, "Remove", tint = TATTextSecondary, modifier = Modifier.size(16.dp))
        }
    }
}
