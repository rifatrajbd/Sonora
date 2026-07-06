package com.sonora.music.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sonora.music.ui.components.EmptyState

/**
 * Home feed — quick picks / recently played / new releases. Skeleton for now; wire these
 * sections to per-source "home"/"charts" endpoints (or a merged feed) in v1.
 */
@Composable
fun HomeScreen(onOpenSettings: () -> Unit) {
    LazyColumn(Modifier.fillMaxWidth()) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Sonora", style = MaterialTheme.typography.displayLarge)
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                }
            }
        }
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text("Good evening", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Your quick picks will appear here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            EmptyState(
                title = "Nothing to show yet",
                subtitle = "Search to start playing across your 5 sources.",
                modifier = Modifier.padding(top = 48.dp),
            )
        }
    }
}
