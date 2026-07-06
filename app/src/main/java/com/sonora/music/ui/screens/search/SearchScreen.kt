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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonora.music.R
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

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::onQueryChange,
            placeholder = { Text(stringResource(R.string.search_hint)) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )

        when (val s = state) {
            is UiState.Empty -> EmptyState(
                title = stringResource(R.string.search_empty),
                subtitle = "Find songs, artists and albums",
                icon = Icons.Rounded.Search,
            )
            is UiState.Loading -> LoadingState()
            is UiState.Error -> ErrorState(s.message, onRetry = viewModel::retry)
            is UiState.Success -> LazyColumn(Modifier.fillMaxSize()) {
                val tracks = s.data.tracks
                items(tracks, key = { it.id }) { track ->
                    TrackRow(track = track, onClick = { onPlay(track, tracks) })
                }
            }
        }
    }
}
