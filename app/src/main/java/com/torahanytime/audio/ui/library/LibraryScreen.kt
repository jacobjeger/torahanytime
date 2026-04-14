package com.torahanytime.audio.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
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
import com.torahanytime.audio.data.api.AuthManager
import com.torahanytime.audio.ui.theme.TATBlue
import com.torahanytime.audio.ui.theme.TATTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToHistory: () -> Unit = {},
    onNavigateToPlaylists: () -> Unit = {},
    onNavigateToListenLater: () -> Unit = {},
    onNavigateToFollowing: () -> Unit = {},
    onNavigateToDownloads: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val isLoggedIn by AuthManager.isLoggedIn.collectAsState()
    val userEmail by AuthManager.userEmail.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        if (isLoggedIn) {
                            Text("Hello", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = TATBlue)
                            Text(
                                userEmail ?: "",
                                fontSize = 12.sp,
                                color = TATTextSecondary
                            )
                        } else {
                            Text("My TAT", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = TATBlue)
                        }
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
        ) {
            if (!isLoggedIn) {
                // Login prompt
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Sign in to sync your favorites and history",
                            color = TATTextSecondary,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = onNavigateToLogin,
                            modifier = Modifier.focusable(),
                            colors = ButtonDefaults.buttonColors(containerColor = TATBlue)
                        ) {
                            Text("Sign In")
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }

            LibraryRow(
                icon = Icons.Outlined.History,
                label = "History",
                onClick = onNavigateToHistory
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            LibraryRow(
                icon = Icons.Outlined.QueueMusic,
                label = "Playlists",
                onClick = onNavigateToPlaylists
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            LibraryRow(
                icon = Icons.Outlined.Headphones,
                label = "Listen Later",
                onClick = onNavigateToListenLater
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            LibraryRow(
                icon = Icons.Outlined.Download,
                label = "Downloads",
                onClick = onNavigateToDownloads
            )

            if (isLoggedIn) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                LibraryRow(
                    icon = Icons.Outlined.PersonOutline,
                    label = "Following",
                    onClick = onNavigateToFollowing
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                LibraryRow(
                    icon = Icons.Outlined.Settings,
                    label = "Settings",
                    onClick = onNavigateToSettings
                )

                Spacer(Modifier.weight(1f))

                // Logout button
                LibraryRow(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    label = "Sign Out",
                    onClick = { AuthManager.logout() }
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun LibraryRow(
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
            .padding(horizontal = 16.dp, vertical = 16.dp),
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
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = if (isFocused) TATBlue else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = TATTextSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}
