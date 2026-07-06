package com.sonora.music.ui.screens.library

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonora.music.core.model.Track
import com.sonora.music.ui.components.EmptyState
import com.sonora.music.ui.components.TrackRow

@Composable
fun LibraryScreen(
    onPlay: (Track, List<Track>) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenStats: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val liked by viewModel.likedSongs.collectAsStateWithLifecycle()
    val local by viewModel.localTracks.collectAsStateWithLifecycle()
    val localEnabled by viewModel.localSyncEnabled.collectAsStateWithLifecycle()
    val syncing by viewModel.syncing.collectAsStateWithLifecycle()
    val matching by viewModel.matching.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permission = if (Build.VERSION.SDK_INT >= 33)
        android.Manifest.permission.READ_MEDIA_AUDIO
    else android.Manifest.permission.READ_EXTERNAL_STORAGE

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) viewModel.syncLocal() }

    fun requestSync() {
        val has = ContextCompat.checkSelfPermission(context, permission) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        if (has) viewModel.syncLocal() else permissionLauncher.launch(permission)
    }

    if (liked.isEmpty() && local.isEmpty() && !localEnabled) {
        EmptyState(
            title = "Your library is empty",
            subtitle = "Like songs and they'll show up here.",
        )
        return
    }

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Text("Library", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(20.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalButton(onClick = onOpenHistory) {
                    Icon(Icons.Rounded.History, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                    Text("History")
                }
                FilledTonalButton(onClick = onOpenStats) {
                    Icon(Icons.Rounded.BarChart, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                    Text("Stats")
                }
            }
        }

        if (localEnabled) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilledTonalButton(onClick = { requestSync() }, enabled = !syncing) {
                        Icon(Icons.Rounded.Sync, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                        Text(if (syncing) "Scanning…" else "Sync device music")
                    }
                    if (local.isNotEmpty()) {
                        Button(onClick = viewModel::matchMetadata, enabled = !matching) {
                            Icon(Icons.Rounded.AutoFixHigh, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                            Text(if (matching) "Matching…" else "Match tags")
                        }
                    }
                }
            }
        }

        if (liked.isNotEmpty()) {
            item { SectionHeader("Liked songs") }
            items(liked, key = { "liked_" + it.id }) { track ->
                TrackRow(track = track, onClick = { onPlay(track, liked) })
            }
        }

        if (local.isNotEmpty()) {
            item { SectionHeader("On this device") }
            items(local, key = { "local_" + it.id }) { track ->
                TrackRow(track = track, onClick = { onPlay(track, local) })
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp),
    )
}
