package com.torahanytime.audio.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.torahanytime.audio.data.local.entity.DownloadedLecture
import com.torahanytime.audio.data.model.Lecture
import com.torahanytime.audio.ui.theme.TATBlue
import com.torahanytime.audio.ui.theme.TATTextSecondary
import com.torahanytime.audio.util.formatDuration
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    onBack: () -> Unit,
    onLectureClick: (Lecture) -> Unit
) {
    val downloads by TATApplication.db.downloadDao().getAll().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Downloads", fontWeight = FontWeight.Bold)
                        if (downloads.isNotEmpty()) {
                            val totalMB = downloads.sumOf { it.fileSizeBytes } / (1024 * 1024)
                            Text("${downloads.size} lectures \u00b7 ${totalMB}MB", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        if (downloads.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No downloaded lectures", color = TATTextSecondary)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(downloads, key = { it.lectureId }) { dl ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusable()
                            .clickable {
                                onLectureClick(Lecture(
                                    id = dl.lectureId, title = dl.title,
                                    speakerNameFirst = dl.speakerName.split(" ").firstOrNull(),
                                    speakerNameLast = dl.speakerName.split(" ").drop(1).joinToString(" "),
                                    mp3Url = dl.localFilePath, thumbnailUrl = dl.thumbnailUrl,
                                    duration = dl.duration, languageName = dl.languageName
                                ))
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (dl.thumbnailUrl != null) {
                            AsyncImage(model = dl.thumbnailUrl, contentDescription = null, contentScale = ContentScale.Crop,
                                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)))
                            Spacer(Modifier.width(12.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(dl.title, fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text("${dl.speakerName} \u00b7 ${formatDuration(dl.duration)}", fontSize = 12.sp, color = TATTextSecondary)
                        }
                        IconButton(onClick = {
                            scope.launch {
                                File(dl.localFilePath).delete()
                                TATApplication.db.downloadDao().delete(dl.lectureId)
                            }
                        }, modifier = Modifier.focusable()) {
                            Icon(Icons.Default.Delete, "Delete", tint = TATTextSecondary)
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                }
            }
        }
    }
}
