package com.sonora.music.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.sonora.music.core.model.HomeSection
import com.sonora.music.core.model.Track
import com.sonora.music.core.model.UiState
import com.sonora.music.ui.components.EmptyState
import com.sonora.music.ui.components.ErrorState
import com.sonora.music.ui.components.LoadingState

@Composable
fun HomeScreen(
    onPlay: (Track, List<Track>) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenExplore: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Sonora", style = MaterialTheme.typography.displayLarge)
            Row {
                IconButton(onClick = onOpenExplore) {
                    Icon(Icons.Rounded.Explore, contentDescription = "Explore")
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                }
            }
        }

        when (val s = state) {
            is UiState.Loading -> LoadingState()
            is UiState.Empty -> EmptyState(
                title = "Your feed is warming up",
                subtitle = "Configure a provider, then pull to refresh.",
            )
            is UiState.Error -> ErrorState(s.message, onRetry = viewModel::load)
            is UiState.Success -> LazyColumn(Modifier.fillMaxWidth()) {
                items(s.data, key = { it.title }) { section ->
                    HomeRow(section, onPlay)
                }
            }
        }
    }
}

@Composable
private fun HomeRow(section: HomeSection, onPlay: (Track, List<Track>) -> Unit) {
    Text(
        section.title,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 10.dp),
    )
    LazyRow(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(section.tracks, key = { it.id }) { track ->
            Column(
                Modifier.width(150.dp).clickable { onPlay(track, section.tracks) },
            ) {
                AsyncImage(
                    model = track.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(150.dp).clip(RoundedCornerShape(14.dp)),
                )
                Text(
                    track.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    track.artistName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
