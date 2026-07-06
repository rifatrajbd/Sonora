package com.sonora.music.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonora.music.core.model.SearchResults
import com.sonora.music.core.model.UiState
import com.sonora.music.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: MusicRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _state = MutableStateFlow<UiState<SearchResults>>(UiState.Empty)
    val state: StateFlow<UiState<SearchResults>> = _state.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(value: String) {
        _query.value = value
        searchJob?.cancel()
        if (value.isBlank()) {
            _state.value = UiState.Empty
            return
        }
        searchJob = viewModelScope.launch {
            delay(350) // debounce
            _state.value = UiState.Loading
            runCatching { repository.search(value) }
                .onSuccess { results ->
                    _state.value = if (results.tracks.isEmpty()) UiState.Empty
                    else UiState.Success(results)
                }
                .onFailure { _state.value = UiState.Error(it.message ?: "Search failed", it) }
        }
    }

    fun retry() = onQueryChange(_query.value)
}
