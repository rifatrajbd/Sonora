package com.sonora.music.data.source

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.sonora.music.core.model.AudioQuality
import com.sonora.music.core.model.HomeSection
import com.sonora.music.core.model.SearchResults
import com.sonora.music.core.model.SourceType
import com.sonora.music.core.model.StreamInfo
import com.sonora.music.core.model.Track
import com.sonora.music.core.source.MusicSource
import com.sonora.music.data.settings.SettingsStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On-device music via MediaStore. Enabled only when the user turns on local sync in Settings.
 * Streams play straight from the content:// URI, so no network is involved. Metadata can be
 * enriched from an online provider via [MetadataMatcher].
 */
@Singleton
class LocalMusicSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsStore,
) : MusicSource {

    override val type = SourceType.LOCAL
    override val enabled: Boolean get() = settings.settings.value.localSyncEnabled
    override val priority: Int = 40

    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    /** Scan the device for audio files. Requires READ_MEDIA_AUDIO permission (caller ensures it). */
    suspend fun scan(): List<Track> = withContext(Dispatchers.IO) {
        val result = mutableListOf<Track>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        context.contentResolver.query(collection, projection, selection, null,
            "${MediaStore.Audio.Media.TITLE} ASC")?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val contentUri = ContentUris.withAppendedId(collection, id)
                val albumArt = ContentUris.withAppendedId(
                    android.net.Uri.parse("content://media/external/audio/albumart"),
                    c.getLong(albumIdCol),
                ).toString()
                result += Track(
                    id = "local:$id",
                    sourceId = contentUri.toString(),
                    source = SourceType.LOCAL,
                    title = c.getString(titleCol) ?: "Unknown",
                    artistName = c.getString(artistCol)?.takeIf { it != "<unknown>" } ?: "Unknown artist",
                    albumTitle = c.getString(albumCol),
                    thumbnailUrl = albumArt,
                    durationMs = c.getLong(durCol),
                    maxQuality = AudioQuality.LOSSLESS,
                )
            }
        }
        _tracks.value = result
        result
    }

    /** Replace a scanned track's metadata (e.g. after an online match). */
    fun applyEnrichment(updated: Track) {
        _tracks.value = _tracks.value.map { if (it.id == updated.id) updated else it }
    }

    override suspend fun search(query: String): SearchResults {
        val q = query.trim().lowercase()
        val matches = _tracks.value.filter {
            it.title.lowercase().contains(q) || it.artistName.lowercase().contains(q)
        }
        return SearchResults(tracks = matches)
    }

    override suspend fun getHome(): List<HomeSection> =
        if (_tracks.value.isEmpty()) emptyList()
        else listOf(HomeSection("On this device", _tracks.value.take(20)))

    override suspend fun getStream(track: Track, preferred: AudioQuality): StreamInfo =
        StreamInfo(url = track.sourceId, quality = AudioQuality.LOSSLESS)
}
