package com.sonora.music.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
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
enum class LyricsPosition { SIDED, LEFT, CENTER, RIGHT }
enum class DefaultTab { HOME, SEARCH, LIBRARY, OFFLINE }
enum class LyricsProvider { LRCLIB, KUGOU }

data class SonoraSettings(
    // Appearance
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val pureBlack: Boolean = true,
    val defaultTab: DefaultTab = DefaultTab.HOME,
    val gridBig: Boolean = false,
    val lyricsPosition: LyricsPosition = LyricsPosition.CENTER,
    val squigglySlider: Boolean = false,
    // Content
    val contentLanguage: String = "System default",
    val contentCountry: String = "System default",
    val hideExplicit: Boolean = false,
    // Player & audio
    val audioQuality: AudioQuality = AudioQuality.LOSSLESS,
    val persistentQueue: Boolean = true,
    val autoLoadMore: Boolean = true,
    val skipSilence: Boolean = false,
    val audioNormalization: Boolean = false,
    val autoSkipOnError: Boolean = true,
    val stopOnTaskClear: Boolean = false,
    // Storage
    val maxCacheMb: Int = 0, // 0 = unlimited
    // Privacy
    val pauseListenHistory: Boolean = false,
    val pauseSearchHistory: Boolean = false,
    val disableScreenshot: Boolean = false,
    val lyricsProvider: LyricsProvider = LyricsProvider.LRCLIB,
    // On-device + providers
    val localSyncEnabled: Boolean = false,
    val sourceEnabled: Map<SourceType, Boolean> = emptyMap(),
    val sourceBaseUrl: Map<SourceType, String> = emptyMap(),
    // Onboarding & personalization
    val onboardingDone: Boolean = false,
    val favoriteGenres: Set<String> = emptySet(),
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("sonora_settings")

/** Persisted user settings, mirroring InnerTune's settings structure. */
@Singleton
class SettingsStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _settings = MutableStateFlow(SonoraSettings())
    val settings: StateFlow<SonoraSettings> = _settings.asStateFlow()

    init {
        scope.launch {
            runCatching { context.dataStore.data.first() }.getOrNull()?.let { _settings.value = it.toSettings() }
        }
    }

    private inline fun <reified E : Enum<E>> Preferences.enum(key: Preferences.Key<String>, default: E): E =
        this[key]?.let { runCatching { enumValueOf<E>(it) }.getOrNull() } ?: default

    private fun Preferences.toSettings(): SonoraSettings {
        val enabled = SourceType.entries.mapNotNull { t -> this[booleanPreferencesKey("enabled_${t.name}")]?.let { t to it } }.toMap()
        val urls = SourceType.entries.mapNotNull { t -> this[stringPreferencesKey("url_${t.name}")]?.let { t to it } }.toMap()
        return SonoraSettings(
            themeMode = enum(KEY_THEME, ThemeMode.SYSTEM),
            dynamicColor = this[KEY_DYNAMIC] ?: true,
            pureBlack = this[KEY_PURE_BLACK] ?: true,
            defaultTab = enum(KEY_DEFAULT_TAB, DefaultTab.HOME),
            gridBig = this[KEY_GRID_BIG] ?: false,
            lyricsPosition = enum(KEY_LYRICS_POS, LyricsPosition.CENTER),
            squigglySlider = this[KEY_SQUIGGLY] ?: false,
            contentLanguage = this[KEY_LANG] ?: "System default",
            contentCountry = this[KEY_COUNTRY] ?: "System default",
            hideExplicit = this[KEY_HIDE_EXPLICIT] ?: false,
            audioQuality = enum(KEY_QUALITY, AudioQuality.LOSSLESS),
            persistentQueue = this[KEY_PERSIST_QUEUE] ?: true,
            autoLoadMore = this[KEY_AUTO_LOAD] ?: true,
            skipSilence = this[KEY_SKIP_SILENCE] ?: false,
            audioNormalization = this[KEY_NORMALIZE] ?: false,
            autoSkipOnError = this[KEY_AUTO_SKIP] ?: true,
            stopOnTaskClear = this[KEY_STOP_TASK] ?: false,
            maxCacheMb = this[KEY_MAX_CACHE] ?: 0,
            pauseListenHistory = this[KEY_PAUSE_LISTEN] ?: false,
            pauseSearchHistory = this[KEY_PAUSE_SEARCH] ?: false,
            disableScreenshot = this[KEY_DISABLE_SS] ?: false,
            lyricsProvider = enum(KEY_LYRICS_PROVIDER, LyricsProvider.LRCLIB),
            localSyncEnabled = this[KEY_LOCAL_SYNC] ?: false,
            sourceEnabled = enabled,
            sourceBaseUrl = urls,
            onboardingDone = this[KEY_ONBOARDING] ?: false,
            favoriteGenres = this[KEY_GENRES] ?: emptySet(),
        )
    }

    private fun set(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit, update: (SonoraSettings) -> SonoraSettings) {
        _settings.value = update(_settings.value)
        scope.launch { context.dataStore.edit(block) }
    }

    fun setThemeMode(v: ThemeMode) = set({ it[KEY_THEME] = v.name }) { it.copy(themeMode = v) }
    fun setDynamicColor(v: Boolean) = set({ it[KEY_DYNAMIC] = v }) { it.copy(dynamicColor = v) }
    fun setPureBlack(v: Boolean) = set({ it[KEY_PURE_BLACK] = v }) { it.copy(pureBlack = v) }
    fun setDefaultTab(v: DefaultTab) = set({ it[KEY_DEFAULT_TAB] = v.name }) { it.copy(defaultTab = v) }
    fun setGridBig(v: Boolean) = set({ it[KEY_GRID_BIG] = v }) { it.copy(gridBig = v) }
    fun setLyricsPosition(v: LyricsPosition) = set({ it[KEY_LYRICS_POS] = v.name }) { it.copy(lyricsPosition = v) }
    fun setSquigglySlider(v: Boolean) = set({ it[KEY_SQUIGGLY] = v }) { it.copy(squigglySlider = v) }
    fun setContentLanguage(v: String) = set({ it[KEY_LANG] = v }) { it.copy(contentLanguage = v) }
    fun setContentCountry(v: String) = set({ it[KEY_COUNTRY] = v }) { it.copy(contentCountry = v) }
    fun setHideExplicit(v: Boolean) = set({ it[KEY_HIDE_EXPLICIT] = v }) { it.copy(hideExplicit = v) }
    fun setAudioQuality(v: AudioQuality) = set({ it[KEY_QUALITY] = v.name }) { it.copy(audioQuality = v) }
    fun setPersistentQueue(v: Boolean) = set({ it[KEY_PERSIST_QUEUE] = v }) { it.copy(persistentQueue = v) }
    fun setAutoLoadMore(v: Boolean) = set({ it[KEY_AUTO_LOAD] = v }) { it.copy(autoLoadMore = v) }
    fun setSkipSilence(v: Boolean) = set({ it[KEY_SKIP_SILENCE] = v }) { it.copy(skipSilence = v) }
    fun setAudioNormalization(v: Boolean) = set({ it[KEY_NORMALIZE] = v }) { it.copy(audioNormalization = v) }
    fun setAutoSkipOnError(v: Boolean) = set({ it[KEY_AUTO_SKIP] = v }) { it.copy(autoSkipOnError = v) }
    fun setStopOnTaskClear(v: Boolean) = set({ it[KEY_STOP_TASK] = v }) { it.copy(stopOnTaskClear = v) }
    fun setMaxCacheMb(v: Int) = set({ it[KEY_MAX_CACHE] = v }) { it.copy(maxCacheMb = v) }
    fun setPauseListenHistory(v: Boolean) = set({ it[KEY_PAUSE_LISTEN] = v }) { it.copy(pauseListenHistory = v) }
    fun setPauseSearchHistory(v: Boolean) = set({ it[KEY_PAUSE_SEARCH] = v }) { it.copy(pauseSearchHistory = v) }
    fun setDisableScreenshot(v: Boolean) = set({ it[KEY_DISABLE_SS] = v }) { it.copy(disableScreenshot = v) }
    fun setLyricsProvider(v: LyricsProvider) = set({ it[KEY_LYRICS_PROVIDER] = v.name }) { it.copy(lyricsProvider = v) }
    fun setLocalSyncEnabled(v: Boolean) = set({ it[KEY_LOCAL_SYNC] = v }) { it.copy(localSyncEnabled = v) }
    fun completeOnboarding(genres: Set<String>) = set({
        it[KEY_ONBOARDING] = true; it[KEY_GENRES] = genres
    }) { it.copy(onboardingDone = true, favoriteGenres = genres) }
    fun setFavoriteGenres(genres: Set<String>) = set({ it[KEY_GENRES] = genres }) { it.copy(favoriteGenres = genres) }
    fun setSourceEnabled(t: SourceType, v: Boolean) = set({ it[booleanPreferencesKey("enabled_${t.name}")] = v }) {
        it.copy(sourceEnabled = it.sourceEnabled + (t to v))
    }
    fun setSourceBaseUrl(t: SourceType, v: String) = set({ it[stringPreferencesKey("url_${t.name}")] = v }) {
        it.copy(sourceBaseUrl = it.sourceBaseUrl + (t to v))
    }

    companion object {
        private val KEY_THEME = stringPreferencesKey("theme_mode")
        private val KEY_DYNAMIC = booleanPreferencesKey("dynamic_color")
        private val KEY_PURE_BLACK = booleanPreferencesKey("pure_black")
        private val KEY_DEFAULT_TAB = stringPreferencesKey("default_tab")
        private val KEY_GRID_BIG = booleanPreferencesKey("grid_big")
        private val KEY_LYRICS_POS = stringPreferencesKey("lyrics_pos")
        private val KEY_SQUIGGLY = booleanPreferencesKey("squiggly_slider")
        private val KEY_LANG = stringPreferencesKey("content_language")
        private val KEY_COUNTRY = stringPreferencesKey("content_country")
        private val KEY_HIDE_EXPLICIT = booleanPreferencesKey("hide_explicit")
        private val KEY_QUALITY = stringPreferencesKey("audio_quality")
        private val KEY_PERSIST_QUEUE = booleanPreferencesKey("persistent_queue")
        private val KEY_AUTO_LOAD = booleanPreferencesKey("auto_load_more")
        private val KEY_SKIP_SILENCE = booleanPreferencesKey("skip_silence")
        private val KEY_NORMALIZE = booleanPreferencesKey("audio_normalization")
        private val KEY_AUTO_SKIP = booleanPreferencesKey("auto_skip_on_error")
        private val KEY_STOP_TASK = booleanPreferencesKey("stop_on_task_clear")
        private val KEY_MAX_CACHE = intPreferencesKey("max_cache_mb")
        private val KEY_PAUSE_LISTEN = booleanPreferencesKey("pause_listen_history")
        private val KEY_PAUSE_SEARCH = booleanPreferencesKey("pause_search_history")
        private val KEY_DISABLE_SS = booleanPreferencesKey("disable_screenshot")
        private val KEY_LYRICS_PROVIDER = stringPreferencesKey("lyrics_provider")
        private val KEY_LOCAL_SYNC = booleanPreferencesKey("local_sync")
        private val KEY_ONBOARDING = booleanPreferencesKey("onboarding_done")
        private val KEY_GENRES = stringSetPreferencesKey("favorite_genres")
    }
}
