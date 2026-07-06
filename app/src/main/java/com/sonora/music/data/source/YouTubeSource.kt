package com.sonora.music.data.source

import com.sonora.music.core.config.RemoteConfigRepository
import com.sonora.music.core.model.AudioQuality
import com.sonora.music.core.model.SearchResults
import com.sonora.music.core.model.SourceType
import com.sonora.music.core.model.StreamInfo
import com.sonora.music.core.model.Track
import com.sonora.music.core.source.MusicSource
import com.sonora.music.core.source.SourceResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfo as NpStreamInfo
import javax.inject.Inject
import javax.inject.Singleton

/**
 * YouTube Music source powered by NewPipeExtractor. We deliberately DON'T hand-roll the
 * InnerTube API: NewPipeExtractor already implements the client-spoof (Android VR / TV) and
 * PoToken handling that ReVanced relies on, and keeps up with Google's changes. That makes this
 * the most maintainable way to survive YouTube-side breakage.
 *
 * Requires NewPipe.init(...) once at app start (see SonoraApp).
 */
@Singleton
class YouTubeSource @Inject constructor(
    private val config: RemoteConfigRepository,
) : MusicSource {

    override val type = SourceType.YOUTUBE_MUSIC

    private val cfg get() = config.config.value.forType(type)
    override val enabled: Boolean get() = cfg?.enabled ?: true
    override val priority: Int
        get() = cfg?.priority ?: SourceResolver.PRIORITY_DEFAULTS[type] ?: 60

    private val yt get() = ServiceList.YouTube

    override suspend fun search(query: String): SearchResults = withContext(Dispatchers.IO) {
        val extractor = yt.getSearchExtractor(query)
        extractor.fetchPage()
        val tracks = extractor.initialPage.items
            .filterIsInstance<org.schabi.newpipe.extractor.stream.StreamInfoItem>()
            .map { item ->
                Track(
                    id = item.url,
                    sourceId = item.url,
                    source = type,
                    title = item.name.orEmpty(),
                    artistName = item.uploaderName.orEmpty(),
                    thumbnailUrl = item.thumbnails.lastOrNull()?.url,
                    durationMs = (item.duration.coerceAtLeast(0)) * 1000L,
                    maxQuality = AudioQuality.HIGH,
                )
            }
        SearchResults(tracks = tracks)
    }

    override suspend fun getStream(track: Track, preferred: AudioQuality): StreamInfo =
        withContext(Dispatchers.IO) {
            val info = NpStreamInfo.getInfo(yt, track.sourceId)
            val audio = info.audioStreams
                .filter { it.url != null }
                .maxByOrNull { it.averageBitrate }
                ?: error("YouTube: no audio stream for ${track.title}")
            StreamInfo(
                url = audio.url!!,
                quality = AudioQuality.from(bitrateKbps = audio.averageBitrate / 1000, lossless = false),
                mimeType = audio.format?.mimeType,
            )
        }

    override suspend fun isHealthy(): Boolean = enabled
}
