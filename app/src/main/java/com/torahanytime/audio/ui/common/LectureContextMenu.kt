package com.torahanytime.audio.ui.common

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.torahanytime.audio.data.download.LectureDownloader
import com.torahanytime.audio.data.model.Lecture
import com.torahanytime.audio.data.repository.FavoriteRepository
import com.torahanytime.audio.ui.theme.TATBlue
import com.torahanytime.audio.ui.theme.TATOrange
import com.torahanytime.audio.ui.theme.TATTextSecondary
import kotlinx.coroutines.launch

@Composable
fun LectureContextMenu(
    lecture: Lecture,
    isFavorite: Boolean,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onAddToQueue: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                lecture.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2
            )
        },
        text = {
            Column {
                ContextMenuItem(
                    icon = Icons.Default.PlayArrow,
                    label = "Play",
                    onClick = { onPlay(); onDismiss() },
                    modifier = Modifier.focusRequester(focusRequester)
                )
                if (onAddToQueue != null) {
                    ContextMenuItem(
                        icon = Icons.Default.QueueMusic,
                        label = "Add to Queue",
                        onClick = { onAddToQueue(); onDismiss() }
                    )
                }
                if (lecture.noDownload != true && lecture.mp3Url != null) {
                    ContextMenuItem(
                        icon = Icons.Outlined.CloudDownload,
                        label = "Download",
                        onClick = {
                            scope.launch { LectureDownloader.download(context, lecture) }
                            onDismiss()
                        }
                    )
                }
                ContextMenuItem(
                    icon = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    label = if (isFavorite) "Unfavorite" else "Favorite",
                    tint = if (isFavorite) TATOrange else TATTextSecondary,
                    onClick = {
                        scope.launch { FavoriteRepository.toggleFavorite(lecture) }
                        onDismiss()
                    }
                )
                ContextMenuItem(
                    icon = Icons.Default.Share,
                    label = "Share",
                    onClick = {
                        val shareText = "${lecture.title} by ${lecture.speakerFullName}\nhttps://www.torahanytime.com/#/lectures?id=${lecture.id}"
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share lecture"))
                        onDismiss()
                    }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ContextMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: androidx.compose.ui.graphics.Color = TATTextSecondary
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .focusable()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 14.sp)
    }
}
