package com.sonora.music.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonora.music.core.model.Track
import com.sonora.music.data.repository.MusicRepository
import com.sonora.music.data.settings.SettingsStore
import com.sonora.music.data.source.LocalMusicSource
import com.sonora.music.data.source.MetadataMatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    repository: MusicRepository,
    private val localSource: LocalMusicSource,
    private val matcher: MetadataMatcher,
    settings: SettingsStore,
) : ViewModel() {

    val likedSongs: StateFlow<List<Track>> = repository.likedSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val localTracks: StateFlow<List<Track>> = localSource.tracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val localSyncEnabled: StateFlow<Boolean> = settings.settings
        .map { it.localSyncEnabled }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing
    private val _matching = MutableStateFlow(false)
    val matching: StateFlow<Boolean> = _matching

    /** Scan the device for audio (call after the media permission is granted). */
    fun syncLocal() {
        viewModelScope.launch {
            _syncing.value = true
            runCatching { localSource.scan() }
            _syncing.value = false
        }
    }

    /** Enrich each local track's tags/cover art from the online provider. */
    fun matchMetadata() {
        viewModelScope.launch {
            _matching.value = true
            localSource.tracks.value.forEach { track ->
                val enriched = runCatching { matcher.enrich(track) }.getOrNull()
                if (enriched != null && enriched != track) localSource.applyEnrichment(enriched)
            }
            _matching.value = false
        }
    }
}
