package com.sonora.music.data.source

import com.sonora.music.core.model.Track
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enriches on-device tracks with metadata from an online provider: matches by title + artist and
 * copies over cover art (and cleaned title/artist/album) when the local tags are missing or stale.
 * Uses JioSaavn as the metadata source since it's DRM-free and reliable.
 */
@Singleton
class MetadataMatcher @Inject constructor(
    private val jioSaavn: JioSaavnSource,
) {
    /** Return an enriched copy of [local], or the original if no confident match was found. */
    suspend fun enrich(local: Track): Track {
        val query = "${local.title} ${local.artistName}".trim()
        val match = runCatching { jioSaavn.search(query).tracks.firstOrNull() }.getOrNull()
            ?: return local
        // Only adopt the online cover + cleaned fields; keep the local stream (sourceId/source).
        return local.copy(
            title = match.title.ifBlank { local.title },
            artistName = match.artistName.ifBlank { local.artistName },
            albumTitle = match.albumTitle ?: local.albumTitle,
            thumbnailUrl = match.thumbnailUrl ?: local.thumbnailUrl,
        )
    }
}
