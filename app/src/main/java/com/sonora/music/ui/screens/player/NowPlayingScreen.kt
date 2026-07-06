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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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

/**
 * Signature screen: full-bleed blurred artwork → floating art card → (frosted) control bar with
 * quality + source chips. A lyrics toggle swaps the artwork for the live synced-lyrics panel;
 * a download toggle saves the track for offline.
 */
@Composable
fun NowPlayingScreen(
    track: Track,
    isPlaying: Boolean,
    isLiked: Boolean,
    isDownloaded: Boolean,
    hasNext: Boolean,
    hasPrevious: Boolean,
    positionMs: Long,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleLike: () -> Unit,
    onToggleDownload: () -> Unit,
    onSeek: (Long) -> Unit,
    onCollapse: () -> Unit,
) {
    var showLyrics by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        AsyncImage(
            model = track.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().blur(48.dp),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.background.copy(alpha = 0.35f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.92f),
                        )
                    )
                )
        )

        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onCollapse) {
                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Collapse")
                }
                Row {
                    IconButton(onClick = { showLyrics = !showLyrics }) {
                        Icon(
                            Icons.Rounded.Lyrics,
                            contentDescription = "Lyrics",
                            tint = if (showLyrics) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    IconButton(onClick = onToggleDownload) {
                        Icon(
                            if (isDownloaded) Icons.Rounded.CheckCircle else Icons.Rounded.Download,
                            contentDescription = if (isDownloaded) "Downloaded" else "Download",
                            tint = if (isDownloaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    IconButton(onClick = onToggleLike) {
                        Icon(
                            if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = if (isLiked) "Unlike" else "Like",
                            tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (showLyrics) {
                LyricsPanel(
                    track = track,
                    positionMs = positionMs,
                    onSeek = onSeek,
                    modifier = Modifier.weight(1f),
                )
            } else {
                AsyncImage(
                    model = track.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(24.dp)),
                )
                Spacer(Modifier.weight(1f))
            }

            Spacer(Modifier.height(20.dp))
            Text(
                track.title,
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Text(
                track.artistName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            QualityChip(track.maxQuality)

            Spacer(Modifier.height(20.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onPrevious, enabled = hasPrevious) {
                    Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous")
                }
                IconButton(onClick = onPlayPause) {
                    Icon(
                        if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.padding(4.dp),
                    )
                }
                IconButton(onClick = onNext, enabled = hasNext) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = "Next")
                }
            }
        }
    }
}
