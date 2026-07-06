package com.sonora.music.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.ui.graphics.vector.ImageVector

/** Top-level tabs shown in the bottom navigation bar. */
enum class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    HOME("home", "Home", Icons.Rounded.Home),
    SEARCH("search", "Search", Icons.Rounded.Search),
    LIBRARY("library", "Library", Icons.Rounded.LibraryMusic),
    OFFLINE("offline", "Offline", Icons.Rounded.DownloadForOffline),
}

object Routes {
    const val NOW_PLAYING = "now_playing"
    const val EXPLORE = "explore"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
    const val SOURCES = "sources"
    const val QUALITY = "quality"
    const val APPEARANCE = "appearance"
}
