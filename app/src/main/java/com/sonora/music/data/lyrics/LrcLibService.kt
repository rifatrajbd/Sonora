package com.sonora.music.data.lyrics

import com.sonora.music.core.model.LyricLine
import com.sonora.music.core.model.Lyrics
import com.sonora.music.core.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Synchronised lyrics from LrcLib (https://lrclib.net) — a free, open, no-auth lyrics API.
 * Provider-agnostic: works for a track from any source, matched on title + artist (+ album,
 * duration when available for a better hit). Returns synced LRC when available, else plain text.
 *
 * The exact `get` and the fuzzy `search` are fired in parallel with a short call timeout, so a
 * miss on the exact endpoint doesn't serialise into a second round-trip.
 */
@Singleton
class LrcLibService @Inject constructor(
    client: OkHttpClient,
    private val json: Json,
) {
    // Lyrics are a nice-to-have: cap the whole call so a slow API never blocks the panel long.
    private val client = client.newBuilder()
        .callTimeout(8, TimeUnit.SECONDS)
        .connectTimeout(5, TimeUnit.SECONDS)
        .build()

    suspend fun fetch(track: Track): Lyrics? = withContext(Dispatchers.IO) {
        val getUrl = buildString {
            append("https://lrclib.net/api/get?")
            append("track_name=").append(track.title.enc())
            append("&artist_name=").append(track.artistName.enc())
            track.albumTitle?.let { append("&album_name=").append(it.enc()) }
            if (track.durationMs > 0) append("&duration=").append(track.durationMs / 1000)
        }
        val searchUrl =
            "https://lrclib.net/api/search?track_name=${track.title.enc()}&artist_name=${track.artistName.enc()}"

        // Race both endpoints; prefer the exact hit, fall back to the first search result.
        coroutineScope {
            val exact = async { get(getUrl) }
            val fuzzy = async { get(searchUrl)?.let { firstFromSearch(it) } }
            val body = exact.await() ?: fuzzy.await() ?: return@coroutineScope null
            fuzzy.cancel()
            parse(body)
        }
    }

    private fun parse(body: String): Lyrics? {
        val obj = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return null
        val synced = obj["syncedLyrics"]?.jsonPrimitive?.contentOrNull()
        val plain = obj["plainLyrics"]?.jsonPrimitive?.contentOrNull()
        return when {
            !synced.isNullOrBlank() -> Lyrics(parseLrc(synced), synced = true, sourceName = "LrcLib", raw = synced)
            !plain.isNullOrBlank() -> Lyrics(
                plain.lines().map { LyricLine(-1, it) }, synced = false, sourceName = "LrcLib", raw = plain,
            )
            else -> null
        }
    }

    /** Search returns an array; take the first result's JSON object as the body to parse. */
    private fun firstFromSearch(body: String): String? = runCatching {
        val arr = json.parseToJsonElement(body)
        val first = (arr as? kotlinx.serialization.json.JsonArray)?.firstOrNull() ?: return null
        first.toString()
    }.getOrNull()

    private fun get(url: String): String? = runCatching {
        client.newCall(Request.Builder().url(url).header("User-Agent", "Sonora").build())
            .execute().use { if (it.isSuccessful) it.body?.string() else null }
    }.getOrNull()

    private fun String.enc() = java.net.URLEncoder.encode(this, "UTF-8")
    private fun kotlinx.serialization.json.JsonPrimitive.contentOrNull(): String? =
        if (this is kotlinx.serialization.json.JsonNull) null else content

    companion object {
        private val LINE = Regex("""\[(\d{2}):(\d{2})[.:](\d{2,3})]\s?(.*)""")

        /** Parse an LRC string into time-stamped lines, sorted by time. */
        fun parseLrc(lrc: String): List<LyricLine> = lrc.lines().mapNotNull { raw ->
            val m = LINE.find(raw) ?: return@mapNotNull null
            val (mm, ss, frac, text) = m.destructured
            val fracMs = frac.padEnd(3, '0').take(3).toLong()
            val startMs = mm.toLong() * 60_000 + ss.toLong() * 1_000 + fracMs
            LyricLine(startMs, text.trim())
        }.sortedBy { it.startMs }
    }
}
