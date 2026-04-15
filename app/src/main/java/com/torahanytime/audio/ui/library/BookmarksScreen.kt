package com.torahanytime.audio.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.torahanytime.audio.TATApplication
import com.torahanytime.audio.data.local.entity.Bookmark
import com.torahanytime.audio.data.model.Lecture
import com.torahanytime.audio.ui.theme.TATBlue
import com.torahanytime.audio.ui.theme.TATTextSecondary
import com.torahanytime.audio.util.formatDuration
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(
    onBack: () -> Unit,
    onLectureClick: (Lecture, Long) -> Unit
) {
    val bookmarks by TATApplication.db.bookmarkDao().getAll().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bookmarks", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.focusable()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (bookmarks.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No bookmarks yet", color = TATTextSecondary)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(bookmarks, key = { it.id }) { bookmark ->
                    BookmarkItem(
                        bookmark = bookmark,
                        onClick = {
                            onLectureClick(
                                Lecture(
                                    id = bookmark.lectureId,
                                    title = bookmark.lectureTitle,
                                    speakerNameFirst = bookmark.speakerName.split(" ").firstOrNull(),
                                    speakerNameLast = bookmark.speakerName.split(" ").drop(1).joinToString(" ")
                                ),
                                bookmark.position
                            )
                        },
                        onDelete = {
                            scope.launch { TATApplication.db.bookmarkDao().delete(bookmark.id) }
                        }
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

@Composable
private fun BookmarkItem(
    bookmark: Bookmark,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .focusable()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                bookmark.lectureTitle,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${bookmark.speakerName} \u00b7 at ${formatDuration((bookmark.position / 1000).toInt())}",
                fontSize = 12.sp,
                color = TATTextSecondary
            )
            if (bookmark.note.isNotEmpty()) {
                Text(
                    "\u201c${bookmark.note}\u201d",
                    fontSize = 12.sp,
                    color = TATBlue,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
