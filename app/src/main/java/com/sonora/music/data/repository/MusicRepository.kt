package com.sonora.music.data.repository

import com.sonora.music.core.model.AudioQuality
import com.sonora.music.core.model.SearchResults
import com.sonora.music.core.model.StreamInfo
import com.sonora.music.core.model.Track
import com.sonora.music.core.source.SourceResolver
import com.sonora.music.data.db.ArtistPlays
import com.sonora.music.data.db.HistoryDao
import com.sonora.music.data.db.PlayEventEntity
import com.sonora.music.data.db.SongDao
import com.sonora.music.data.db.SongEntity
import com.sonora.music.data.download.DownloadManager
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
    private val historyDao: HistoryDao,
    private val downloadManager: DownloadManager,
    private val settings: com.sonora.music.data.settings.SettingsStore,
) {
    suspend fun search(query: String): SearchResults = resolver.searchAll(query)

    suspend fun homeFeed() = resolver.homeFeed()

    /** Resolve a playable stream with automatic cross-source failover. */
    suspend fun resolveStream(track: Track, preferred: AudioQuality): StreamInfo =
        resolver.resolveStream(track, preferred)

    /** Local file path if the track is downloaded, else null (playback prefers this). */
    suspend fun localPath(trackId: String): String? = songDao.localPath(trackId)

    // --- library ---------------------------------------------------------------

    val likedSongs: Flow<List<Track>> =
        songDao.likedSongs().map { list -> list.map { it.toTrack() } }

    val downloadedSongs: Flow<List<Track>> =
        songDao.downloadedSongs().map { list -> list.map { it.toTrack() } }

    fun isLiked(trackId: String): Flow<Boolean> = songDao.isLiked(trackId)
    fun isDownloaded(trackId: String): Flow<Boolean> = songDao.isDownloaded(trackId)
    val downloadsInProgress: Flow<Set<String>> = downloadManager.inProgress

    suspend fun toggleLike(track: Track) {
        val existing = songDao.byId(track.id)
        if (existing == null) {
            songDao.upsert(SongEntity.from(track, liked = true))
        } else {
            val nowLiked = !existing.liked
            songDao.setLiked(track.id, nowLiked, if (nowLiked) System.currentTimeMillis() else null)
        }
    }

    // --- history & stats -------------------------------------------------------

    suspend fun recordPlay(track: Track) {
        if (settings.settings.value.pauseListenHistory) return
        historyDao.record(PlayEventEntity.from(track))
    }

    val recentlyPlayed: Flow<List<Track>> =
        historyDao.recentlyPlayed().map { list -> list.map { it.toTrack() } }
    val topSongs: Flow<List<Track>> =
        historyDao.topSongs().map { list -> list.map { it.toTrack() } }
    val topArtists: Flow<List<ArtistPlays>> = historyDao.topArtists()
    val totalPlays: Flow<Int> = historyDao.totalPlays()
    val totalListenedMs: Flow<Long> = historyDao.totalListenedMs()
    suspend fun clearHistory() = historyDao.clear()

    /** Download for offline, or remove the local copy if already downloaded. */
    suspend fun toggleDownload(track: Track): Result<Unit> {
        return if (songDao.localPath(track.id) != null) {
            downloadManager.remove(track); Result.success(Unit)
        } else {
            downloadManager.download(track)
        }
    }
}
