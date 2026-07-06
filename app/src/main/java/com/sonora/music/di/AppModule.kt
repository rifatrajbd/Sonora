package com.sonora.music.di

import android.content.Context
import com.sonora.music.core.config.RemoteConfigRepository
import com.sonora.music.data.db.PlaylistDao
import com.sonora.music.data.db.SongDao
import com.sonora.music.data.db.SonoraDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun json(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Provides @Singleton
    fun okHttp(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        // A realistic UA reduces trivial blocking by the reverse-engineered backends.
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", "Sonora/0.1 (Android)")
                    .build()
            )
        }
        .build()

    @Provides @Singleton @Named("remoteConfigUrl")
    fun remoteConfigUrl(): String = RemoteConfigRepository.CONFIG_URL

    @Provides @Singleton
    fun database(@ApplicationContext ctx: Context): SonoraDatabase = SonoraDatabase.build(ctx)

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    @Provides @Singleton
    fun mediaCache(
        @ApplicationContext ctx: Context,
        settings: com.sonora.music.data.settings.SettingsStore,
    ): androidx.media3.datasource.cache.SimpleCache {
        val dir = java.io.File(ctx.cacheDir, "media")
        val maxMb = settings.settings.value.maxCacheMb
        val evictor = if (maxMb <= 0)
            androidx.media3.datasource.cache.NoOpCacheEvictor()
        else
            androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor(maxMb * 1024L * 1024L)
        return androidx.media3.datasource.cache.SimpleCache(
            dir, evictor, androidx.media3.database.StandaloneDatabaseProvider(ctx),
        )
    }

    @Provides fun songDao(db: SonoraDatabase): SongDao = db.songDao()
    @Provides fun playlistDao(db: SonoraDatabase): PlaylistDao = db.playlistDao()
    @Provides fun historyDao(db: SonoraDatabase): com.sonora.music.data.db.HistoryDao = db.historyDao()
}
