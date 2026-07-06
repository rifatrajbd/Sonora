package com.sonora.music.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sonora.music.core.model.AudioQuality
import com.sonora.music.core.model.SourceType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class SonoraSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val pureBlack: Boolean = true,
    val dynamicColor: Boolean = true,
    val audioQuality: AudioQuality = AudioQuality.LOSSLESS,
    /** Per-source enable overrides. Absent = use built-in default. */
    val sourceEnabled: Map<SourceType, Boolean> = emptyMap(),
    /** Per-source base-URL overrides for the provider backends. */
    val sourceBaseUrl: Map<SourceType, String> = emptyMap(),
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("sonora_settings")

/**
 * Persisted user settings (theme, audio quality, per-source config). Exposes a synchronous
 * snapshot [settings] StateFlow so the theme, player and sources can read it without suspending.
 */
@Singleton
class SettingsStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _settings = MutableStateFlow(SonoraSettings())
    val settings: StateFlow<SonoraSettings> = _settings.asStateFlow()

    init {
        // Load persisted values asynchronously (no main-thread blocking at startup).
        scope.launch {
            runCatching { context.dataStore.data.first() }.getOrNull()?.let {
                _settings.value = it.toSettings()
            }
        }
    }

    private fun Preferences.toSettings(): SonoraSettings {
        val enabled = SourceType.entries.mapNotNull { t ->
            this[booleanPreferencesKey("enabled_${t.name}")]?.let { t to it }
        }.toMap()
        val urls = SourceType.entries.mapNotNull { t ->
            this[stringPreferencesKey("url_${t.name}")]?.let { t to it }
        }.toMap()
        return SonoraSettings(
            themeMode = this[KEY_THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
            pureBlack = this[KEY_PURE_BLACK] ?: true,
            dynamicColor = this[KEY_DYNAMIC] ?: true,
            audioQuality = this[KEY_QUALITY]?.let { runCatching { AudioQuality.valueOf(it) }.getOrNull() } ?: AudioQuality.LOSSLESS,
            sourceEnabled = enabled,
            sourceBaseUrl = urls,
        )
    }

    fun setThemeMode(mode: ThemeMode) = update { it[KEY_THEME] = mode.name }.also {
        _settings.value = _settings.value.copy(themeMode = mode)
    }
    fun setPureBlack(v: Boolean) = update { it[KEY_PURE_BLACK] = v }.also {
        _settings.value = _settings.value.copy(pureBlack = v)
    }
    fun setDynamicColor(v: Boolean) = update { it[KEY_DYNAMIC] = v }.also {
        _settings.value = _settings.value.copy(dynamicColor = v)
    }
    fun setAudioQuality(q: AudioQuality) = update { it[KEY_QUALITY] = q.name }.also {
        _settings.value = _settings.value.copy(audioQuality = q)
    }
    fun setSourceEnabled(type: SourceType, enabled: Boolean) =
        update { it[booleanPreferencesKey("enabled_${type.name}")] = enabled }.also {
            _settings.value = _settings.value.copy(
                sourceEnabled = _settings.value.sourceEnabled + (type to enabled),
            )
        }
    fun setSourceBaseUrl(type: SourceType, url: String) =
        update { it[stringPreferencesKey("url_${type.name}")] = url }.also {
            _settings.value = _settings.value.copy(
                sourceBaseUrl = _settings.value.sourceBaseUrl + (type to url),
            )
        }

    private fun update(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        scope.launch { context.dataStore.edit(block) }
    }

    companion object {
        private val KEY_THEME = stringPreferencesKey("theme_mode")
        private val KEY_PURE_BLACK = booleanPreferencesKey("pure_black")
        private val KEY_DYNAMIC = booleanPreferencesKey("dynamic_color")
        private val KEY_QUALITY = stringPreferencesKey("audio_quality")
    }
}
