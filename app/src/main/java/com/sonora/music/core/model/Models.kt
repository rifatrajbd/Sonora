package com.sonora.music.core.model

/** Which private/trick-based backend a piece of content came from. */
enum class SourceType(val displayName: String) {
    YOUTUBE_MUSIC("YouTube Music"),
    QOBUZ("Qobuz"),
    TIDAL("Tidal"),
    AMAZON_MUSIC("Amazon Music"),
    APPLE_MUSIC("Apple Music"),
    JIOSAAVN("JioSaavn"),
    LOCAL("On device");
}

/**
 * Audio quality tiers. [chipLabel] drives the Spotify-style badge shown on tracks
 * and the Now Playing screen (e.g. "HiFi", "HD", "FLAC").
 */
enum class AudioQuality(val chipLabel: String, val approxBitrateKbps: Int, val lossless: Boolean) {
    LOW("Low", 96, false),
    NORMAL("Normal", 160, false),
    HIGH("HD", 320, false),
    LOSSLESS("HiFi", 1000, true),
    HI_RES("Hi-Res", 3000, true);

    companion object {
        /** Best guess of a display tier from a container/codec + bitrate. */
        fun from(bitrateKbps: Int, lossless: Boolean, hiRes: Boolean = false): AudioQuality = when {
            hiRes -> HI_RES
            lossless -> LOSSLESS
            bitrateKbps >= 300 -> HIGH
            bitrateKbps >= 128 -> NORMAL
            else -> LOW
        }
    }
}

/** A resolvable stream for a track at a given quality from a given source. */
data class StreamInfo(
    val url: String,
    val quality: AudioQuality,
    val mimeType: String? = null,
    /** Extra headers the player must send (auth cookies, user tokens, spoofed UA). */
    val headers: Map<String, String> = emptyMap(),
    /** When non-null, the player must decrypt with this key (e.g. Deezer Blowfish). */
    val decryptionKey: String? = null,
)

data class Artist(
    val id: String,
    val name: String,
    val thumbnailUrl: String? = null,
    val source: SourceType,
)

data class Album(
    val id: String,
    val title: String,
    val artistName: String,
    val thumbnailUrl: String? = null,
    val year: Int? = null,
    val source: SourceType,
)

/**
 * A single playable track. [sourceId] is opaque to the app and only meaningful to the
 * originating [MusicSource]; resolving a stream is deferred until playback via that source.
 */
data class Track(
    val id: String,
    val sourceId: String,
    val source: SourceType,
    val title: String,
    val artistName: String,
    val albumTitle: String? = null,
    val thumbnailUrl: String? = null,
    val durationMs: Long = 0L,
    /** Best quality this source claims it can deliver for this track. */
    val maxQuality: AudioQuality = AudioQuality.HIGH,
    val explicit: Boolean = false,
)

data class SearchResults(
    val tracks: List<Track> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
)

/** One synced lyric line; [startMs] < 0 means unsynced/plain line. */
data class LyricLine(val startMs: Long, val text: String)

data class Lyrics(val lines: List<LyricLine>, val synced: Boolean, val sourceName: String)
