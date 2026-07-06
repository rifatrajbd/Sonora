package com.sonora.music.ui.screens.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonora.music.core.model.SearchResults
import com.sonora.music.core.model.UiState
import com.sonora.music.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val repository: MusicRepository,
) : ViewModel() {

    private val _selected = MutableStateFlow<String?>(null)
    val selected: StateFlow<String?> = _selected.asStateFlow()

    private val _state = MutableStateFlow<UiState<SearchResults>>(UiState.Empty)
    val state: StateFlow<UiState<SearchResults>> = _state.asStateFlow()

    fun select(category: String, query: String) {
        _selected.value = category
        _state.value = UiState.Loading
        viewModelScope.launch {
            runCatching { repository.search(query) }
                .onSuccess {
                    _state.value = if (it.tracks.isEmpty()) UiState.Empty else UiState.Success(it)
                }
                .onFailure { _state.value = UiState.Error(it.message ?: "Couldn't load", it) }
        }
    }

    companion object {
        // Category label -> underlying search query. Colours cycle in the UI.
        val CATEGORIES = listOf(
            "Pop" to "pop hits",
            "Hip-Hop" to "hip hop",
            "Lo-fi" to "lofi beats",
            "Rock" to "rock classics",
            "Chill" to "chill vibes",
            "Workout" to "workout energy",
            "Romance" to "love songs",
            "Focus" to "focus instrumental",
            "Party" to "party dance",
            "Sad" to "sad songs",
            "Indie" to "indie",
            "Jazz" to "jazz",
        )
    }
}
