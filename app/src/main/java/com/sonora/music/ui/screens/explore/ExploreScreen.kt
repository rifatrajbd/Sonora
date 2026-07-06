package com.sonora.music.ui.screens.explore

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonora.music.core.model.Track
import com.sonora.music.core.model.UiState
import com.sonora.music.ui.components.EmptyState
import com.sonora.music.ui.components.ErrorState
import com.sonora.music.ui.components.LoadingState
import com.sonora.music.ui.components.TrackRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    onPlay: (Track, List<Track>) -> Unit,
    onBack: () -> Unit,
    viewModel: ExploreViewModel = hiltViewModel(),
) {
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Explore") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text(
                "Browse by mood & genre",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            LazyHorizontalGrid(
                rows = GridCells.Fixed(2),
                modifier = Modifier.fillMaxWidth().height(120.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
            ) {
                items(ExploreViewModel.CATEGORIES) { (label, query) ->
                    FilterChip(
                        selected = selected == label,
                        onClick = { viewModel.select(label, query) },
                        label = { Text(label) },
                    )
                }
            }

            Box(Modifier.fillMaxSize()) {
                when (val s = state) {
                    is UiState.Empty -> if (selected == null) {
                        EmptyState(title = "Pick a mood", subtitle = "Tap a chip to explore.")
                    } else {
                        EmptyState(title = "Nothing found")
                    }
                    is UiState.Loading -> LoadingState()
                    is UiState.Error -> ErrorState(s.message)
                    is UiState.Success -> LazyColumn(Modifier.fillMaxSize()) {
                        val tracks = s.data.tracks
                        items(tracks, key = { it.id }) { track ->
                            TrackRow(track = track, onClick = { onPlay(track, tracks) })
                        }
                    }
                }
            }
        }
    }
}
