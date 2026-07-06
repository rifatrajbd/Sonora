package com.sonora.music.ui.screens.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonora.music.core.model.Lyrics
import com.sonora.music.core.model.Track
import com.sonora.music.core.model.UiState

/**
 * Synced-lyrics panel. Highlights the active line based on [positionMs] and keeps it scrolled to
 * center. Tapping a synced line seeks to it. Falls back to plain scrollable text, or a tidy empty
 * state when no lyrics exist.
 */
@Composable
fun LyricsPanel(
    track: Track,
    positionMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LyricsViewModel = hiltViewModel(),
) {
    LaunchedEffect(track.id) { viewModel.load(track) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(modifier.fillMaxWidth()) {
        when (val s = state) {
            is UiState.Loading -> CenterText("Finding lyrics…")
            is UiState.Empty -> CenterText("No lyrics available")
            is UiState.Error -> CenterText("Couldn't load lyrics")
            is UiState.Success -> LyricsList(s.data, positionMs, onSeek)
        }
    }
}

@Composable
private fun LyricsList(lyrics: Lyrics, positionMs: Long, onSeek: (Long) -> Unit) {
    if (!lyrics.synced) {
        LazyColumn(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            itemsIndexed(lyrics.lines) { _, line ->
                Text(
                    line.text,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
        }
        return
    }

    val lines = lyrics.lines
    val activeIndex = remember(positionMs, lines) {
        lines.indexOfLast { it.startMs <= positionMs }.coerceAtLeast(0)
    }
    val listState = rememberLazyListState()
    LaunchedEffect(activeIndex) {
        if (lines.isNotEmpty()) listState.animateScrollToItem(activeIndex.coerceAtMost(lines.lastIndex))
    }

    LazyColumn(state = listState, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        itemsIndexed(lines) { index, line ->
            val active = index == activeIndex
            Text(
                text = line.text.ifBlank { "♪" },
                textAlign = TextAlign.Center,
                fontSize = if (active) 22.sp else 18.sp,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                color = if (active) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSeek(line.startMs) }
                    .padding(vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun CenterText(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(24.dp),
    )
}
