package com.sonora.music.ui.screens.settings

import androidx.lifecycle.ViewModel
import com.sonora.music.core.model.AudioQuality
import com.sonora.music.core.model.SourceType
import com.sonora.music.data.settings.SettingsStore
import com.sonora.music.data.settings.SonoraSettings
import com.sonora.music.data.settings.ThemeMode
import com.sonora.music.playback.PlayerConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val store: SettingsStore,
    private val player: PlayerConnection,
) : ViewModel() {

    val settings: StateFlow<SonoraSettings> = store.settings

    fun setThemeMode(mode: ThemeMode) = store.setThemeMode(mode)
    fun setPureBlack(v: Boolean) = store.setPureBlack(v)
    fun setDynamicColor(v: Boolean) = store.setDynamicColor(v)

    fun setAudioQuality(q: AudioQuality) {
        store.setAudioQuality(q)
        player.setPreferredQuality(q)
    }

    fun setSourceEnabled(type: SourceType, enabled: Boolean) = store.setSourceEnabled(type, enabled)
    fun setSourceBaseUrl(type: SourceType, url: String) = store.setSourceBaseUrl(type, url)
    fun setLocalSyncEnabled(v: Boolean) = store.setLocalSyncEnabled(v)

    /** Sources the user can meaningfully configure in the UI (name shown generically elsewhere). */
    val configurableSources = SourceType.entries.filter { it != SourceType.LOCAL }
}
