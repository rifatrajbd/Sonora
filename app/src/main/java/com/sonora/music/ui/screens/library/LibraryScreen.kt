package com.sonora.music.ui.screens.library

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
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
fun LibraryScreen(
    onPlay: (Track, List<Track>) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val liked by viewModel.likedSongs.collectAsStateWithLifecycle()

    if (liked.isEmpty()) {
        EmptyState(
            title = "Your library is empty",
            subtitle = "Like songs and they'll show up here.",
            icon = Icons.Rounded.Favorite,
        )
        return
    }

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Text(
                "Liked songs",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(20.dp),
            )
        }
        items(liked, key = { it.id }) { track ->
            TrackRow(track = track, onClick = { onPlay(track, liked) })
        }
    }
}
