package com.sonora.music.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonora.music.core.model.HomeSection
import com.sonora.music.core.model.UiState
import com.sonora.music.data.repository.MusicRepository
import com.sonora.music.data.settings.SettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val settings: SettingsStore,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<HomeSection>>>(UiState.Loading)
    val state: StateFlow<UiState<List<HomeSection>>> = _state.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    val gridBig: Boolean get() = settings.settings.value.gridBig

    init { load() }

    fun load(isPullToRefresh: Boolean = false) {
        if (!isPullToRefresh) _state.value = UiState.Loading
        _refreshing.value = isPullToRefresh
        viewModelScope.launch {
            runCatching { buildFeed() }
                .onSuccess { sections -> _state.value = if (sections.isEmpty()) UiState.Empty else UiState.Success(sections) }
                .onFailure { _state.value = UiState.Error(it.message ?: "Couldn't load home", it) }
            _refreshing.value = false
        }
    }

    /**
     * Personalized feed: sections for the user's favorite genres and their most-played artist,
     * then the providers' own home rows. Falls back to just provider rows for new users.
     */
    private suspend fun buildFeed(): List<HomeSection> = coroutineScope {
        val genres = settings.settings.value.favoriteGenres.toList()
        val topArtist = runCatching { repository.topArtists.first().firstOrNull()?.artistName }.getOrNull()

        val personalized = buildList {
            genres.take(4).forEach { g -> add(async { section("$g for you", g) }) }
            if (!topArtist.isNullOrBlank()) add(async { section("More of $topArtist", topArtist) })
        }.mapNotNull { it.await() }

        val providerRows = runCatching { repository.homeFeed() }.getOrDefault(emptyList())
        personalized + providerRows
    }

    private suspend fun section(title: String, query: String): HomeSection? {
        val tracks = runCatching { repository.search(query).tracks.take(12) }.getOrDefault(emptyList())
        return if (tracks.isEmpty()) null else HomeSection(title, tracks)
    }

    fun refresh() = load(isPullToRefresh = true)
}
