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

/**
 * Base for the three lossless sources that ride on ONE self-hosted squid.wtf backend:
 * Qobuz, Tidal and Amazon Music. squid.wtf implements each service's reverse-engineered
 * "trick" server-side and exposes a single clean API returning FLAC up to 24-bit/192kHz.
 *
 * Because all the fragile auth lives on YOUR server, when a trick breaks you patch the server
 * (or flip [baseUrl] via RemoteConfig) — the app keeps working with no reinstall.
 *
 * NOTE: The exact JSON shape depends on your squid.wtf build; the request/response glue below
 * is a skeleton — wire it to your instance's `/search` and `/track` endpoints.
 */
abstract class SquidWtfSource(
    final override val type: SourceType,
    private val client: OkHttpClient,
    private val json: Json,
    private val config: RemoteConfigRepository,
) : MusicSource {

    /** squid.wtf provider slug, e.g. "qobuz", "tidal", "amazon". */
    protected abstract val provider: String

    private val cfg get() = config.config.value.forType(type)

    override val enabled: Boolean get() = cfg?.enabled ?: true
    override val priority: Int
        get() = cfg?.priority ?: SourceResolver.PRIORITY_DEFAULTS[type] ?: 0

    private val baseUrl: String
        get() = (cfg?.baseUrl ?: DEFAULT_BASE).trimEnd('/')

    override suspend fun search(query: String): SearchResults = withContext(Dispatchers.IO) {
        val url = "$baseUrl/api/search?provider=$provider&q=${query.encode()}"
        val body = get(url) ?: return@withContext SearchResults()
        // TODO: map your squid.wtf search schema -> SearchResults. Skeleton keeps compile-safe.
        parseSearch(body)
    }

    override suspend fun getStream(track: Track, preferred: AudioQuality): StreamInfo =
        withContext(Dispatchers.IO) {
            val quality = if (preferred.lossless) "lossless" else "high"
            val url = "$baseUrl/api/track?provider=$provider&id=${track.sourceId}&quality=$quality"
            val body = get(url) ?: error("$type: empty track response")
            parseStream(body, preferred)
        }

    override suspend fun isHealthy(): Boolean = withContext(Dispatchers.IO) {
        enabled && runCatching { get("$baseUrl/api/health") != null }.getOrDefault(false)
    }

    // --- helpers ---------------------------------------------------------------

    protected fun get(url: String): String? {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { resp ->
            return if (resp.isSuccessful) resp.body?.string() else null
        }
    }

    /** Override per-provider if the search schema differs; default returns empty (skeleton). */
    protected open fun parseSearch(body: String): SearchResults = SearchResults()

    /** Override per-provider; default expects a top-level {"url": "...", "bitDepth":..,"sampleRate":..}. */
    protected open fun parseStream(body: String, preferred: AudioQuality): StreamInfo {
        // Skeleton parse — replace with your squid.wtf response mapping.
        val obj = runCatching { json.parseToJsonElement(body) }.getOrNull()
        val url = obj?.let { it.toString() } // placeholder
        error("Implement ${type.name} parseStream() against your squid.wtf response; got: ${body.take(120)}")
    }

    protected fun String.encode(): String = java.net.URLEncoder.encode(this, "UTF-8")

    companion object {
        // Fallback if RemoteConfig has no baseUrl. Override via config for your own instance.
        const val DEFAULT_BASE = "https://squid.wtf"
    }
}

class QobuzSource(client: OkHttpClient, json: Json, config: RemoteConfigRepository) :
    SquidWtfSource(SourceType.QOBUZ, client, json, config) {
    override val provider = "qobuz"
}

class TidalSource(client: OkHttpClient, json: Json, config: RemoteConfigRepository) :
    SquidWtfSource(SourceType.TIDAL, client, json, config) {
    override val provider = "tidal"
}

class AmazonMusicSource(client: OkHttpClient, json: Json, config: RemoteConfigRepository) :
    SquidWtfSource(SourceType.AMAZON_MUSIC, client, json, config) {
    override val provider = "amazon"
}
