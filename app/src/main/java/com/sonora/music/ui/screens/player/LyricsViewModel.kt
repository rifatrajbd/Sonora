package com.sonora.music.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonora.music.core.model.LyricLine
import com.sonora.music.core.model.Lyrics
import com.sonora.music.core.model.Track
import com.sonora.music.core.model.UiState
import com.sonora.music.data.db.LyricsDao
import com.sonora.music.data.db.LyricsEntity
import com.sonora.music.data.lyrics.LrcLibService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Lyrics with an InnerTune-style disk cache: first look in Room (instant, works offline), only
 * then hit the network — and persist the result (including confirmed misses) so each song is
 * fetched at most once.
 */
@HiltViewModel
class LyricsViewModel @Inject constructor(
    private val lrcLib: LrcLibService,
    private val lyricsDao: LyricsDao,
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
            // 1) Disk cache — instant and offline-friendly.
            val cached = runCatching { lyricsDao.byId(track.id) }.getOrNull()
            if (cached != null) {
                // Re-fetch cached misses after a day; a song may gain lyrics on LrcLib later.
                val missExpired = cached.content.isEmpty() &&
                    System.currentTimeMillis() - cached.fetchedAt > MISS_TTL_MS
                if (!missExpired) {
                    _state.value = cached.toUiState()
                    return@launch
                }
            }

            // 2) Network, then persist whatever we learned (hit or miss).
            val lyrics = runCatching { lrcLib.fetch(track) }.getOrNull()
            if (loadedFor != track.id) return@launch // user already skipped to another song
            _state.value = if (lyrics == null) UiState.Empty else UiState.Success(lyrics)
            runCatching {
                lyricsDao.upsert(
                    LyricsEntity(
                        songId = track.id,
                        content = lyrics?.raw.orEmpty(),
                        synced = lyrics?.synced ?: false,
                        sourceName = lyrics?.sourceName ?: "",
                    )
                )
            }
        }
    }

    private fun LyricsEntity.toUiState(): UiState<Lyrics> = when {
        content.isEmpty() -> UiState.Empty
        synced -> UiState.Success(
            Lyrics(LrcLibService.parseLrc(content), synced = true, sourceName = sourceName, raw = content)
        )
        else -> UiState.Success(
            Lyrics(content.lines().map { LyricLine(-1, it) }, synced = false, sourceName = sourceName, raw = content)
        )
    }

    companion object {
        private const val MISS_TTL_MS = 24 * 60 * 60 * 1000L
    }
}
