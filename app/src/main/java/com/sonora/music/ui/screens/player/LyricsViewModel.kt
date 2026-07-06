package com.sonora.music.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonora.music.core.model.Lyrics
import com.sonora.music.core.model.Track
import com.sonora.music.core.model.UiState
import com.sonora.music.data.lyrics.LrcLibService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LyricsViewModel @Inject constructor(
    private val lrcLib: LrcLibService,
    settings: com.sonora.music.data.settings.SettingsStore,
) : ViewModel() {

    val lyricsPosition = settings.settings.value.lyricsPosition

    private val _state = MutableStateFlow<UiState<Lyrics>>(UiState.Loading)
    val state: StateFlow<UiState<Lyrics>> = _state.asStateFlow()

    private var loadedFor: String? = null

    fun load(track: Track) {
        if (loadedFor == track.id) return
        loadedFor = track.id
        _state.value = UiState.Loading
        viewModelScope.launch {
            val lyrics = runCatching { lrcLib.fetch(track) }.getOrNull()
            _state.value = when {
                lyrics == null -> UiState.Empty
                else -> UiState.Success(lyrics)
            }
        }
    }
}
