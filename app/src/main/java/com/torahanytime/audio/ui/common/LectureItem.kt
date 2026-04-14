package com.torahanytime.audio.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
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

    Row(
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
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
                onClick = {
                    scope.launch { FavoriteRepository.toggleFavorite(lecture) }
                },
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
