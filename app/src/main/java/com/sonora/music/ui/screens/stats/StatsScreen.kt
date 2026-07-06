package com.sonora.music.ui.screens.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonora.music.core.model.Track
import com.sonora.music.ui.components.TrackRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onPlay: (Track, List<Track>) -> Unit,
    onBack: () -> Unit,
    viewModel: HistoryStatsViewModel = hiltViewModel(),
) {
    val topSongs by viewModel.topSongs.collectAsStateWithLifecycle()
    val topArtists by viewModel.topArtists.collectAsStateWithLifecycle()
    val totalPlays by viewModel.totalPlays.collectAsStateWithLifecycle()
    val totalMs by viewModel.totalListenedMs.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stats") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("Plays", totalPlays.toString(), Modifier.weight(1f))
                    StatCard("Listening", formatHours(totalMs), Modifier.weight(1f))
                }
            }
            if (topArtists.isNotEmpty()) {
                item { Header("Top artists") }
                items(topArtists.take(10), key = { it.artistName }) { a ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(a.artistName, style = MaterialTheme.typography.titleMedium)
                        Text("${a.plays} plays", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (topSongs.isNotEmpty()) {
                item { Header("Top songs") }
                items(topSongs, key = { it.id }) { track ->
                    TrackRow(track = track, onClick = { onPlay(track, topSongs) })
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Header(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp))
}

private fun formatHours(ms: Long): String {
    val minutes = ms / 60000
    return if (minutes < 60) "${minutes}m" else "%dh %dm".format(minutes / 60, minutes % 60)
}
