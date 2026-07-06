package com.sonora.music.data.source

import com.sonora.music.core.model.AudioQuality
import com.sonora.music.core.model.HomeSection
import com.sonora.music.core.model.SearchResults
import com.sonora.music.core.model.SourceType
import com.sonora.music.core.model.StreamInfo
import com.sonora.music.core.model.Track
import com.sonora.music.core.source.MusicSource
import com.sonora.music.data.settings.SettingsStore
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
 * Spotify as a METADATA + HOME provider (no playback). Uses the unofficial anonymous web token
 * from open.spotify.com, then the public Web API for search / new-releases / recommendations.
 * Spotify tracks carry no stream, so [getStream] throws and the SourceResolver fails over to a
 * real audio provider (JioSaavn/YouTube) matched by title + artist — playback stays unchanged.
 */
@Singleton
class SpotifySource @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json,
    private val settings: SettingsStore,
) : MusicSource {

    override val type = SourceType.SPOTIFY
    override val enabled: Boolean get() = settings.settings.value.sourceEnabled[type] ?: true
    override val priority: Int = 30 // low: metadata only, never wins stream resolution

    @Volatile private var token: String? = null
    @Volatile private var tokenExpiry: Long = 0L

    private fun tokenOrNull(): String? {
        if (token != null && System.currentTimeMillis() < tokenExpiry) return token
        return runCatching {
            val req = Request.Builder()
                .url("https://open.spotify.com/get_access_token?reason=transport&productType=web_player")
                .header("User-Agent", "Mozilla/5.0")
                .build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string() ?: return null
                val o = json.parseToJsonElement(body).jsonObject
                token = o["accessToken"]?.jsonPrimitive?.content
                tokenExpiry = (o["accessTokenExpirationTimestampMs"]?.jsonPrimitive?.content?.toLongOrNull()
                    ?: (System.currentTimeMillis() + 30 * 60_000))
                token
            }
        }.getOrNull()
    }

    private fun apiGet(path: String): String? {
        val t = tokenOrNull() ?: return null
        val req = Request.Builder().url("https://api.spotify.com/v1/$path")
            .header("Authorization", "Bearer $t").build()
        return client.newCall(req).execute().use { if (it.isSuccessful) it.body?.string() else null }
    }

    override suspend fun search(query: String): SearchResults = withContext(Dispatchers.IO) {
        val body = apiGet("search?type=track&limit=25&q=${query.enc()}") ?: return@withContext SearchResults()
        val items = runCatching {
            json.parseToJsonElement(body).jsonObject["tracks"]?.jsonObject?.get("items")?.jsonArray
        }.getOrNull() ?: return@withContext SearchResults()
        SearchResults(tracks = items.mapNotNull { it.jsonObject.toTrack() })
    }

    override suspend fun getHome(): List<HomeSection> = withContext(Dispatchers.IO) {
        val sections = mutableListOf<HomeSection>()
        // New releases → tracks from each album's first track (via search fallback keeps it simple).
        apiGet("browse/new-releases?limit=12")?.let { body ->
            val albums = runCatching {
                json.parseToJsonElement(body).jsonObject["albums"]?.jsonObject?.get("items")?.jsonArray
            }.getOrNull()
            val tracks = albums?.mapNotNull { el ->
                val a = el.jsonObject
                Track(
                    id = "spotify:" + (a["id"]?.jsonPrimitive?.content ?: return@mapNotNull null),
                    sourceId = a["name"]?.jsonPrimitive?.content.orEmpty(),
                    source = type,
                    title = a["name"]?.jsonPrimitive?.content.orEmpty(),
                    artistName = a["artists"]?.jsonArray?.firstOrNull()?.jsonObject?.get("name")?.jsonPrimitive?.content.orEmpty(),
                    thumbnailUrl = a["images"]?.jsonArray?.firstOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.content,
                    maxQuality = AudioQuality.HIGH,
                )
            }.orEmpty()
            if (tracks.isNotEmpty()) sections += HomeSection("New releases", tracks)
        }
        sections
    }

    override suspend fun getStream(track: Track, preferred: AudioQuality): StreamInfo =
        error("Spotify is metadata-only; resolve playback from another source")

    /** Popular artists (name + image) for the onboarding picker, across a few genre seeds. */
    suspend fun popularArtists(): List<Pair<String, String?>> = withContext(Dispatchers.IO) {
        val seeds = listOf("pop", "hip hop", "bollywood", "rock", "punjabi", "edm", "indie", "r&b")
        val out = LinkedHashMap<String, String?>()
        for (seed in seeds) {
            val body = apiGet("search?type=artist&limit=6&q=${seed.enc()}") ?: continue
            val artists = runCatching {
                json.parseToJsonElement(body).jsonObject["artists"]?.jsonObject?.get("items")?.jsonArray
            }.getOrNull() ?: continue
            artists.forEach { el ->
                val a = el.jsonObject
                val name = a["name"]?.jsonPrimitive?.content ?: return@forEach
                val img = a["images"]?.jsonArray?.firstOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.content
                if (!out.containsKey(name)) out[name] = img
            }
            if (out.size >= 30) break
        }
        out.entries.map { it.key to it.value }
    }

    private fun kotlinx.serialization.json.JsonObject.toTrack(): Track? {
        val id = this["id"]?.jsonPrimitive?.content ?: return null
        val album = this["album"]?.jsonObject
        return Track(
            id = "spotify:$id",
            sourceId = id,
            source = type,
            title = this["name"]?.jsonPrimitive?.content.orEmpty(),
            artistName = this["artists"]?.jsonArray?.firstOrNull()?.jsonObject?.get("name")?.jsonPrimitive?.content.orEmpty(),
            albumTitle = album?.get("name")?.jsonPrimitive?.content,
            thumbnailUrl = album?.get("images")?.jsonArray?.firstOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.content,
            durationMs = this["duration_ms"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L,
            maxQuality = AudioQuality.HIGH,
            explicit = this["explicit"]?.jsonPrimitive?.content?.toBoolean() ?: false,
        )
    }

    private fun String.enc() = java.net.URLEncoder.encode(this, "UTF-8")
}
