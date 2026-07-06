package com.sonora.music.ui.screens.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.sonora.music.core.model.Track
import com.sonora.music.core.model.UiState
import com.sonora.music.ui.components.EmptyState
import com.sonora.music.ui.components.ErrorState
import com.sonora.music.ui.components.LoadingState
import com.sonora.music.ui.components.TrackRow

/** Shared detail screen for an artist (round art) or an album (square art). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    isArtist: Boolean,
    onPlay: (Track, List<Track>) -> Unit,
    onBack: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isArtist) "Artist" else "Album") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { padding ->
        when (val s = state) {
            is UiState.Loading -> Box(Modifier.fillMaxSize().padding(padding)) { LoadingState() }
            is UiState.Empty -> EmptyState(title = "Nothing found", subtitle = viewModel.title, modifier = Modifier.padding(padding))
            is UiState.Error -> ErrorState(s.message, onRetry = viewModel::load, modifier = Modifier.padding(padding))
            is UiState.Success -> {
                val tracks = s.data
                LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                    item {
                        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            AsyncImage(
                                model = tracks.firstOrNull()?.thumbnailUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(180.dp).clip(if (isArtist) CircleShape else RoundedCornerShape(20.dp)),
                            )
                            Text(
                                viewModel.title,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 16.dp),
                            )
                            Text("${tracks.size} songs", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            androidx.compose.foundation.layout.Row(
                                Modifier.padding(top = 16.dp),
                                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
                            ) {
                                Button(onClick = { tracks.firstOrNull()?.let { onPlay(it, tracks) } }) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.padding(end = 6.dp)); Text("Play")
                                }
                                FilledTonalButton(onClick = { tracks.shuffled().let { s2 -> s2.firstOrNull()?.let { onPlay(it, s2) } } }) {
                                    Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.padding(end = 6.dp)); Text("Shuffle")
                                }
                            }
                        }
                    }
                    items(tracks, key = { it.id }) { track -> TrackRow(track = track, onClick = { onPlay(track, tracks) }) }
                }
            }
        }
    }
}
