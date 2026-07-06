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
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Apple Music via the "anonymous web token" trick: the web player fetches a bearer token from
 * https://sf-api-token-service.itunes.apple.com/apiToken , which authorises the public catalog
 * endpoints (amp-api.music.apple.com). Search + rich metadata work with just that token.
 *
 * Full-fidelity streaming additionally needs a `media-user-token` from a logged-in session and
 * decryption, so treat this as a metadata/preview source unless you route it through your own
 * backend (recommended, same pattern as squid.wtf). Token + backend URL come from RemoteConfig
 * so they can rotate without an app update.
 *
 * This is a skeleton — wire parseSearch/parseStream to your chosen endpoint shape.
 */
@Singleton
class AppleMusicSource @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json,
    private val config: RemoteConfigRepository,
) : MusicSource {

    override val type = SourceType.APPLE_MUSIC

    private val cfg get() = config.config.value.forType(type)
    override val enabled: Boolean get() = cfg?.enabled ?: true
    override val priority: Int
        get() = cfg?.priority ?: SourceResolver.PRIORITY_DEFAULTS[type] ?: 70

    private val storefront get() = cfg?.params?.get("storefront") ?: "us"
    private val backend get() = cfg?.baseUrl?.trimEnd('/')

    override suspend fun search(query: String): SearchResults = withContext(Dispatchers.IO) {
        val token = fetchAnonToken() ?: return@withContext SearchResults()
        val url = "https://amp-api.music.apple.com/v1/catalog/$storefront/search" +
            "?term=${query.encode()}&types=songs&limit=25"
        val body = authedGet(url, token) ?: return@withContext SearchResults()
        // TODO: map Apple's { results.songs.data[] } schema -> SearchResults.
        SearchResults()
    }

    override suspend fun getStream(track: Track, preferred: AudioQuality): StreamInfo =
        withContext(Dispatchers.IO) {
            val base = backend ?: error("Apple Music streaming needs a backend URL in RemoteConfig")
            val body = get("$base/api/apple/track?id=${track.sourceId}")
                ?: error("Apple Music: empty response")
            // TODO: parse your backend's stream response.
            error("Implement AppleMusicSource stream parsing for your backend")
        }

    // --- trick: anonymous token -------------------------------------------------

    private fun fetchAnonToken(): String? {
        // Some builds require scraping the web bundle's developer token instead; RemoteConfig
        // can supply a pre-provisioned token via params["bearer"] to skip this call.
        cfg?.params?.get("bearer")?.let { return it }
        val req = Request.Builder()
            .url("https://sf-api-token-service.itunes.apple.com/apiToken")
            .header("Origin", "https://music.apple.com")
            .build()
        return runCatching {
            client.newCall(req).execute().use { if (it.isSuccessful) it.body?.string() else null }
        }.getOrNull()
    }

    private fun authedGet(url: String, token: String): String? {
        val req = Request.Builder().url(url)
            .header("Authorization", "Bearer $token")
            .header("Origin", "https://music.apple.com")
            .build()
        return client.newCall(req).execute().use { if (it.isSuccessful) it.body?.string() else null }
    }

    private fun get(url: String): String? =
        client.newCall(Request.Builder().url(url).build()).execute()
            .use { if (it.isSuccessful) it.body?.string() else null }

    private fun String.encode() = java.net.URLEncoder.encode(this, "UTF-8")
}
