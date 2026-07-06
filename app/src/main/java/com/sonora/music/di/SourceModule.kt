package com.sonora.music.di

import com.sonora.music.core.config.RemoteConfigRepository
import com.sonora.music.core.source.MusicSource
import com.sonora.music.data.source.AmazonMusicSource
import com.sonora.music.data.source.AppleMusicSource
import com.sonora.music.data.source.JioSaavnSource
import com.sonora.music.data.source.QobuzSource
import com.sonora.music.data.source.TidalSource
import com.sonora.music.data.source.YouTubeSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import javax.inject.Singleton

/**
 * Registers every [MusicSource] into a single Set that [SourceResolver] consumes. Adding a new
 * source later = add one line here. The three squid.wtf-backed sources (Qobuz/Tidal/Amazon)
 * share the OkHttpClient + config; YouTube uses NewPipeExtractor; JioSaavn is the fallback.
 */
@Module
@InstallIn(SingletonComponent::class)
object SourceModule {

    @Provides @Singleton @ElementsIntoSet
    fun provideSources(
        client: OkHttpClient,
        json: Json,
        config: RemoteConfigRepository,
        youtube: YouTubeSource,
        apple: AppleMusicSource,
        jiosaavn: JioSaavnSource,
    ): Set<@JvmSuppressWildcards MusicSource> = setOf(
        // Lossless trio on one self-hosted squid.wtf backend
        QobuzSource(client, json, config),
        TidalSource(client, json, config),
        AmazonMusicSource(client, json, config),
        // YouTube Music (NewPipeExtractor client-spoof) + Apple + reliable JioSaavn fallback
        youtube,
        apple,
        jiosaavn,
    )
}
