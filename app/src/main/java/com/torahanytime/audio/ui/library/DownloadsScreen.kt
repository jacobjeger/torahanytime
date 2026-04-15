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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.torahanytime.audio.TATApplication
import com.torahanytime.audio.data.download.LectureDownloader
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
    val context = LocalContext.current
    val downloads by TATApplication.db.downloadDao().getAll().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    // Storage stats
    val totalDownloadBytes = remember(downloads) { downloads.sumOf { it.fileSizeBytes } }
    val freeBytes = remember { LectureDownloader.getFreeDiskSpace(context) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Downloads", fontWeight = FontWeight.Bold)
                        if (downloads.isNotEmpty()) {
                            Text(
                                "${downloads.size} lectures · ${formatBytes(totalDownloadBytes)} used · ${formatBytes(freeBytes)} free",
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
                }
            )
        }
    ) { padding ->
        if (downloads.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No downloaded lectures", color = TATTextSecondary)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Tap the cloud icon on any lecture to download",
                        color = TATTextSecondary.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                // Storage bar
                item {
                    StorageBar(
                        usedBytes = totalDownloadBytes,
                        freeBytes = freeBytes,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }

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
                            Text(
                                "${dl.speakerName} · ${formatDuration(dl.duration)} · ${formatBytes(dl.fileSizeBytes)}",
                                fontSize = 12.sp, color = TATTextSecondary
                            )
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

@Composable
private fun StorageBar(
    usedBytes: Long,
    freeBytes: Long,
    modifier: Modifier = Modifier
) {
    val totalBytes = usedBytes + freeBytes
    val fraction = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f

    Column(modifier = modifier.fillMaxWidth()) {
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = TATBlue,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${formatBytes(usedBytes)} downloads", fontSize = 11.sp, color = TATTextSecondary)
            Text("${formatBytes(freeBytes)} free", fontSize = 11.sp, color = TATTextSecondary)
        }
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024L * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024))
    }
}
