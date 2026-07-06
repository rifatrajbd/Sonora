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
    const val APPEARANCE = "appearance"
    const val CONTENT = "content"
    const val PLAYER_SETTINGS = "player_settings"
    const val STORAGE = "storage"
    const val PRIVACY = "privacy"
    const val BACKUP = "backup"
    const val HISTORY = "history"
    const val STATS = "stats"
    const val ARTIST = "artist/{name}"
    const val ALBUM = "album/{name}"

    fun artist(name: String) = "artist/" + java.net.URLEncoder.encode(name, "UTF-8")
    fun album(name: String) = "album/" + java.net.URLEncoder.encode(name, "UTF-8")
}
