package com.sonora.music.ui.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonora.music.core.model.Track
import com.sonora.music.data.db.ArtistPlays
import com.sonora.music.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryStatsViewModel @Inject constructor(
    private val repository: MusicRepository,
) : ViewModel() {

    val recentlyPlayed: StateFlow<List<Track>> = repository.recentlyPlayed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val topSongs: StateFlow<List<Track>> = repository.topSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val topArtists: StateFlow<List<ArtistPlays>> = repository.topArtists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val totalPlays: StateFlow<Int> = repository.totalPlays
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val totalListenedMs: StateFlow<Long> = repository.totalListenedMs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    fun clearHistory() = viewModelScope.launch { repository.clearHistory() }
}
