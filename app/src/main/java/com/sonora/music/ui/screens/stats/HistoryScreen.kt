package com.sonora.music.ui.screens.stats

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonora.music.core.model.Track
import com.sonora.music.ui.components.EmptyState
import com.sonora.music.ui.components.TrackRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onPlay: (Track, List<Track>) -> Unit,
    onBack: () -> Unit,
    viewModel: HistoryStatsViewModel = hiltViewModel(),
) {
    val recent by viewModel.recentlyPlayed.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = viewModel::clearHistory) { Icon(Icons.Rounded.DeleteSweep, contentDescription = "Clear history") }
                },
            )
        },
    ) { padding ->
        if (recent.isEmpty()) {
            EmptyState(title = "No history yet", subtitle = "Songs you play will show up here.", icon = Icons.Rounded.History, modifier = Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            items(recent, key = { it.id }) { track ->
                TrackRow(track = track, onClick = { onPlay(track, recent) })
            }
        }
    }
}
