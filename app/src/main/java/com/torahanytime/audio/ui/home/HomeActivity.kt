package com.torahanytime.audio.ui.home

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.torahanytime.audio.data.model.Lecture
import com.torahanytime.audio.ui.auth.LoginScreen
import com.torahanytime.audio.ui.browse.SeriesDetailScreen
import com.torahanytime.audio.ui.browse.SeriesListScreen
import com.torahanytime.audio.ui.browse.SpeakerDetailScreen
import com.torahanytime.audio.ui.browse.SpeakerListScreen
import com.torahanytime.audio.ui.browse.TopicLecturesScreen
import com.torahanytime.audio.ui.browse.TopicListScreen
import com.torahanytime.audio.ui.library.BookmarksScreen
import com.torahanytime.audio.ui.library.DownloadsScreen
import com.torahanytime.audio.ui.library.FavoritesScreen
import com.torahanytime.audio.ui.library.FollowingScreen
import com.torahanytime.audio.ui.library.HistoryScreen
import com.torahanytime.audio.ui.library.LibraryScreen
import com.torahanytime.audio.ui.library.ListenLaterScreen
import com.torahanytime.audio.ui.settings.AboutScreen
import com.torahanytime.audio.ui.settings.SettingsKeys
import com.torahanytime.audio.ui.settings.SettingsScreen
import com.torahanytime.audio.ui.settings.settingsDataStore
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.torahanytime.audio.ui.player.*
import com.torahanytime.audio.ui.search.SearchScreen
import com.torahanytime.audio.ui.common.SnackbarManager
import com.torahanytime.audio.ui.theme.TATBlue
import com.torahanytime.audio.ui.theme.TATTextSecondary
import com.torahanytime.audio.ui.theme.TATTheme
import com.torahanytime.audio.util.NetworkMonitor
import java.net.URLDecoder
import java.net.URLEncoder

class HomeActivity : ComponentActivity() {

    private var playerViewModel: PlayerViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val deepLinkData = parseDeepLink(intent)
        setContent {
            TATTheme {
                MainApp(
                    onPlayerViewModelReady = { playerViewModel = it },
                    deepLinkLectureId = deepLinkData?.first,
                    deepLinkSpeakerId = deepLinkData?.second
                )
            }
        }
    }

    private fun parseDeepLink(intent: Intent?): Pair<Int?, Int?>? {
        val uri = intent?.data ?: return null
        val uriStr = uri.toString()

        // Handle https://www.torahanytime.com/#/lectures?id=12345
        val lectureMatch = Regex("[?&]id=(\\d+)").find(uriStr)
            ?: Regex("#/lectures\\?id=(\\d+)").find(uriStr)
        if (lectureMatch != null) {
            return Pair(lectureMatch.groupValues[1].toIntOrNull(), null)
        }

        // Handle https://www.torahanytime.com/#/speakers?id=123
        val speakerMatch = Regex("#/speakers\\?id=(\\d+)").find(uriStr)
        if (speakerMatch != null) {
            return Pair(null, speakerMatch.groupValues[1].toIntOrNull())
        }

        // Handle torahanytime://lecture/12345
        if (uri.scheme == "torahanytime" && uri.host == "lecture") {
            val id = uri.lastPathSegment?.toIntOrNull()
            return Pair(id, null)
        }

        return null
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val vm = playerViewModel ?: return super.onKeyDown(keyCode, event)
        val state = vm.state.value

        return when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            KeyEvent.KEYCODE_HEADSETHOOK -> {
                if (state.currentLecture != null) {
                    vm.togglePlayPause()
                    true
                } else super.onKeyDown(keyCode, event)
            }
            KeyEvent.KEYCODE_MEDIA_NEXT -> {
                if (state.currentLecture != null) {
                    vm.skipForward(state.skipForwardSeconds)
                    true
                } else super.onKeyDown(keyCode, event)
            }
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                if (state.currentLecture != null) {
                    vm.skipBackward(state.skipBackwardSeconds)
                    true
                } else super.onKeyDown(keyCode, event)
            }
            KeyEvent.KEYCODE_STAR -> {
                // * key = cycle playback speed
                if (state.currentLecture != null) {
                    val speeds = listOf(1f, 1.25f, 1.5f, 1.75f, 2f)
                    val idx = speeds.indexOf(state.playbackSpeed)
                    vm.setPlaybackSpeed(speeds[(idx + 1) % speeds.size])
                    true
                } else super.onKeyDown(keyCode, event)
            }
            KeyEvent.KEYCODE_POUND -> {
                // # key = toggle sleep timer (30 min)
                if (state.currentLecture != null) {
                    if (state.sleepTimerMinutes != null) {
                        vm.cancelSleepTimer()
                    } else {
                        vm.setSleepTimer(30)
                    }
                    true
                } else super.onKeyDown(keyCode, event)
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }
}

sealed class BottomTab(val route: String, val label: String, val icon: ImageVector) {
    data object Home : BottomTab("home", "Home", Icons.Default.Home)
    data object Search : BottomTab("search", "Search", Icons.Default.Search)
    data object Donate : BottomTab("donate", "Donate", Icons.Default.Favorite)
    data object Library : BottomTab("library", "My TAT", Icons.Default.Person)
}

private val tabs = listOf(BottomTab.Home, BottomTab.Search, BottomTab.Donate, BottomTab.Library)

@Composable
fun MainApp(
    onPlayerViewModelReady: (PlayerViewModel) -> Unit = {},
    deepLinkLectureId: Int? = null,
    deepLinkSpeakerId: Int? = null
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val playerViewModel: PlayerViewModel = viewModel(
        factory = PlayerViewModel.Factory(context)
    )
    val playerState by playerViewModel.state.collectAsState()
    var showFullPlayer by remember { mutableStateOf(false) }
    val isOnline by NetworkMonitor.isOnline.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Collect snackbar messages
    LaunchedEffect(snackbarHostState) {
        SnackbarManager.messages.collect { message ->
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
        }
    }

    // Expose playerViewModel to activity for keypad shortcuts
    LaunchedEffect(playerViewModel) {
        onPlayerViewModelReady(playerViewModel)
    }

    // Handle deep links
    LaunchedEffect(deepLinkLectureId, deepLinkSpeakerId) {
        if (deepLinkSpeakerId != null) {
            navController.navigate("speaker/$deepLinkSpeakerId")
        }
    }

    // Rate prompt
    val lecturesPlayed by context.settingsDataStore.data.map {
        it[SettingsKeys.LECTURES_PLAYED_COUNT] ?: 0
    }.collectAsState(initial = 0)
    val rateDismissed by context.settingsDataStore.data.map {
        it[SettingsKeys.RATE_PROMPT_DISMISSED] ?: false
    }.collectAsState(initial = false)
    var showRateDialog by remember { mutableStateOf(false) }
    val rateScope = rememberCoroutineScope()

    LaunchedEffect(lecturesPlayed, rateDismissed) {
        if (lecturesPlayed >= 10 && !rateDismissed) {
            showRateDialog = true
        }
    }

    if (showRateDialog) {
        AlertDialog(
            onDismissRequest = { showRateDialog = false },
            title = { Text("Enjoying TorahAnytime?") },
            text = { Text("You've listened to $lecturesPlayed shiurim! Would you like to rate us on the Play Store?") },
            confirmButton = {
                TextButton(onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.torahanytime.audio"))
                    try { context.startActivity(intent) } catch (_: Exception) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.torahanytime.audio")))
                    }
                    rateScope.launch { context.settingsDataStore.edit { it[SettingsKeys.RATE_PROMPT_DISMISSED] = true } }
                    showRateDialog = false
                }) { Text("Rate", color = TATBlue) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        rateScope.launch { context.settingsDataStore.edit { it[SettingsKeys.RATE_PROMPT_DISMISSED] = true } }
                        showRateDialog = false
                    }) { Text("Never") }
                    TextButton(onClick = { showRateDialog = false }) { Text("Later") }
                }
            }
        )
    }

    if (showFullPlayer && playerState.currentLecture != null) {
        PlayerScreen(
            state = playerState,
            onBack = { showFullPlayer = false },
            onTogglePlayPause = playerViewModel::togglePlayPause,
            onSeek = playerViewModel::seekTo,
            onSkipForward = { playerViewModel.skipForward(playerState.skipForwardSeconds) },
            onSkipBackward = { playerViewModel.skipBackward(playerState.skipBackwardSeconds) },
            onSpeedChange = playerViewModel::setPlaybackSpeed,
            onSleepTimer = playerViewModel::setSleepTimer,
            onSleepTimerEndOfLecture = playerViewModel::setSleepTimerEndOfLecture,
            onCancelSleepTimer = playerViewModel::cancelSleepTimer,
            onCycleRepeat = playerViewModel::cycleRepeatMode,
            onAddBookmark = playerViewModel::addBookmark
        )
        return
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val onLectureClick: (Lecture) -> Unit = { lecture ->
        playerViewModel.playLecture(lecture)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column {
                // Mini player
                if (playerState.currentLecture != null) {
                    MiniPlayerBar(
                        state = playerState,
                        onTogglePlayPause = playerViewModel::togglePlayPause,
                        onClose = playerViewModel::stop,
                        onTap = { showFullPlayer = true },
                        onSpeedCycle = {
                            val speeds = listOf(1f, 1.25f, 1.5f, 1.75f, 2f)
                            val idx = speeds.indexOf(playerState.playbackSpeed)
                            playerViewModel.setPlaybackSpeed(speeds[(idx + 1) % speeds.size])
                        }
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
        Column(modifier = Modifier.padding(innerPadding)) {
            // Offline banner
            if (!isOnline) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFF3E0))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.WifiOff,
                        contentDescription = null,
                        tint = Color(0xFFE65100),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "No internet \u2014 downloaded lectures available offline",
                        fontSize = 12.sp,
                        color = Color(0xFFE65100)
                    )
                }
            }

            NavHost(
                navController = navController,
                startDestination = "home"
            ) {
                composable("home") {
                    HomeScreen(
                        onLectureClick = onLectureClick,
                        onSpeakerClick = { id -> navController.navigate("speaker/$id") },
                        onNavigateToSpeakers = { navController.navigate("speakers") },
                        onNavigateToTopics = { navController.navigate("topics") },
                        onNavigateToSeries = { navController.navigate("series") },
                        onNavigateToSearch = { navController.navigate("search") },
                        onNavigateToParasha = { navController.navigate("topic_lectures/Parasha") },
                        onAddToQueue = { lecture -> playerViewModel.addToQueue(lecture) }
                    )
                }

                composable("search") {
                    SearchScreen(
                        onBack = { navController.navigate("home") },
                        onSpeakerClick = { id -> navController.navigate("speaker/$id") },
                        onTopicClick = { topic ->
                            val encoded = URLEncoder.encode(topic.text, "UTF-8")
                            navController.navigate("topic_lectures/$encoded")
                        },
                        onLectureClick = onLectureClick
                    )
                }

                composable("library") {
                    LibraryScreen(
                        onNavigateToLogin = { navController.navigate("login") },
                        onNavigateToHistory = { navController.navigate("history") },
                        onNavigateToPlaylists = { navController.navigate("listen_later") },
                        onNavigateToListenLater = { navController.navigate("listen_later") },
                        onNavigateToFavorites = { navController.navigate("favorites") },
                        onNavigateToFollowing = { navController.navigate("following") },
                        onNavigateToDownloads = { navController.navigate("downloads") },
                        onNavigateToBookmarks = { navController.navigate("bookmarks") },
                        onNavigateToSettings = { navController.navigate("settings") },
                        onNavigateToQueue = { navController.navigate("queue") }
                    )
                }

                composable("login") {
                    LoginScreen(
                        onLoginSuccess = { navController.popBackStack() },
                        onSkip = { navController.popBackStack() }
                    )
                }

                composable("listen_later") {
                    ListenLaterScreen(
                        onBack = { navController.popBackStack() },
                        onLectureClick = onLectureClick
                    )
                }

                composable("favorites") {
                    FavoritesScreen(
                        onBack = { navController.popBackStack() },
                        onLectureClick = onLectureClick
                    )
                }

                composable("following") {
                    FollowingScreen(
                        onBack = { navController.popBackStack() },
                        onSpeakerClick = { id -> navController.navigate("speaker/$id") }
                    )
                }

                composable("history") {
                    HistoryScreen(
                        onBack = { navController.popBackStack() },
                        onLectureClick = onLectureClick
                    )
                }

                composable("downloads") {
                    DownloadsScreen(
                        onBack = { navController.popBackStack() },
                        onLectureClick = onLectureClick
                    )
                }

                composable("bookmarks") {
                    BookmarksScreen(
                        onBack = { navController.popBackStack() },
                        onLectureClick = { lecture, position ->
                            playerViewModel.playLecture(lecture)
                            // Seek to bookmark position after a brief delay for player to start
                            playerViewModel.seekTo(position)
                        }
                    )
                }

                composable("settings") {
                    SettingsScreen(
                        onBack = { navController.popBackStack() },
                        onNavigateToAbout = { navController.navigate("about") }
                    )
                }

                composable("about") {
                    AboutScreen(
                        onBack = { navController.popBackStack() }
                    )
                }

                composable("queue") {
                    QueueScreen(
                        onBack = { navController.popBackStack() },
                        onClearQueue = { playerViewModel.clearQueue() }
                    )
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
                        onTopicClick = { topic ->
                            val encoded = URLEncoder.encode(topic.text, "UTF-8")
                            navController.navigate("topic_lectures/$encoded")
                        }
                    )
                }

                composable("series") {
                    SeriesListScreen(
                        onBack = { navController.popBackStack() },
                        onSeriesClick = { id -> navController.navigate("series_detail/$id") }
                    )
                }

                composable(
                    "series_detail/{seriesId}",
                    arguments = listOf(navArgument("seriesId") { type = NavType.IntType })
                ) { backStackEntry ->
                    val seriesId = backStackEntry.arguments?.getInt("seriesId") ?: return@composable
                    SeriesDetailScreen(
                        seriesId = seriesId,
                        onBack = { navController.popBackStack() },
                        onLectureClick = onLectureClick
                    )
                }

                composable(
                    "topic_lectures/{topicName}",
                    arguments = listOf(navArgument("topicName") { type = NavType.StringType })
                ) { backStackEntry ->
                    val rawName = backStackEntry.arguments?.getString("topicName") ?: return@composable
                    val topicName = URLDecoder.decode(rawName, "UTF-8")
                    TopicLecturesScreen(
                        topicName = topicName,
                        onBack = { navController.popBackStack() },
                        onLectureClick = onLectureClick
                    )
                }
            }
        }
    }
}
