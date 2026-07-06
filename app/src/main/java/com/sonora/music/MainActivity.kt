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
import com.sonora.music.ui.screens.settings.AudioQualityScreen
import com.sonora.music.ui.screens.settings.SettingsScreen
import com.sonora.music.ui.screens.settings.SettingsViewModel
import com.sonora.music.ui.screens.settings.SourcesScreen
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
            SonoraTheme(
                darkTheme = darkTheme,
                dynamicColor = settings.dynamicColor,
                amoled = settings.pureBlack,
            ) {
                SonoraRoot()
            }
        }
    }
}

@Composable
private fun SonoraRoot(player: PlayerViewModel = hiltViewModel()) {
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
            NavHost(navController = navController, startDestination = TopLevelDestination.HOME.route) {
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
                    LibraryScreen(onPlay = { track, queue -> player.play(track, queue) })
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
                        onOpenAbout = { navController.navigate(Routes.ABOUT) },
                        onOpenSources = { navController.navigate(Routes.SOURCES) },
                        onOpenQuality = { navController.navigate(Routes.QUALITY) },
                        onOpenAppearance = { navController.navigate(Routes.APPEARANCE) },
                    )
                }
                composable(Routes.ABOUT) {
                    AboutScreen(onBack = { navController.popBackStack() })
                }
                composable(Routes.SOURCES) {
                    SourcesScreen(onBack = { navController.popBackStack() })
                }
                composable(Routes.QUALITY) {
                    AudioQualityScreen(onBack = { navController.popBackStack() })
                }
                composable(Routes.APPEARANCE) {
                    AppearanceScreen(onBack = { navController.popBackStack() })
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
                onPlayPause = player::togglePlayPause,
                onNext = player::next,
                onPrevious = player::previous,
                onToggleLike = player::toggleLikeCurrent,
                onToggleDownload = player::toggleDownloadCurrent,
                onCycleRepeat = player::cycleRepeatMode,
                onSetSleepTimer = player::setSleepTimer,
                onSeek = player::seekTo,
                onCollapse = { showNowPlaying = false },
            )
        }
    }
    }
}
