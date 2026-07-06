package com.sonora.music.core.source

import android.util.Log
import com.sonora.music.core.model.AudioQuality
import com.sonora.music.core.model.HomeSection
import com.sonora.music.core.model.SearchResults
import com.sonora.music.core.model.SourceType
import com.sonora.music.core.model.StreamInfo
import com.sonora.music.core.model.Track
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fans work out across all registered [MusicSource]s and, critically, provides FAILOVER:
 * if the source that owns a track can't produce a stream (its "trick" broke, token expired,
 * rate-limited), Sonora silently re-resolves the same song from another source so playback
 * never dies. This is what keeps the app alive when a private API changes upstream.
 */
@Singleton
class SourceResolver @Inject constructor(
    private val sources: Set<@JvmSuppressWildcards MusicSource>,
) {
    private val active: List<MusicSource>
        get() = sources.filter { it.enabled }.sortedByDescending { it.priority }

    /** Unified search: query every enabled source in parallel and merge, de-duplicated. */
    suspend fun searchAll(query: String): SearchResults = coroutineScope {
        val results = active.map { source ->
            async {
                runCatching { source.search(query) }
                    .onFailure { Log.w(TAG, "search failed on ${source.type}: ${it.message}") }
                    .getOrDefault(SearchResults())
            }
        }.awaitAll()

        SearchResults(
            tracks = results.flatMap { it.tracks }.dedupTracks(),
            albums = results.flatMap { it.albums },
            artists = results.flatMap { it.artists },
        )
    }

    /** Aggregate Home rows from every enabled source (highest-priority source first). */
    suspend fun homeFeed(): List<HomeSection> = coroutineScope {
        active.map { source ->
            async { runCatching { source.getHome() }.getOrDefault(emptyList()) }
        }.awaitAll().flatten()
    }

    /**
     * Resolve a stream for [track], starting with the owning source, then failing over to any
     * other enabled source that can match the same song (by title + artist).
     */
    suspend fun resolveStream(track: Track, preferred: AudioQuality): StreamInfo {
        val ordered = buildList {
            active.firstOrNull { it.type == track.source }?.let { add(it) }
            addAll(active.filter { it.type != track.source })
        }

        var lastError: Throwable? = null
        for (source in ordered) {
            val candidate = if (source.type == track.source) track else source.rematch(track) ?: continue
            val result = runCatching { source.getStream(candidate, preferred) }
            result.onSuccess { return it }
            result.onFailure {
                lastError = it
                Log.w(TAG, "stream failed on ${source.type}, failing over: ${it.message}")
            }
        }
        throw IllegalStateException("No source could stream '${track.title}'", lastError)
    }

    /** Find the equivalent of [track] on another source by searching for title + artist. */
    private suspend fun MusicSource.rematch(track: Track): Track? = runCatching {
        search("${track.title} ${track.artistName}").tracks.firstOrNull()
    }.getOrNull()

    private fun List<Track>.dedupTracks(): List<Track> {
        val seen = HashSet<String>()
        return filter { seen.add("${it.title.lowercase()}|${it.artistName.lowercase()}") }
    }

    companion object {
        private const val TAG = "SourceResolver"
        val PRIORITY_DEFAULTS = mapOf(
            SourceType.QOBUZ to 100,        // highest fidelity first
            SourceType.TIDAL to 90,
            SourceType.AMAZON_MUSIC to 80,
            SourceType.APPLE_MUSIC to 70,
            SourceType.YOUTUBE_MUSIC to 60, // widest catalog, lower quality
            SourceType.JIOSAAVN to 50,      // reliable DRM-free fallback
            SourceType.LOCAL to 40,
        )
    }
}
