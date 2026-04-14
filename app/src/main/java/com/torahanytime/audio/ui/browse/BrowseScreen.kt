package com.torahanytime.audio.ui.browse

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.torahanytime.audio.ui.theme.TATBlue
import com.torahanytime.audio.ui.theme.TATBrowseAllText
import com.torahanytime.audio.ui.theme.TATTextSecondary

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
                title = {
                    Text("Browse", fontWeight = FontWeight.Bold, fontSize = 24.sp)
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
        ) {
            // Search bar
            OutlinedTextField(
                value = "",
                onValueChange = {},
                placeholder = { Text("Search your next Torah class\u2026", fontSize = 14.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .focusable()
                    .clickable(onClick = onNavigateToSearch),
                enabled = false,
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Outlined.Search, "Search", tint = TATTextSecondary)
                },
                shape = MaterialTheme.shapes.medium
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "BROWSE ALL",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = TATBrowseAllText,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            BrowseRow(
                icon = Icons.Outlined.BookmarkBorder,
                label = "Topics",
                onClick = onNavigateToTopics
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            BrowseRow(
                icon = Icons.Outlined.Person,
                label = "Speakers",
                onClick = onNavigateToSpeakers
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            BrowseRow(
                icon = Icons.Outlined.List,
                label = "Series",
                onClick = onNavigateToSeries
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            BrowseRow(
                icon = Icons.Outlined.Groups,
                label = "Organizations",
                onClick = { /* TODO */ }
            )
        }
    }
}

@Composable
private fun BrowseRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isFocused) TATBlue else TATTextSecondary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium
            ),
            color = if (isFocused) TATBlue else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = TATBlue,
            modifier = Modifier.size(20.dp)
        )
    }
}
