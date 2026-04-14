package com.torahanytime.audio.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.torahanytime.audio.data.model.Lecture
import com.torahanytime.audio.data.repository.FavoriteRepository
import com.torahanytime.audio.data.repository.SpeakerCache
import com.torahanytime.audio.ui.theme.TATBlue
import com.torahanytime.audio.ui.theme.TATOrange
import com.torahanytime.audio.ui.theme.TATTextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LectureItem(
    lecture: Lecture,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showFavorite: Boolean = true,
    onAddToQueue: (() -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    val isFavorite by FavoriteRepository.isFavorite(lecture.id).collectAsState(initial = false)
    val scope = rememberCoroutineScope()

    // Get speaker photo from cache
    val speakerPhotoUrl = remember(lecture.speaker) {
        lecture.speaker?.let { speakerId ->
            SpeakerCache.getSpeakerById(speakerId)?.photoUrl
        }
    }

    if (onAddToQueue != null) {
        // Swipeable version
        val dismissState = rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                when (value) {
                    SwipeToDismissBoxValue.StartToEnd -> {
                        // Swipe right → toggle favorite
                        scope.launch { FavoriteRepository.toggleFavorite(lecture) }
                        false // don't actually dismiss
                    }
                    SwipeToDismissBoxValue.EndToStart -> {
                        // Swipe left → add to queue
                        onAddToQueue()
                        false // don't actually dismiss
                    }
                    SwipeToDismissBoxValue.Settled -> false
                }
            }
        )

        SwipeToDismissBox(
            state = dismissState,
            modifier = modifier,
            backgroundContent = {
                val direction = dismissState.dismissDirection
                val color by animateColorAsState(
                    when (direction) {
                        SwipeToDismissBoxValue.StartToEnd -> TATOrange.copy(alpha = 0.15f)
                        SwipeToDismissBoxValue.EndToStart -> TATBlue.copy(alpha = 0.15f)
                        else -> Color.Transparent
                    },
                    label = "swipeBg"
                )
                val alignment = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                    else -> Alignment.CenterEnd
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color)
                        .padding(horizontal = 20.dp),
                    contentAlignment = alignment
                ) {
                    when (direction) {
                        SwipeToDismissBoxValue.StartToEnd -> {
                            Icon(
                                Icons.Filled.Favorite,
                                contentDescription = "Favorite",
                                tint = TATOrange,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        SwipeToDismissBoxValue.EndToStart -> {
                            Icon(
                                Icons.Filled.QueueMusic,
                                contentDescription = "Add to Queue",
                                tint = TATBlue,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        else -> {}
                    }
                }
            },
            enableDismissFromStartToEnd = true,
            enableDismissFromEndToStart = true
        ) {
            LectureItemContent(
                lecture = lecture,
                onClick = onClick,
                isFocused = isFocused,
                onFocusChanged = { isFocused = it },
                isFavorite = isFavorite,
                showFavorite = showFavorite,
                speakerPhotoUrl = speakerPhotoUrl,
                onToggleFavorite = { scope.launch { FavoriteRepository.toggleFavorite(lecture) } }
            )
        }
    } else {
        // Non-swipeable version
        LectureItemContent(
            lecture = lecture,
            onClick = onClick,
            modifier = modifier,
            isFocused = isFocused,
            onFocusChanged = { isFocused = it },
            isFavorite = isFavorite,
            showFavorite = showFavorite,
            speakerPhotoUrl = speakerPhotoUrl,
            onToggleFavorite = { scope.launch { FavoriteRepository.toggleFavorite(lecture) } }
        )
    }
}

@Composable
private fun LectureItemContent(
    lecture: Lecture,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isFocused: Boolean,
    onFocusChanged: (Boolean) -> Unit,
    isFavorite: Boolean,
    showFavorite: Boolean,
    speakerPhotoUrl: String?,
    onToggleFavorite: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .onFocusChanged { onFocusChanged(it.isFocused) }
            .focusable()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Speaker photo (small circle) or lecture thumbnail
        Box(modifier = Modifier.size(56.dp)) {
            if (lecture.thumbnailUrl != null) {
                AsyncImage(
                    model = lecture.thumbnailUrl,
                    contentDescription = lecture.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }
            // Small speaker avatar overlay
            if (speakerPhotoUrl != null) {
                AsyncImage(
                    model = speakerPhotoUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .align(Alignment.BottomEnd)
                )
            }
        }
        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = lecture.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = if (isFocused) TATBlue else Color.Unspecified
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = buildString {
                    append(lecture.speakerFullName)
                    if (lecture.durationFormatted.isNotEmpty()) {
                        append(" \u00b7 ${lecture.durationFormatted}")
                    }
                    lecture.languageName?.let { append(" \u00b7 $it") }
                },
                style = MaterialTheme.typography.bodySmall,
                color = TATTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Favorite heart icon
        if (showFavorite) {
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (isFavorite) "Unfavorite" else "Favorite",
                    modifier = Modifier.size(18.dp),
                    tint = if (isFavorite) TATOrange else TATTextSecondary
                )
            }
        }
    }
}
