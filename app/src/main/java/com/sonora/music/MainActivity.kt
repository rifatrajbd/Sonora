package com.sonora.music

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sonora.music.ui.PlayerViewModel
import com.sonora.music.ui.components.MiniPlayer
import com.sonora.music.ui.navigation.Routes
import com.sonora.music.ui.navigation.TopLevelDestination
import com.sonora.music.ui.screens.explore.ExploreScreen
import com.sonora.music.ui.screens.home.HomeScreen
import com.sonora.music.ui.screens.library.LibraryScreen
import com.sonora.music.ui.screens.offline.OfflineScreen
import com.sonora.music.ui.screens.player.NowPlayingScreen
import androidx.compose.runtime.LaunchedEffect
import com.sonora.music.ui.screens.search.SearchScreen
import androidx.compose.foundation.isSystemInDarkTheme
import com.sonora.music.data.settings.ThemeMode
import com.sonora.music.ui.screens.settings.AboutScreen
import com.sonora.music.ui.screens.settings.AppearanceScreen
import com.sonora.music.ui.screens.settings.BackupScreen
import com.sonora.music.ui.screens.settings.ContentScreen
import com.sonora.music.ui.screens.settings.PlayerSettingsScreen
import com.sonora.music.ui.screens.settings.PrivacyScreen
import com.sonora.music.ui.screens.settings.SettingsScreen
import com.sonora.music.ui.screens.settings.SettingsViewModel
import com.sonora.music.ui.screens.settings.SourcesScreen
import com.sonora.music.ui.screens.settings.StorageScreen
import com.sonora.music.ui.theme.SonoraTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val settingsVm: SettingsViewModel = hiltViewModel()
            val settings by settingsVm.settings.collectAsStateWithLifecycle()
            val darkTheme = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
            }
            // Privacy: block screenshots / screen recording when enabled.
            LaunchedEffect(settings.disableScreenshot) {
                if (settings.disableScreenshot) {
                    window.setFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE, android.view.WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
            SonoraTheme(
                darkTheme = darkTheme,
                dynamicColor = settings.dynamicColor,
                amoled = settings.pureBlack,
            ) {
                SonoraRoot(startRoute = defaultRoute(settings.defaultTab))
            }
        }
    }
}

private fun defaultRoute(tab: com.sonora.music.data.settings.DefaultTab) = when (tab) {
    com.sonora.music.data.settings.DefaultTab.HOME -> TopLevelDestination.HOME.route
    com.sonora.music.data.settings.DefaultTab.SEARCH -> TopLevelDestination.SEARCH.route
    com.sonora.music.data.settings.DefaultTab.LIBRARY -> TopLevelDestination.LIBRARY.route
    com.sonora.music.data.settings.DefaultTab.OFFLINE -> TopLevelDestination.OFFLINE.route
}

@Composable
private fun SonoraRoot(startRoute: String, player: PlayerViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val currentTrack by player.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by player.isPlaying.collectAsStateWithLifecycle()
    val isLiked by player.currentIsLiked.collectAsStateWithLifecycle()
    val isDownloaded by player.currentIsDownloaded.collectAsStateWithLifecycle()
    val isDownloading by player.currentIsDownloading.collectAsStateWithLifecycle()
    val hasNext by player.hasNext.collectAsStateWithLifecycle()
    val hasPrevious by player.hasPrevious.collectAsStateWithLifecycle()
    val positionMs by player.positionMs.collectAsStateWithLifecycle()
    val repeatMode by player.repeatMode.collectAsStateWithLifecycle()
    val sleepEndMs by player.sleepTimerEndMs.collectAsStateWithLifecycle()
    val isOnline by player.isOnline.collectAsStateWithLifecycle()
    val queue by player.queue.collectAsStateWithLifecycle()
    val currentIndex by player.currentIndex.collectAsStateWithLifecycle()
    var showNowPlaying by remember { mutableStateOf(false) }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isTopLevel = TopLevelDestination.entries.any { it.route == currentRoute }

    // Auto-switch to the Offline tab when the connection drops.
    LaunchedEffect(isOnline) {
        if (!isOnline && currentRoute != TopLevelDestination.OFFLINE.route) {
            navController.navigate(TopLevelDestination.OFFLINE.route) {
                popUpTo(TopLevelDestination.HOME.route)
                launchSingleTop = true
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
    Scaffold(
        bottomBar = {
            if (isTopLevel) {
                NavigationBar {
                    TopLevelDestination.entries.forEach { dest ->
                        NavigationBarItem(
                            selected = currentRoute == dest.route,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(TopLevelDestination.HOME.route)
                                    launchSingleTop = true
                                }
                            },
                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                            label = { Text(dest.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            NavHost(navController = navController, startDestination = startRoute) {
                composable(TopLevelDestination.HOME.route) {
                    HomeScreen(
                        onPlay = { track, queue -> player.play(track, queue) },
                        onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                        onOpenExplore = { navController.navigate(Routes.EXPLORE) },
                    )
                }
                composable(TopLevelDestination.SEARCH.route) {
                    SearchScreen(onPlay = { track, queue -> player.play(track, queue) })
                }
                composable(TopLevelDestination.LIBRARY.route) {
                    LibraryScreen(
                        onPlay = { track, q -> player.play(track, q) },
                        onOpenHistory = { navController.navigate(Routes.HISTORY) },
                        onOpenStats = { navController.navigate(Routes.STATS) },
                    )
                }
                composable(TopLevelDestination.OFFLINE.route) {
                    OfflineScreen(onPlay = { track, queue -> player.play(track, queue) }, isOnline = isOnline)
                }
                composable(Routes.EXPLORE) {
                    ExploreScreen(
                        onPlay = { track, queue -> player.play(track, queue) },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        onBack = { navController.popBackStack() },
                        onOpenAppearance = { navController.navigate(Routes.APPEARANCE) },
                        onOpenContent = { navController.navigate(Routes.CONTENT) },
                        onOpenPlayer = { navController.navigate(Routes.PLAYER_SETTINGS) },
                        onOpenStorage = { navController.navigate(Routes.STORAGE) },
                        onOpenPrivacy = { navController.navigate(Routes.PRIVACY) },
                        onOpenSources = { navController.navigate(Routes.SOURCES) },
                        onOpenBackup = { navController.navigate(Routes.BACKUP) },
                        onOpenAbout = { navController.navigate(Routes.ABOUT) },
                    )
                }
                composable(Routes.ABOUT) { AboutScreen(onBack = { navController.popBackStack() }) }
                composable(Routes.SOURCES) { SourcesScreen(onBack = { navController.popBackStack() }) }
                composable(Routes.APPEARANCE) { AppearanceScreen(onBack = { navController.popBackStack() }) }
                composable(Routes.CONTENT) { ContentScreen(onBack = { navController.popBackStack() }) }
                composable(Routes.PLAYER_SETTINGS) { PlayerSettingsScreen(onBack = { navController.popBackStack() }) }
                composable(Routes.STORAGE) { StorageScreen(onBack = { navController.popBackStack() }) }
                composable(Routes.PRIVACY) { PrivacyScreen(onBack = { navController.popBackStack() }) }
                composable(Routes.BACKUP) { BackupScreen(onBack = { navController.popBackStack() }) }
                composable(Routes.HISTORY) {
                    com.sonora.music.ui.screens.stats.HistoryScreen(
                        onPlay = { track, q -> player.play(track, q) },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Routes.STATS) {
                    com.sonora.music.ui.screens.stats.StatsScreen(
                        onPlay = { track, q -> player.play(track, q) },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Routes.ARTIST) {
                    com.sonora.music.ui.screens.detail.DetailScreen(
                        isArtist = true,
                        onPlay = { track, q -> player.play(track, q) },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Routes.ALBUM) {
                    com.sonora.music.ui.screens.detail.DetailScreen(
                        isArtist = false,
                        onPlay = { track, q -> player.play(track, q) },
                        onBack = { navController.popBackStack() },
                    )
                }
            }

            // Mini-player docked above the bottom nav
            currentTrack?.let { track ->
                if (isTopLevel && !showNowPlaying) {
                    MiniPlayer(
                        track = track,
                        isPlaying = isPlaying,
                        onPlayPause = player::togglePlayPause,
                        onExpand = { showNowPlaying = true },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }

    // Full-screen Now Playing overlay (expands from mini-player)
    AnimatedVisibility(
        visible = showNowPlaying && currentTrack != null,
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
    ) {
        currentTrack?.let { track ->
            NowPlayingScreen(
                track = track,
                isPlaying = isPlaying,
                isLiked = isLiked,
                isDownloaded = isDownloaded,
                isDownloading = isDownloading,
                hasNext = hasNext,
                hasPrevious = hasPrevious,
                positionMs = positionMs,
                repeatMode = repeatMode,
                sleepActive = sleepEndMs != null,
                queue = queue,
                currentIndex = currentIndex,
                onJumpTo = player::jumpTo,
                onPlayPause = player::togglePlayPause,
                onNext = player::next,
                onPrevious = player::previous,
                onToggleLike = player::toggleLikeCurrent,
                onToggleDownload = player::toggleDownloadCurrent,
                onCycleRepeat = player::cycleRepeatMode,
                onSetSleepTimer = player::setSleepTimer,
                onSeek = player::seekTo,
                onOpenArtist = { name -> navController.navigate(Routes.artist(name)) },
                onCollapse = { showNowPlaying = false },
            )
        }
    }
    }
}
