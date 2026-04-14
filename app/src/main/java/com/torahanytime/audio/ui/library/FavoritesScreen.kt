package com.torahanytime.audio.ui.library

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.torahanytime.audio.data.model.Lecture
import com.torahanytime.audio.data.repository.FavoriteRepository
import com.torahanytime.audio.ui.common.LectureItem
import com.torahanytime.audio.ui.theme.TATBlue
import com.torahanytime.audio.ui.theme.TATTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onBack: () -> Unit,
    onLectureClick: (Lecture) -> Unit
) {
    val favorites by FavoriteRepository.getAll().collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Favorites", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.focusable()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        if (favorites.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No favorites yet", color = TATTextSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                items(favorites, key = { it.lectureId }) { fav ->
                    val lecture = Lecture(
                        id = fav.lectureId,
                        title = fav.title,
                        speakerNameFirst = fav.speakerName.split(" ").firstOrNull(),
                        speakerNameLast = fav.speakerName.split(" ").drop(1).joinToString(" "),
                        mp3Url = fav.mp3Url,
                        thumbnailUrl = fav.thumbnailUrl,
                        duration = fav.duration,
                        languageName = fav.languageName
                    )
                    LectureItem(
                        lecture = lecture,
                        onClick = { onLectureClick(lecture) }
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
