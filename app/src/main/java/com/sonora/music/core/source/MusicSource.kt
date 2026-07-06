package com.sonora.music.core.source

import com.sonora.music.core.model.AudioQuality
import com.sonora.music.core.model.HomeSection
import com.sonora.music.core.model.Lyrics
import com.sonora.music.core.model.SearchResults
import com.sonora.music.core.model.SourceType
import com.sonora.music.core.model.StreamInfo
import com.sonora.music.core.model.Track

/**
 * A pluggable music provider. Every private/trick-based backend (YouTube, Qobuz, Tidal,
 * Amazon, Apple Music, JioSaavn, local) implements this. The rest of the app never talks
 * to a backend directly — only through this interface — so a source can be swapped, disabled,
 * or reconfigured (via RemoteConfig) without touching UI or playback code.
 */
interface MusicSource {

    val type: SourceType

    /** Whether this source is currently enabled (driven by RemoteConfig + user settings). */
    val enabled: Boolean

    /**
     * Default priority for the resolver — higher wins when the same track is available from
     * multiple sources and the user asked for "best quality". Overridable via RemoteConfig.
     */
    val priority: Int

    suspend fun search(query: String): SearchResults

    /** Optional home/discovery rows for this source. Default: none. */
    suspend fun getHome(): List<HomeSection> = emptyList()

    /**
     * Resolve a playable stream for [track] at the closest available quality to [preferred].
     * Throws on failure so the [SourceResolver] can fail over to the next source.
     */
    suspend fun getStream(track: Track, preferred: AudioQuality): StreamInfo

    /** Optional lyrics (LrcLib / provider-native). Null if unavailable. */
    suspend fun getLyrics(track: Track): Lyrics? = null

    /** Cheap health check so the resolver can skip a source whose trick is currently broken. */
    suspend fun isHealthy(): Boolean = enabled
}
