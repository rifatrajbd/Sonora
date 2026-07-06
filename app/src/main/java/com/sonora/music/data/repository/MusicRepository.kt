package com.sonora.music.data.repository

import com.sonora.music.core.model.AudioQuality
import com.sonora.music.core.model.SearchResults
import com.sonora.music.core.model.StreamInfo
import com.sonora.music.core.model.Track
import com.sonora.music.core.source.SourceResolver
import com.sonora.music.data.db.SongDao
import com.sonora.music.data.db.SongEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single entry point the UI/playback layer uses. Hides whether data comes from a remote source
 * (via [SourceResolver], with failover) or the local Room library.
 */
@Singleton
class MusicRepository @Inject constructor(
    private val resolver: SourceResolver,
    private val songDao: SongDao,
) {
    suspend fun search(query: String): SearchResults = resolver.searchAll(query)

    /** Resolve a playable stream with automatic cross-source failover. */
    suspend fun resolveStream(track: Track, preferred: AudioQuality): StreamInfo =
        resolver.resolveStream(track, preferred)

    // --- library ---------------------------------------------------------------

    val likedSongs: Flow<List<Track>> =
        songDao.likedSongs().map { list -> list.map { it.toTrack() } }

    val downloadedSongs: Flow<List<Track>> =
        songDao.downloadedSongs().map { list -> list.map { it.toTrack() } }

    fun isLiked(trackId: String): Flow<Boolean> = songDao.isLiked(trackId)

    suspend fun toggleLike(track: Track) {
        val existing = songDao.byId(track.id)
        if (existing == null) {
            songDao.upsert(SongEntity.from(track, liked = true))
        } else {
            val nowLiked = !existing.liked
            songDao.setLiked(track.id, nowLiked, if (nowLiked) System.currentTimeMillis() else null)
        }
    }
}
