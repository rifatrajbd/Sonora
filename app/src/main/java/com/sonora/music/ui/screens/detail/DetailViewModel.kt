package com.sonora.music.ui.screens.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonora.music.core.model.Track
import com.sonora.music.core.model.UiState
import com.sonora.music.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.URLDecoder
import javax.inject.Inject

/**
 * Backs both the Artist and Album detail screens. Since providers expose search, the detail is
 * built by querying for the artist/album name and listing the matching tracks.
 */
@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: MusicRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val title: String = URLDecoder.decode(savedStateHandle.get<String>("name").orEmpty(), "UTF-8")

    private val _state = MutableStateFlow<UiState<List<Track>>>(UiState.Loading)
    val state: StateFlow<UiState<List<Track>>> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            runCatching { repository.search(title).tracks }
                .onSuccess { tracks -> _state.value = if (tracks.isEmpty()) UiState.Empty else UiState.Success(tracks) }
                .onFailure { _state.value = UiState.Error(it.message ?: "Couldn't load", it) }
        }
    }
}
