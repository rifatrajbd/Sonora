package com.sonora.music

import android.app.Application
import com.sonora.music.core.config.RemoteConfigRepository
import com.sonora.music.data.settings.ContentLocales
import com.sonora.music.data.settings.SettingsStore
import com.sonora.music.data.source.NewPipeDownloaderImpl
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import javax.inject.Inject

@HiltAndroidApp
class SonoraApp : Application() {

    @Inject lateinit var okHttpClient: OkHttpClient
    @Inject lateinit var remoteConfig: RemoteConfigRepository
    @Inject lateinit var settings: SettingsStore

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        // Initialise NewPipeExtractor for the YouTube Music source, honouring the user's
        // content language/country so trending & search results match their region.
        val s = settings.settings.value
        NewPipe.init(
            NewPipeDownloaderImpl(okHttpClient),
            Localization(ContentLocales.effectiveLanguage(s.contentLanguage)),
            ContentCountry(ContentLocales.effectiveCountry(s.contentCountry)),
        )
        // Re-apply when the user changes the Content settings.
        appScope.launch {
            settings.settings
                .map { it.contentLanguage to it.contentCountry }
                .distinctUntilChanged()
                .collect { (lang, country) ->
                    NewPipe.setupLocalization(
                        Localization(ContentLocales.effectiveLanguage(lang)),
                        ContentCountry(ContentLocales.effectiveCountry(country)),
                    )
                }
        }

        // Pull remote config early so source URLs / enable-flags / update info are fresh.
        appScope.launch { remoteConfig.refresh() }
    }
}
