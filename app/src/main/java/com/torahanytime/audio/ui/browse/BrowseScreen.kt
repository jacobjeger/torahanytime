package com.torahanytime.audio.ui.browse

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.torahanytime.audio.ui.common.CategoryTile
import com.torahanytime.audio.ui.theme.TATBlue
import com.torahanytime.audio.ui.theme.TATOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    onNavigateToSpeakers: () -> Unit,
    onNavigateToTopics: () -> Unit,
    onNavigateToSeries: () -> Unit,
    onNavigateToSearch: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Browse", fontWeight = FontWeight.Bold, fontSize = 24.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CategoryTile(
                    label = "Topics",
                    icon = Icons.Outlined.BookmarkBorder,
                    iconTint = Color(0xFF2E7D32),
                    onClick = onNavigateToTopics,
                    modifier = Modifier.weight(1f)
                )
                CategoryTile(
                    label = "Speakers",
                    icon = Icons.Filled.Person,
                    iconTint = TATOrange,
                    onClick = onNavigateToSpeakers,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CategoryTile(
                    label = "Series",
                    icon = Icons.Outlined.List,
                    iconTint = Color(0xFF7B1FA2),
                    onClick = onNavigateToSeries,
                    modifier = Modifier.weight(1f)
                )
                CategoryTile(
                    label = "Organizations",
                    icon = Icons.Outlined.Groups,
                    iconTint = TATBlue,
                    onClick = { },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(10.dp))
            CategoryTile(
                label = "Search Classes",
                icon = Icons.Filled.Search,
                iconTint = TATBlue,
                onClick = onNavigateToSearch,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
