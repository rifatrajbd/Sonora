package com.sonora.music.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sonora.music.core.model.Track
import com.sonora.music.ui.components.QualityChip

@Composable
fun NowPlayingScreen(
    track: Track,
    isPlaying: Boolean,
    isLiked: Boolean,
    isDownloaded: Boolean,
    hasNext: Boolean,
    hasPrevious: Boolean,
    positionMs: Long,
    repeatMode: Int,
    sleepActive: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleLike: () -> Unit,
    onToggleDownload: () -> Unit,
    onCycleRepeat: () -> Unit,
    onSetSleepTimer: (Int) -> Unit,
    onSeek: (Long) -> Unit,
    onCollapse: () -> Unit,
) {
    var showLyrics by remember { mutableStateOf(false) }
    var showSleepDialog by remember { mutableStateOf(false) }
    val accent = MaterialTheme.colorScheme.primary

    Box(Modifier.fillMaxSize()) {
        AsyncImage(
            model = track.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().blur(48.dp),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background.copy(alpha = 0.35f),
                        MaterialTheme.colorScheme.background.copy(alpha = 0.94f),
                    )
                )
            )
        )

        Column(
            Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            IconButton(onClick = onCollapse, modifier = Modifier.align(Alignment.Start)) {
                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Collapse")
            }

            Spacer(Modifier.height(8.dp))
            if (showLyrics) {
                LyricsPanel(track = track, positionMs = positionMs, onSeek = onSeek, modifier = Modifier.weight(1f))
            } else {
                AsyncImage(
                    model = track.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(24.dp)),
                )
                Spacer(Modifier.weight(1f))
            }

            Spacer(Modifier.height(16.dp))
            Text(
                track.title,
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
            )
            Text(track.artistName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            QualityChip(track.maxQuality)

            // Seek bar
            Spacer(Modifier.height(12.dp))
            val duration = track.durationMs.coerceAtLeast(1L)
            Slider(
                value = (positionMs.coerceIn(0, duration)).toFloat() / duration,
                onValueChange = { onSeek((it * duration).toLong()) },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(fmt(positionMs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(fmt(track.durationMs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Transport controls — big play button in the centre
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onCycleRepeat) {
                    Icon(
                        if (repeatMode == 2) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        contentDescription = "Repeat",
                        tint = if (repeatMode == 0) MaterialTheme.colorScheme.onSurfaceVariant else accent,
                    )
                }
                IconButton(onClick = onPrevious, enabled = hasPrevious, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(38.dp))
                }
                Surface(
                    onClick = onPlayPause,
                    shape = CircleShape,
                    color = accent,
                    modifier = Modifier.size(76.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(44.dp),
                        )
                    }
                }
                IconButton(onClick = onNext, enabled = hasNext, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = "Next", modifier = Modifier.size(38.dp))
                }
                IconButton(onClick = { showSleepDialog = true }) {
                    Icon(
                        Icons.Rounded.Bedtime,
                        contentDescription = "Sleep timer",
                        tint = if (sleepActive) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Secondary actions — below the controls, at the bottom
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { showLyrics = !showLyrics }) {
                    Icon(Icons.Rounded.Lyrics, contentDescription = "Lyrics", tint = if (showLyrics) accent else MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = onToggleDownload) {
                    Icon(
                        if (isDownloaded) Icons.Rounded.CheckCircle else Icons.Rounded.Download,
                        contentDescription = "Download",
                        tint = if (isDownloaded) accent else MaterialTheme.colorScheme.onSurface,
                    )
                }
                IconButton(onClick = onToggleLike) {
                    Icon(
                        if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) accent else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }

    if (showSleepDialog) {
        SleepTimerDialog(
            onPick = { onSetSleepTimer(it); showSleepDialog = false },
            onDismiss = { showSleepDialog = false },
        )
    }
}

@Composable
private fun SleepTimerDialog(onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sleep timer") },
        text = {
            Column {
                listOf(15, 30, 45, 60).forEach { m ->
                    TextButton(onClick = { onPick(m) }, modifier = Modifier.fillMaxWidth()) {
                        Text("$m minutes", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                    }
                }
                TextButton(onClick = { onPick(0) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Turn off", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun fmt(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}
