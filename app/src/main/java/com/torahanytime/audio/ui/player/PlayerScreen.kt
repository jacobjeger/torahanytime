package com.torahanytime.audio.ui.player

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.torahanytime.audio.ui.theme.TATBlue
import com.torahanytime.audio.ui.theme.TATTextSecondary
import com.torahanytime.audio.util.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    state: PlayerState,
    onBack: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onSkipForward: () -> Unit,
    onSkipBackward: () -> Unit,
    onSpeedChange: (Float) -> Unit
) {
    val lecture = state.currentLecture ?: return

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Now Playing", fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.focusable()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            // Thumbnail
            AsyncImage(
                model = lecture.thumbnailUrl,
                contentDescription = lecture.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(180.dp)
                    .clip(RoundedCornerShape(12.dp))
            )

            Spacer(Modifier.height(20.dp))

            // Title
            Text(
                text = lecture.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                ),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(4.dp))

            // Speaker
            Text(
                text = lecture.speakerFullName,
                style = MaterialTheme.typography.bodyMedium,
                color = TATBlue
            )

            lecture.languageName?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = TATTextSecondary
                )
            }

            Spacer(Modifier.height(24.dp))

            // Seek bar
            val progress = if (state.duration > 0) {
                (state.currentPosition.toFloat() / state.duration.toFloat()).coerceIn(0f, 1f)
            } else 0f

            Slider(
                value = progress,
                onValueChange = { onSeek((it * state.duration).toLong()) },
                modifier = Modifier.fillMaxWidth().focusable(),
                colors = SliderDefaults.colors(
                    thumbColor = TATBlue,
                    activeTrackColor = TATBlue
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    formatDuration((state.currentPosition / 1000).toInt()),
                    style = MaterialTheme.typography.bodySmall,
                    color = TATTextSecondary
                )
                Text(
                    formatDuration((state.duration / 1000).toInt()),
                    style = MaterialTheme.typography.bodySmall,
                    color = TATTextSecondary
                )
            }

            Spacer(Modifier.height(16.dp))

            // Playback controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Speed button
                TextButton(
                    onClick = {
                        val speeds = listOf(0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)
                        val currentIdx = speeds.indexOf(state.playbackSpeed)
                        val nextSpeed = speeds[(currentIdx + 1) % speeds.size]
                        onSpeedChange(nextSpeed)
                    },
                    modifier = Modifier.focusable()
                ) {
                    Text(
                        "${state.playbackSpeed}x",
                        color = TATBlue,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Skip back 15s
                IconButton(onClick = onSkipBackward, modifier = Modifier.focusable()) {
                    Icon(Icons.Default.Replay10, "Skip back", modifier = Modifier.size(32.dp))
                }

                // Play/Pause
                FilledIconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier.size(56.dp).focusable(),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = TATBlue
                    )
                ) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Skip forward 15s
                IconButton(onClick = onSkipForward, modifier = Modifier.focusable()) {
                    Icon(Icons.Default.Forward10, "Skip forward", modifier = Modifier.size(32.dp))
                }

                // Placeholder for balance
                Spacer(Modifier.width(48.dp))
            }
        }
    }
}
