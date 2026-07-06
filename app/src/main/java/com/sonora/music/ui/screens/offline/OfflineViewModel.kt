package com.sonora.music.ui.screens.offline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonora.music.core.model.Track
import com.sonora.music.data.repository.MusicRepository
import com.sonora.music.data.source.LocalMusicSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class OfflineViewModel @Inject constructor(
    repository: MusicRepository,
    localSource: LocalMusicSource,
) : ViewModel() {

    val downloads: StateFlow<List<Track>> = repository.downloadedSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val localTracks: StateFlow<List<Track>> = localSource.tracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
