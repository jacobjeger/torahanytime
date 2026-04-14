package com.torahanytime.audio.ui.home

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.torahanytime.audio.data.model.Lecture
import com.torahanytime.audio.ui.browse.*
import com.torahanytime.audio.ui.library.LibraryScreen
import com.torahanytime.audio.ui.player.*
import com.torahanytime.audio.ui.search.SearchScreen
import com.torahanytime.audio.ui.theme.TATBlue
import com.torahanytime.audio.ui.theme.TATTextSecondary
import com.torahanytime.audio.ui.theme.TATTheme

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TATTheme {
                MainApp()
            }
        }
    }
}

sealed class BottomTab(val route: String, val label: String, val icon: ImageVector) {
    data object Home : BottomTab("home", "Home", Icons.Default.Home)
    data object Browse : BottomTab("browse", "Browse", Icons.Default.Search)
    data object Donate : BottomTab("donate", "Donate", Icons.Default.Favorite)
    data object Library : BottomTab("library", "My TAT", Icons.Default.Person)
}

private val tabs = listOf(BottomTab.Home, BottomTab.Browse, BottomTab.Donate, BottomTab.Library)

@Composable
fun MainApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val playerViewModel: PlayerViewModel = viewModel(
        factory = PlayerViewModel.Factory(context)
    )
    val playerState by playerViewModel.state.collectAsState()
    var showFullPlayer by remember { mutableStateOf(false) }

    if (showFullPlayer && playerState.currentLecture != null) {
        PlayerScreen(
            state = playerState,
            onBack = { showFullPlayer = false },
            onTogglePlayPause = playerViewModel::togglePlayPause,
            onSeek = playerViewModel::seekTo,
            onSkipForward = playerViewModel::skipForward,
            onSkipBackward = playerViewModel::skipBackward,
            onSpeedChange = playerViewModel::setPlaybackSpeed
        )
        return
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val onLectureClick: (Lecture) -> Unit = { lecture ->
        playerViewModel.playLecture(lecture)
    }

    Scaffold(
        bottomBar = {
            Column {
                // Mini player
                if (playerState.currentLecture != null) {
                    MiniPlayerBar(
                        state = playerState,
                        onTogglePlayPause = playerViewModel::togglePlayPause,
                        onClose = playerViewModel::stop,
                        onTap = { showFullPlayer = true }
                    )
                }

                // Bottom nav
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute?.startsWith(tab.route) == true,
                            onClick = {
                                if (tab is BottomTab.Donate) {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.torahanytime.com/donate"))
                                    context.startActivity(intent)
                                    return@NavigationBarItem
                                }
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(tab.icon, contentDescription = tab.label)
                            },
                            label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = TATBlue,
                                selectedTextColor = TATBlue,
                                unselectedIconColor = TATTextSecondary,
                                unselectedTextColor = TATTextSecondary,
                                indicatorColor = TATBlue.copy(alpha = 0.1f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                HomeScreen(onLectureClick = onLectureClick)
            }

            composable("browse") {
                BrowseScreen(
                    onNavigateToSpeakers = { navController.navigate("speakers") },
                    onNavigateToTopics = { navController.navigate("topics") },
                    onNavigateToSeries = { navController.navigate("series") },
                    onNavigateToSearch = { navController.navigate("search") }
                )
            }

            composable("library") {
                LibraryScreen()
            }

            composable("speakers") {
                SpeakerListScreen(
                    onBack = { navController.popBackStack() },
                    onSpeakerClick = { id -> navController.navigate("speaker/$id") }
                )
            }

            composable(
                "speaker/{speakerId}",
                arguments = listOf(navArgument("speakerId") { type = NavType.IntType })
            ) { backStackEntry ->
                val speakerId = backStackEntry.arguments?.getInt("speakerId") ?: return@composable
                SpeakerDetailScreen(
                    speakerId = speakerId,
                    onBack = { navController.popBackStack() },
                    onLectureClick = onLectureClick
                )
            }

            composable("topics") {
                TopicListScreen(
                    onBack = { navController.popBackStack() },
                    onTopicClick = { /* TODO: navigate to topic lectures */ }
                )
            }

            composable("series") {
                SeriesListScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable("search") {
                SearchScreen(
                    onBack = { navController.popBackStack() },
                    onSpeakerClick = { id -> navController.navigate("speaker/$id") },
                    onTopicClick = { /* TODO */ }
                )
            }
        }
    }
}
