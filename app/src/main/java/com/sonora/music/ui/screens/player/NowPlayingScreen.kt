package com.sonora.music.ui.screens.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
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
    isDownloading: Boolean,
    hasNext: Boolean,
    hasPrevious: Boolean,
    positionMs: Long,
    repeatMode: Int,
    sleepActive: Boolean,
    queue: List<Track>,
    currentIndex: Int,
    onJumpTo: (Int) -> Unit,
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
    var showQueue by remember { mutableStateOf(false) }
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
            // Tap the artwork to flip it over and reveal the lyrics (and back).
            val rotation by animateFloatAsState(if (showLyrics) 180f else 0f, tween(500), label = "flip")
            val density = LocalDensity.current
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .graphicsLayer {
                        rotationY = rotation
                        cameraDistance = 14f * density.density
                    }
                    .clip(RoundedCornerShape(24.dp))
                    .clickable { showLyrics = !showLyrics },
            ) {
                if (rotation <= 90f) {
                    AsyncImage(
                        model = track.thumbnailUrl,
                        contentDescription = "Tap for lyrics",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    // Back face — counter-rotate so text isn't mirrored.
                    Box(
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer { rotationY = 180f }
                            .background(MaterialTheme.colorScheme.surface),
                    ) {
                        LyricsPanel(track = track, positionMs = positionMs, onSeek = onSeek, modifier = Modifier.fillMaxSize())
                    }
                }
            }
            Spacer(Modifier.weight(1f))

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
                IconButton(onClick = onToggleDownload, enabled = !isDownloading) {
                    if (isDownloading) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = accent,
                        )
                    } else {
                        Icon(
                            if (isDownloaded) Icons.Rounded.CheckCircle else Icons.Rounded.Download,
                            contentDescription = if (isDownloaded) "Downloaded" else "Download",
                            tint = if (isDownloaded) accent else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                IconButton(onClick = onToggleLike) {
                    Icon(
                        if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) accent else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            // Swipe up (or tap) this handle to reveal the queue / playlist.
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { showQueue = true }
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { _, dragAmount ->
                            if (dragAmount < -8f) showQueue = true
                        }
                    }
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Up next", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))
            }
        }
    }

    if (showQueue) {
        QueueSheet(
            queue = queue,
            currentIndex = currentIndex,
            onJumpTo = { onJumpTo(it); showQueue = false },
            onDismiss = { showQueue = false },
        )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QueueSheet(
    queue: List<Track>,
    currentIndex: Int,
    onJumpTo: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            "Up next",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 20.dp, bottom = 8.dp),
        )
        LazyColumn(Modifier.fillMaxWidth()) {
            itemsIndexed(queue, key = { i, t -> "$i-${t.id}" }) { i, t ->
                Row(
                    Modifier.fillMaxWidth().clickable { onJumpTo(i) }.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    coil.compose.AsyncImage(
                        model = t.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(6.dp)),
                    )
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(
                            t.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (i == currentIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        Text(t.artistName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (i == currentIndex) {
                        Icon(Icons.Rounded.QueueMusic, contentDescription = "Now playing", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
