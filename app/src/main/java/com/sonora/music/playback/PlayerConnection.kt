package com.sonora.music.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.sonora.music.core.model.AudioQuality
import com.sonora.music.core.model.Track
import com.sonora.music.data.repository.MusicRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-side bridge to [PlaybackService]. Owns the logical play queue (a list of [Track]) and the
 * current index. Because each source resolves its stream URL on demand (and may fail over to
 * another source), items are resolved just-in-time as they start rather than all up front.
 */
@Singleton
class PlayerConnection @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: MusicRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var controller: MediaController? = null

    private val queue = mutableListOf<Track>()
    private var index = -1

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _hasNext = MutableStateFlow(false)
    val hasNext: StateFlow<Boolean> = _hasNext.asStateFlow()

    private val _hasPrevious = MutableStateFlow(false)
    val hasPrevious: StateFlow<Boolean> = _hasPrevious.asStateFlow()

    private val _preferredQuality = MutableStateFlow(AudioQuality.LOSSLESS)
    val preferredQuality: StateFlow<AudioQuality> = _preferredQuality.asStateFlow()

    fun connect() {
        if (controller != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            controller = future.get().also { c ->
                c.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(playing: Boolean) {
                        _isPlaying.value = playing
                    }

                    override fun onPlaybackStateChanged(state: Int) {
                        // Auto-advance when the current stream finishes.
                        if (state == Player.STATE_ENDED) next()
                    }
                })
            }
        }, MoreExecutors.directExecutor())
    }

    /** Replace the queue and start playing at [startIndex]. */
    fun setQueue(tracks: List<Track>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        queue.clear()
        queue.addAll(tracks)
        playIndex(startIndex.coerceIn(0, queue.lastIndex))
    }

    /** Convenience: play a single track as a one-item queue. */
    fun play(track: Track) = setQueue(listOf(track), 0)

    fun next() {
        if (index < queue.lastIndex) playIndex(index + 1)
    }

    fun previous() {
        // Restart current track if we're past the first few seconds, else go back.
        val position = controller?.currentPosition ?: 0
        if (position > 3_000 || index == 0) {
            controller?.seekTo(0)
        } else if (index > 0) {
            playIndex(index - 1)
        }
    }

    fun togglePlayPause() {
        controller?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun setPreferredQuality(quality: AudioQuality) { _preferredQuality.value = quality }

    private fun playIndex(target: Int) {
        index = target
        val track = queue[target]
        _currentTrack.value = track
        updateNavState()

        scope.launch {
            val stream = runCatching {
                withContext(Dispatchers.IO) { repository.resolveStream(track, _preferredQuality.value) }
            }.getOrElse {
                // Couldn't resolve this track from any source; skip to the next one.
                next()
                return@launch
            }

            val item = MediaItem.Builder()
                .setUri(stream.url)
                .setMediaId(track.id)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.artistName)
                        .setAlbumTitle(track.albumTitle)
                        .setArtworkUri(track.thumbnailUrl?.let(android.net.Uri::parse))
                        .build()
                )
                .build()

            controller?.apply {
                setMediaItem(item)
                prepare()
                play()
            }
        }
    }

    private fun updateNavState() {
        _hasNext.value = index < queue.lastIndex
        _hasPrevious.value = index > 0
    }

    fun release() {
        controller?.release()
        controller = null
    }
}
