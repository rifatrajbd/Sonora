package com.sonora.music.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonora.music.core.model.AudioQuality
import com.sonora.music.core.model.SourceType
import com.sonora.music.data.repository.MusicRepository
import com.sonora.music.data.settings.DefaultTab
import com.sonora.music.data.settings.LyricsPosition
import com.sonora.music.data.settings.LyricsProvider
import com.sonora.music.data.settings.SettingsStore
import com.sonora.music.data.settings.SonoraSettings
import com.sonora.music.data.settings.ThemeMode
import com.sonora.music.playback.PlayerConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val store: SettingsStore,
    private val player: PlayerConnection,
    private val repository: MusicRepository,
) : ViewModel() {

    val settings: StateFlow<SonoraSettings> = store.settings

    // Appearance
    fun setThemeMode(v: ThemeMode) = store.setThemeMode(v)
    fun setPureBlack(v: Boolean) = store.setPureBlack(v)
    fun setDynamicColor(v: Boolean) = store.setDynamicColor(v)
    fun setDefaultTab(v: DefaultTab) = store.setDefaultTab(v)
    fun setGridBig(v: Boolean) = store.setGridBig(v)
    fun setLyricsPosition(v: LyricsPosition) = store.setLyricsPosition(v)
    fun setSquigglySlider(v: Boolean) = store.setSquigglySlider(v)

    // Content
    fun setContentLanguage(v: String) = store.setContentLanguage(v)
    fun setContentCountry(v: String) = store.setContentCountry(v)
    fun setHideExplicit(v: Boolean) = store.setHideExplicit(v)

    // Player & audio
    fun setAudioQuality(q: AudioQuality) { store.setAudioQuality(q); player.setPreferredQuality(q) }
    fun setPersistentQueue(v: Boolean) = store.setPersistentQueue(v)
    fun setAutoLoadMore(v: Boolean) = store.setAutoLoadMore(v)
    fun setSkipSilence(v: Boolean) = store.setSkipSilence(v)
    fun setAudioNormalization(v: Boolean) = store.setAudioNormalization(v)
    fun setAutoSkipOnError(v: Boolean) = store.setAutoSkipOnError(v)
    fun setStopOnTaskClear(v: Boolean) = store.setStopOnTaskClear(v)

    // Storage
    fun setMaxCacheMb(v: Int) = store.setMaxCacheMb(v)

    // Privacy
    fun setPauseListenHistory(v: Boolean) = store.setPauseListenHistory(v)
    fun setPauseSearchHistory(v: Boolean) = store.setPauseSearchHistory(v)
    fun setDisableScreenshot(v: Boolean) = store.setDisableScreenshot(v)
    fun setLyricsProvider(v: LyricsProvider) = store.setLyricsProvider(v)
    fun clearListenHistory() = viewModelScope.launch { repository.clearHistory() }

    // Sources / on-device
    fun setLocalSyncEnabled(v: Boolean) = store.setLocalSyncEnabled(v)
    fun setSourceEnabled(type: SourceType, enabled: Boolean) = store.setSourceEnabled(type, enabled)
    fun setSourceBaseUrl(type: SourceType, url: String) = store.setSourceBaseUrl(type, url)

    val configurableSources = SourceType.entries.filter { it != SourceType.LOCAL }
}
