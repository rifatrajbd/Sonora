package com.sonora.music.core.config

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Fetches and caches [RemoteConfig]. Point [CONFIG_URL] at a raw GitHub file you control, e.g.
 * https://raw.githubusercontent.com/<you>/sonora-config/main/config.json
 *
 * Editing that file is how you hot-fix sources, toggle a broken trick, or announce an update —
 * all without users reinstalling the app.
 */
@Singleton
class RemoteConfigRepository @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json,
    @Named("remoteConfigUrl") private val configUrl: String,
) {
    private val _config = MutableStateFlow(RemoteConfig.DEFAULT)
    val config: StateFlow<RemoteConfig> = _config.asStateFlow()

    /** Refresh from remote; silently keeps last good config on failure. */
    suspend fun refresh(): RemoteConfig = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url(configUrl).build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) error("HTTP ${resp.code}")
                val body = resp.body?.string().orEmpty()
                json.decodeFromString<RemoteConfig>(body)
            }
        }.onSuccess { _config.value = it }
            .onFailure { Log.w(TAG, "remote config fetch failed, using cached: ${it.message}") }
            .getOrDefault(_config.value)
    }

    companion object {
        private const val TAG = "RemoteConfig"
        // TODO: replace with your GitHub raw config URL once the repo exists.
        const val CONFIG_URL = "https://raw.githubusercontent.com/CHANGE_ME/sonora-config/main/config.json"
    }
}
