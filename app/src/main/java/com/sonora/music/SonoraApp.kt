package com.sonora.music

import android.app.Application
import com.sonora.music.core.config.RemoteConfigRepository
import com.sonora.music.data.source.NewPipeDownloaderImpl
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.schabi.newpipe.extractor.NewPipe
import javax.inject.Inject

@HiltAndroidApp
class SonoraApp : Application() {

    @Inject lateinit var okHttpClient: OkHttpClient
    @Inject lateinit var remoteConfig: RemoteConfigRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        // Initialise NewPipeExtractor for the YouTube Music source.
        NewPipe.init(NewPipeDownloaderImpl(okHttpClient))

        // Pull remote config early so source URLs / enable-flags / update info are fresh.
        appScope.launch { remoteConfig.refresh() }
    }
}
