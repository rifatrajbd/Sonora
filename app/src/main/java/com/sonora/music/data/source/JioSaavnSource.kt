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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

/**
 * JioSaavn via the unofficial REST API (e.g. saavn.dev / your self-hosted instance). This is the
 * most robust "easy" source: DRM-free MP3/M4A up to 320kbps, no token dance, no decryption.
 * It's Sonora's reliability floor — when a lossless trick hiccups, failover lands here so the
 * app never goes silent. Base URL comes from RemoteConfig (self-host to avoid rate limits).
 *
 * The parse below targets the common `saavn.dev` v4 response shape.
 */
@Singleton
class JioSaavnSource @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json,
    private val config: RemoteConfigRepository,
    private val settings: com.sonora.music.data.settings.SettingsStore,
) : MusicSource {

    override val type = SourceType.JIOSAAVN

    private val cfg get() = config.config.value.forType(type)
    override val enabled: Boolean
        get() = settings.settings.value.sourceEnabled[type] ?: cfg?.enabled ?: true
    override val priority: Int
        get() = cfg?.priority ?: SourceResolver.PRIORITY_DEFAULTS[type] ?: 50

    private val baseUrl: String
        get() = (settings.settings.value.sourceBaseUrl[type]?.takeIf { it.isNotBlank() }
            ?: cfg?.baseUrl ?: DEFAULT_BASE).trimEnd('/')

    override suspend fun search(query: String): SearchResults = withContext(Dispatchers.IO) {
        val body = get("$baseUrl/api/search/songs?query=${query.encode()}&limit=25")
            ?: return@withContext SearchResults()
        val results = runCatching {
            val root = json.parseToJsonElement(body).jsonObject
            val list = root["data"]?.jsonObject?.get("results")?.jsonArray ?: return@runCatching emptyList()
            list.map { el ->
                val o = el.jsonObject
                val artists = o["artists"]?.jsonObject?.get("primary")?.jsonArray
                    ?.joinToString { it.jsonObject["name"]?.jsonPrimitive?.content.orEmpty() }
                Track(
                    id = o["id"]?.jsonPrimitive?.content.orEmpty(),
                    sourceId = o["id"]?.jsonPrimitive?.content.orEmpty(),
                    source = type,
                    title = o["name"]?.jsonPrimitive?.content.orEmpty().decodeHtml(),
                    artistName = artists.orEmpty().decodeHtml(),
                    albumTitle = o["album"]?.jsonObject?.get("name")?.jsonPrimitive?.content?.decodeHtml(),
                    thumbnailUrl = o["image"]?.jsonArray?.lastOrNull()
                        ?.jsonObject?.get("url")?.jsonPrimitive?.content,
                    durationMs = (o["duration"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L) * 1000,
                    maxQuality = AudioQuality.HIGH,
                )
            }
        }.getOrDefault(emptyList())
        SearchResults(tracks = results)
    }

    /** Look up a real artist portrait (name + image) via the artists endpoint. */
    suspend fun searchArtistPick(query: String): Pair<String, String?>? = withContext(Dispatchers.IO) {
        val body = get("$baseUrl/api/search/artists?query=${query.encode()}&limit=1") ?: return@withContext null
        runCatching {
            val results = json.parseToJsonElement(body).jsonObject["data"]?.jsonObject?.get("results")?.jsonArray
            val a = results?.firstOrNull()?.jsonObject ?: return@runCatching null
            val name = a["name"]?.jsonPrimitive?.content ?: return@runCatching null
            val img = a["image"]?.jsonArray?.lastOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.content
            name to img
        }.getOrNull()
    }

    override suspend fun getHome(): List<com.sonora.music.core.model.HomeSection> {
        // Build a lightweight home feed from a few seed queries (works without a fragile
        // provider-specific "modules" schema). Swap for a real home endpoint later.
        val seeds = listOf(
            "Trending" to "top hits",
            "New Releases" to "new songs",
            "Chill" to "lofi chill",
        )
        return seeds.mapNotNull { (title, query) ->
            val tracks = runCatching { search(query).tracks.take(12) }.getOrDefault(emptyList())
            if (tracks.isEmpty()) null else com.sonora.music.core.model.HomeSection(title, tracks)
        }
    }

    override suspend fun getStream(track: Track, preferred: AudioQuality): StreamInfo =
        withContext(Dispatchers.IO) {
            val body = get("$baseUrl/api/songs/${track.sourceId}") ?: error("JioSaavn: empty response")
            val root = json.parseToJsonElement(body).jsonObject
            val song = root["data"]?.jsonArray?.firstOrNull()?.jsonObject
                ?: error("JioSaavn: no song data")
            val urls = song["downloadUrl"]?.jsonArray ?: error("JioSaavn: no download urls")
            val best = urls.last().jsonObject // last = highest quality (320kbps)
            StreamInfo(
                url = best["url"]?.jsonPrimitive?.content ?: error("JioSaavn: missing url"),
                quality = AudioQuality.HIGH,
                mimeType = "audio/mp4",
            )
        }

    private fun get(url: String): String? =
        client.newCall(Request.Builder().url(url).build()).execute()
            .use { if (it.isSuccessful) it.body?.string() else null }

    private fun String.encode() = java.net.URLEncoder.encode(this, "UTF-8")

    /** JioSaavn returns HTML-encoded text (e.g. &quot;, &amp;). Decode it for display. */
    @Suppress("DEPRECATION")
    private fun String.decodeHtml(): String =
        android.text.Html.fromHtml(this, android.text.Html.FROM_HTML_MODE_LEGACY).toString()

    companion object {
        // A public, DRM-free JioSaavn API instance (sumitkolhe-schema). Override in Settings.
        const val DEFAULT_BASE = "https://saavn-api.nandanvarma.com"
    }
}
