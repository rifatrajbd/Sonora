package com.sonora.music.ui.screens.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonora.music.R
import com.sonora.music.core.model.SourceType
import com.sonora.music.core.model.Track
import com.sonora.music.core.model.UiState
import com.sonora.music.ui.components.EmptyState
import com.sonora.music.ui.components.ErrorState
import com.sonora.music.ui.components.LoadingState
import com.sonora.music.ui.components.TrackRow

@Composable
fun SearchScreen(
    onPlay: (Track, List<Track>) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) } // 0 = All, 1 = Songs

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::onQueryChange,
            placeholder = { Text(stringResource(R.string.search_hint)) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )

        if (state is UiState.Success) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("All") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Songs") })
            }
        }

        when (val s = state) {
            is UiState.Empty -> EmptyState(
                title = stringResource(R.string.search_empty),
                subtitle = "Find songs, artists and albums",
                icon = Icons.Rounded.Search,
            )
            is UiState.Loading -> LoadingState()
            is UiState.Error -> ErrorState(s.message, onRetry = viewModel::retry)
            is UiState.Success -> {
                // "Songs" = original audio providers first; "Videos" = YouTube-to-music.
                val songs = s.data.tracks.filter { it.source != SourceType.YOUTUBE_MUSIC }
                val videos = s.data.tracks.filter { it.source == SourceType.YOUTUBE_MUSIC }
                val all = songs + videos

                LazyColumn(Modifier.fillMaxSize()) {
                    if (tab == 1) {
                        // Songs tab — original audio only, never YouTube videos.
                        section("Songs", songs, onPlay, songs)
                        if (songs.isEmpty()) item {
                            EmptyState(title = "No songs found", subtitle = "Try the All tab for more results.")
                        }
                    } else {
                        // All tab — Songs first, then Videos.
                        section("Songs", songs, onPlay, all)
                        section("Videos", videos, onPlay, all)
                    }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.section(
    title: String,
    tracks: List<Track>,
    onPlay: (Track, List<Track>) -> Unit,
    queue: List<Track>,
) {
    if (tracks.isEmpty()) return
    item(key = "hdr_$title") {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 6.dp),
        )
    }
    items(tracks, key = { "${title}_${it.id}" }) { track ->
        TrackRow(track = track, onClick = { onPlay(track, queue) })
    }
}
