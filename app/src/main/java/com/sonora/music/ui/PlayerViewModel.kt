package com.sonora.music.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonora.music.core.model.Track
import com.sonora.music.data.repository.MusicRepository
import com.sonora.music.playback.PlayerConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerConnection: PlayerConnection,
    private val repository: MusicRepository,
    networkMonitor: com.sonora.music.core.NetworkMonitor,
) : ViewModel() {

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
    val currentTrack: StateFlow<Track?> = playerConnection.currentTrack
    val isPlaying: StateFlow<Boolean> = playerConnection.isPlaying
    val hasNext: StateFlow<Boolean> = playerConnection.hasNext
    val hasPrevious: StateFlow<Boolean> = playerConnection.hasPrevious
    val positionMs: StateFlow<Long> = playerConnection.positionMs
    val repeatMode: StateFlow<Int> = playerConnection.repeatMode
    val sleepTimerEndMs: StateFlow<Long?> = playerConnection.sleepTimerEndMs

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val currentIsLiked: StateFlow<Boolean> = currentTrack
        .flatMapLatest { track -> track?.let { repository.isLiked(it.id) } ?: flowOf(false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val currentIsDownloaded: StateFlow<Boolean> = currentTrack
        .flatMapLatest { track -> track?.let { repository.isDownloaded(it.id) } ?: flowOf(false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val currentIsDownloading: StateFlow<Boolean> = kotlinx.coroutines.flow.combine(
        currentTrack, repository.downloadsInProgress,
    ) { track, inProgress -> track != null && track.id in inProgress }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        playerConnection.connect()
    }

    fun play(track: Track, queue: List<Track> = listOf(track)) {
        val start = queue.indexOfFirst { it.id == track.id }.takeIf { it >= 0 } ?: 0
        playerConnection.setQueue(queue, start)
    }

    fun togglePlayPause() = playerConnection.togglePlayPause()
    fun next() = playerConnection.next()
    fun previous() = playerConnection.previous()
    fun seekTo(ms: Long) = playerConnection.seekTo(ms)
    fun cycleRepeatMode() = playerConnection.cycleRepeatMode()
    fun setSleepTimer(minutes: Int) = playerConnection.setSleepTimer(minutes)

    fun toggleLikeCurrent() {
        val track = currentTrack.value ?: return
        viewModelScope.launch { repository.toggleLike(track) }
    }

    fun toggleDownloadCurrent() {
        val track = currentTrack.value ?: return
        viewModelScope.launch { repository.toggleDownload(track) }
    }
}
