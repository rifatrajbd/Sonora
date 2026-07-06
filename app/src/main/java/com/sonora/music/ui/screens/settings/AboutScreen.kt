package com.sonora.music.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sonora.music.BuildConfig

/**
 * InnerTune-style About / Credits page: app version, then acknowledgements for every open-source
 * project and reverse-engineered source Sonora is built on. Keep this honest and up to date —
 * it's both good manners and useful documentation of where the music comes from.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(20.dp).verticalScroll(rememberScrollState()),
        ) {
            Text("Sonora", style = MaterialTheme.typography.displayLarge)
            Text(
                "Version ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "A multi-source Material 3 music player, inspired by InnerTune.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )

            Spacer(Modifier.height(24.dp))
            SectionTitle("Sources")
            Credit("YouTube Music", "via NewPipeExtractor (client-spoof + PoToken handling)")
            Credit("Qobuz · Tidal · Amazon Music", "lossless FLAC via a self-hosted squid.wtf backend")
            Credit("Apple Music", "catalog via the anonymous web-token method")
            Credit("JioSaavn", "DRM-free audio via the unofficial JioSaavn API")

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            SectionTitle("Built with")
            Credit("NewPipeExtractor", "TeamNewPipe — GPLv3")
            Credit("Media3 / ExoPlayer", "Android Open Source Project")
            Credit("Jetpack Compose & Material 3", "Google")
            Credit("Coil", "image loading")
            Credit("LrcLib & Kugou", "synchronised lyrics (v1)")

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            SectionTitle("Inspiration")
            Credit("InnerTune", "z-huang — the app this design pays homage to")

            Spacer(Modifier.height(24.dp))
            Text(
                "Sonora accesses several services through unofficial means for personal use. " +
                    "Respect the terms of the services you use.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun Credit(name: String, detail: String) {
    Column(Modifier.padding(vertical = 6.dp)) {
        Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
