package com.sonora.music.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonora.music.core.model.HomeSection
import com.sonora.music.core.model.UiState
import com.sonora.music.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MusicRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<HomeSection>>>(UiState.Loading)
    val state: StateFlow<UiState<List<HomeSection>>> = _state.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    init { load() }

    fun load(isPullToRefresh: Boolean = false) {
        if (!isPullToRefresh) _state.value = UiState.Loading
        _refreshing.value = isPullToRefresh
        viewModelScope.launch {
            runCatching { repository.homeFeed() }
                .onSuccess { sections ->
                    _state.value = if (sections.isEmpty()) UiState.Empty else UiState.Success(sections)
                }
                .onFailure { _state.value = UiState.Error(it.message ?: "Couldn't load home", it) }
            _refreshing.value = false
        }
    }

    fun refresh() = load(isPullToRefresh = true)
}
