package com.sonora.music.ui.screens.offline

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonora.music.core.model.Track
import com.sonora.music.ui.components.EmptyState
import com.sonora.music.ui.components.TrackRow

@Composable
fun OfflineScreen(
    onPlay: (Track, List<Track>) -> Unit,
    isOnline: Boolean,
    viewModel: OfflineViewModel = hiltViewModel(),
) {
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val local by viewModel.localTracks.collectAsStateWithLifecycle()

    if (downloads.isEmpty() && local.isEmpty()) {
        EmptyState(
            title = if (isOnline) "No offline music yet" else "You're offline",
            subtitle = "Download songs or sync device music to listen without a connection.",
            icon = Icons.Rounded.CloudOff,
        )
        return
    }

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Text("Offline", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(20.dp))
            if (!isOnline) {
                Text(
                    "No connection — showing your downloaded & local music.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        }
        if (downloads.isNotEmpty()) {
            item { Header("Downloaded") }
            items(downloads, key = { "dl_" + it.id }) { TrackRow(track = it, onClick = { onPlay(it, downloads) }) }
        }
        if (local.isNotEmpty()) {
            item { Header("On this device") }
            items(local, key = { "loc_" + it.id }) { TrackRow(track = it, onClick = { onPlay(it, local) }) }
        }
    }
}

@Composable
private fun Header(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp))
}
