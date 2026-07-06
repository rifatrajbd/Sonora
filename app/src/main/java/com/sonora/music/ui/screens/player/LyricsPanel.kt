package com.sonora.music.ui.screens.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonora.music.core.model.Lyrics
import com.sonora.music.core.model.Track
import com.sonora.music.core.model.UiState
import com.sonora.music.data.settings.LyricsPosition

/**
 * Synced-lyrics panel, Apple-Music style: the current line sits fixed in the middle with two past
 * lines above and two upcoming lines below, fading out with distance. Tapping any line seeks to
 * it. Falls back to plain scrollable text, or a tidy empty state when no lyrics exist.
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

    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        when (val s = state) {
            is UiState.Loading -> CenterText("Finding lyrics…")
            is UiState.Empty -> CenterText("No lyrics available")
            is UiState.Error -> CenterText("Couldn't load lyrics")
            is UiState.Success -> LyricsList(s.data, positionMs, onSeek)
        }
    }
}

@Composable
private fun LyricsList(lyrics: Lyrics, positionMs: Long, onSeek: (Long) -> Unit, viewModel: LyricsViewModel = hiltViewModel()) {
    val align = when (viewModel.lyricsPosition) {
        LyricsPosition.LEFT -> TextAlign.Start
        LyricsPosition.RIGHT -> TextAlign.End
        else -> TextAlign.Center
    }
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

    // Fixed five-line window: two past, the current line dead-centre, two upcoming.
    Column(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        for (offset in -2..2) {
            val index = activeIndex + offset
            val line = lines.getOrNull(index)
            val active = offset == 0
            // Fade with distance from the current line; blank slots keep the layout stable.
            val targetAlpha = when {
                line == null -> 0f
                active -> 1f
                kotlin.math.abs(offset) == 1 -> 0.55f
                else -> 0.3f
            }
            val alpha by animateFloatAsState(targetAlpha, tween(300), label = "lyricAlpha")
            Text(
                text = line?.text?.ifBlank { "♪" } ?: "",
                textAlign = align,
                fontSize = if (active) 22.sp else 17.sp,
                lineHeight = if (active) 28.sp else 22.sp,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                maxLines = 2,
                color = if (active) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(alpha)
                    .then(
                        if (line != null) Modifier.clickable { onSeek(line.startMs) } else Modifier
                    )
                    .padding(vertical = 6.dp),
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
