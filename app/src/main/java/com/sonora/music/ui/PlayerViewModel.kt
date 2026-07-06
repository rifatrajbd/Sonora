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
) : ViewModel() {

    val currentTrack: StateFlow<Track?> = playerConnection.currentTrack
    val isPlaying: StateFlow<Boolean> = playerConnection.isPlaying
    val hasNext: StateFlow<Boolean> = playerConnection.hasNext
    val hasPrevious: StateFlow<Boolean> = playerConnection.hasPrevious

    /** Whether the currently playing track is liked (drives the heart toggle). */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val currentIsLiked: StateFlow<Boolean> = currentTrack
        .flatMapLatest { track -> track?.let { repository.isLiked(it.id) } ?: flowOf(false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        playerConnection.connect()
    }

    /** Play [track] within the context of [queue] (e.g. the full search result list). */
    fun play(track: Track, queue: List<Track> = listOf(track)) {
        val start = queue.indexOfFirst { it.id == track.id }.takeIf { it >= 0 } ?: 0
        playerConnection.setQueue(queue, start)
    }

    fun togglePlayPause() = playerConnection.togglePlayPause()
    fun next() = playerConnection.next()
    fun previous() = playerConnection.previous()

    fun toggleLikeCurrent() {
        val track = currentTrack.value ?: return
        viewModelScope.launch { repository.toggleLike(track) }
    }
}
