package com.sonora.music.core.config

import com.sonora.music.core.model.SourceType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Remote-controlled configuration fetched at launch (and periodically) from a JSON file you
 * host on GitHub (raw.githubusercontent.com) or a tiny Cloudflare Worker. This is the key to
 * updating Sonora WITHOUT publishing a new APK:
 *
 *  - A source's base URL / client_id / app_id changed?  -> edit the JSON, users pick it up.
 *  - A "trick" broke and you need to disable a source?   -> set enabled=false in the JSON.
 *  - Want to reorder which source is preferred?          -> change priority in the JSON.
 *  - A change genuinely needs new app code (e.g. NewPipe bump)? -> bump [minAppVersionCode]
 *    and point [updateUrl] at the new GitHub Release APK so the app can prompt an update.
 */
@Serializable
data class RemoteConfig(
    val configVersion: Int = 1,
    val sources: List<SourceConfig> = emptyList(),
    @SerialName("update") val update: UpdateInfo = UpdateInfo(),
    /** Optional broadcast message shown in-app (maintenance notice, etc.). */
    val notice: String? = null,
) {
    fun forType(type: SourceType): SourceConfig? = sources.firstOrNull { it.type == type }

    companion object {
        /**
         * Safe built-in defaults. Only the providers that work with no user-supplied backend are
         * enabled out of the box (YouTube via NewPipeExtractor, JioSaavn, on-device local). The
         * backend-dependent providers stay OFF until the user configures a base URL — otherwise
         * they fail on every request and cause playback to skip.
         */
        private val ENABLED_BY_DEFAULT = setOf(
            SourceType.YOUTUBE_MUSIC, SourceType.JIOSAAVN, SourceType.LOCAL,
        )
        val DEFAULT = RemoteConfig(
            sources = SourceType.entries.map {
                SourceConfig(type = it, enabled = it in ENABLED_BY_DEFAULT)
            },
        )
    }
}

@Serializable
data class SourceConfig(
    val type: SourceType,
    val enabled: Boolean = true,
    val priority: Int? = null,
    /** Base URL of the proxy/backend that implements this source's trick (e.g. your squid.wtf). */
    val baseUrl: String? = null,
    /** Arbitrary per-source secrets the backend needs: client_id, app_id, region, tokens… */
    val params: Map<String, String> = emptyMap(),
)

@Serializable
data class UpdateInfo(
    /** Users on a lower versionCode than this are asked (or forced) to update. */
    @SerialName("min_version_code") val minAppVersionCode: Int = 0,
    @SerialName("latest_version_code") val latestVersionCode: Int = 0,
    @SerialName("latest_version_name") val latestVersionName: String = "",
    /** Direct link to the GitHub Release APK (Sonora is side-loaded, not on Play Store). */
    @SerialName("apk_url") val apkUrl: String? = null,
    @SerialName("release_notes") val releaseNotes: String? = null,
    /** If true, block usage until updated (for hard-breaking source-protocol changes). */
    val mandatory: Boolean = false,
)
