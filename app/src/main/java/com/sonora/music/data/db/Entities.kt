package com.sonora.music.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sonora.music.core.model.AudioQuality
import com.sonora.music.core.model.SourceType
import com.sonora.music.core.model.Track

/** A track saved into the library (liked or in a playlist / downloaded). */
@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: String,
    val sourceId: String,
    val source: SourceType,
    val title: String,
    val artistName: String,
    val albumTitle: String?,
    val thumbnailUrl: String?,
    val durationMs: Long,
    val maxQuality: AudioQuality,
    val liked: Boolean = false,
    val likedAt: Long? = null,
    val downloaded: Boolean = false,
    val localPath: String? = null,
    val addedAt: Long = System.currentTimeMillis(),
) {
    fun toTrack() = Track(
        id = id, sourceId = sourceId, source = source, title = title,
        artistName = artistName, albumTitle = albumTitle, thumbnailUrl = thumbnailUrl,
        durationMs = durationMs, maxQuality = maxQuality,
    )

    companion object {
        fun from(t: Track, liked: Boolean = false) = SongEntity(
            id = t.id, sourceId = t.sourceId, source = t.source, title = t.title,
            artistName = t.artistName, albumTitle = t.albumTitle, thumbnailUrl = t.thumbnailUrl,
            durationMs = t.durationMs, maxQuality = t.maxQuality, liked = liked,
            likedAt = if (liked) System.currentTimeMillis() else null,
        )
    }
}

/** One playback event, denormalised so history & stats queries need no joins. */
@Entity(tableName = "play_events")
data class PlayEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: String,
    val title: String,
    val artistName: String,
    val albumTitle: String?,
    val thumbnailUrl: String?,
    val source: SourceType,
    val sourceId: String,
    val maxQuality: AudioQuality,
    val durationMs: Long,
    val playedAt: Long = System.currentTimeMillis(),
) {
    fun toTrack() = Track(
        id = songId, sourceId = sourceId, source = source, title = title,
        artistName = artistName, albumTitle = albumTitle, thumbnailUrl = thumbnailUrl,
        durationMs = durationMs, maxQuality = maxQuality,
    )

    companion object {
        fun from(t: Track) = PlayEventEntity(
            songId = t.id, title = t.title, artistName = t.artistName, albumTitle = t.albumTitle,
            thumbnailUrl = t.thumbnailUrl, source = t.source, sourceId = t.sourceId,
            maxQuality = t.maxQuality, durationMs = t.durationMs,
        )
    }
}

/** Aggregated top-artist row for the stats screen. */
data class ArtistPlays(val artistName: String, val plays: Int)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "playlist_songs", primaryKeys = ["playlistId", "songId"])
data class PlaylistSongCrossRef(
    val playlistId: Long,
    val songId: String,
    val position: Int,
)
