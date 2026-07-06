package com.sonora.music.data.download

import android.content.Context
import com.sonora.music.core.model.AudioQuality
import com.sonora.music.core.model.Track
import com.sonora.music.core.source.SourceResolver
import com.sonora.music.data.db.SongDao
import com.sonora.music.data.db.SongEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Simple offline downloader: resolves the best stream (with cross-source failover), streams it to
 * the app's private files dir, then flags the track downloaded in Room with its local path.
 * Playback prefers the local file when present (see PlayerConnection).
 */
@Singleton
class DownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val resolver: SourceResolver,
    private val songDao: SongDao,
    private val client: OkHttpClient,
) {
    private val dir: File by lazy { File(context.filesDir, "downloads").apply { mkdirs() } }

    /** Track ids currently downloading, for in-progress UI. */
    private val _inProgress = MutableStateFlow<Set<String>>(emptySet())
    val inProgress: StateFlow<Set<String>> = _inProgress.asStateFlow()

    suspend fun download(track: Track): Result<Unit> = withContext(Dispatchers.IO) {
        _inProgress.value = _inProgress.value + track.id
        try {
            val stream = resolver.resolveStream(track, AudioQuality.LOSSLESS)
            val ext = when {
                stream.mimeType?.contains("flac") == true -> "flac"
                stream.mimeType?.contains("mp4") == true || stream.mimeType?.contains("m4a") == true -> "m4a"
                stream.mimeType?.contains("mpeg") == true -> "mp3"
                else -> "audio"
            }
            val file = File(dir, "${track.id.sanitize()}.$ext")

            val request = Request.Builder().url(stream.url).apply {
                stream.headers.forEach { (k, v) -> header(k, v) }
            }.build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) error("HTTP ${resp.code}")
                resp.body?.byteStream()?.use { input ->
                    file.outputStream().use { out -> input.copyTo(out) }
                } ?: error("empty body")
            }

            // Ensure the song row exists, then flag it downloaded.
            if (songDao.byId(track.id) == null) songDao.upsert(SongEntity.from(track))
            songDao.setDownloaded(track.id, true, file.absolutePath)
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        } finally {
            _inProgress.value = _inProgress.value - track.id
        }
    }

    suspend fun remove(track: Track) = withContext(Dispatchers.IO) {
        songDao.localPath(track.id)?.let { runCatching { File(it).delete() } }
        songDao.setDownloaded(track.id, false, null)
    }

    private fun String.sanitize() = replace(Regex("[^A-Za-z0-9_-]"), "_").take(80)
}
