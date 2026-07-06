package com.sonora.music.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonora.music.core.model.AudioQuality
import com.sonora.music.core.model.SourceType
import com.sonora.music.data.settings.DefaultTab
import com.sonora.music.data.settings.LyricsPosition
import com.sonora.music.data.settings.LyricsProvider
import com.sonora.music.data.settings.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScaffold(title: String, onBack: () -> Unit, content: @Composable (Modifier) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding -> content(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) }
}

// ---------------- Appearance ----------------

@Composable
fun AppearanceScreen(onBack: () -> Unit, vm: SettingsViewModel = hiltViewModel()) {
    val s by vm.settings.collectAsStateWithLifecycle()
    SettingsScaffold("Appearance", onBack) { mod ->
        Column(mod) {
            SwitchRow("Enable dynamic theme", "Use colors from your wallpaper (Android 12+)", s.dynamicColor, vm::setDynamicColor)
            SectionLabel("Dark theme")
            RadioRow("Follow system", selected = s.themeMode == ThemeMode.SYSTEM) { vm.setThemeMode(ThemeMode.SYSTEM) }
            RadioRow("On", selected = s.themeMode == ThemeMode.DARK) { vm.setThemeMode(ThemeMode.DARK) }
            RadioRow("Off", selected = s.themeMode == ThemeMode.LIGHT) { vm.setThemeMode(ThemeMode.LIGHT) }
            SwitchRow("Pure black", "True-black background in dark mode", s.pureBlack, vm::setPureBlack)
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SectionLabel("Default open tab")
            DefaultTab.entries.forEach { t ->
                RadioRow(t.name.lowercase().replaceFirstChar { it.uppercase() }, selected = s.defaultTab == t) { vm.setDefaultTab(t) }
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SectionLabel("Grid cell size")
            RadioRow("Small", selected = !s.gridBig) { vm.setGridBig(false) }
            RadioRow("Big", selected = s.gridBig) { vm.setGridBig(true) }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SectionLabel("Lyrics text position")
            LyricsPosition.entries.forEach { p ->
                RadioRow(p.name.lowercase().replaceFirstChar { it.uppercase() }, selected = s.lyricsPosition == p) { vm.setLyricsPosition(p) }
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SectionLabel("Player slider style")
            RadioRow("Default", selected = !s.squigglySlider) { vm.setSquigglySlider(false) }
            RadioRow("Squiggly", selected = s.squigglySlider) { vm.setSquigglySlider(true) }
        }
    }
}

// ---------------- Content ----------------

@Composable
fun ContentScreen(onBack: () -> Unit, vm: SettingsViewModel = hiltViewModel()) {
    val s by vm.settings.collectAsStateWithLifecycle()
    SettingsScaffold("Content", onBack) { mod ->
        Column(mod.padding(bottom = 24.dp)) {
            SectionLabel("Default content language")
            OutlinedTextField(
                value = s.contentLanguage, onValueChange = vm::setContentLanguage, singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            SectionLabel("Default content country")
            OutlinedTextField(
                value = s.contentCountry, onValueChange = vm::setContentCountry, singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SwitchRow("Hide explicit", "Hide content marked as explicit", s.hideExplicit, vm::setHideExplicit)
        }
    }
}

// ---------------- Player and audio ----------------

@Composable
fun PlayerSettingsScreen(onBack: () -> Unit, vm: SettingsViewModel = hiltViewModel()) {
    val s by vm.settings.collectAsStateWithLifecycle()
    SettingsScaffold("Player and audio", onBack) { mod ->
        Column(mod) {
            SectionLabel("Audio quality")
            listOf(AudioQuality.HI_RES to "Auto / highest available", AudioQuality.HIGH to "High", AudioQuality.NORMAL to "Low")
                .forEach { (q, d) -> RadioRow(q.chipLabel, d, s.audioQuality == q) { vm.setAudioQuality(q) } }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SwitchRow("Persistent queue", "Restore your last queue when the app starts", s.persistentQueue, vm::setPersistentQueue)
            SwitchRow("Auto load more", "Automatically add more songs when the queue ends", s.autoLoadMore, vm::setAutoLoadMore)
            SwitchRow("Skip silence", "Skip silent parts of tracks", s.skipSilence, vm::setSkipSilence)
            SwitchRow("Audio normalization", "Keep a consistent volume across tracks", s.audioNormalization, vm::setAudioNormalization)
            SwitchRow("Auto skip on error", "Ensure your continuous playback experience", s.autoSkipOnError, vm::setAutoSkipOnError)
            SwitchRow("Stop music on task clear", "Stop playback when the app is swiped away", s.stopOnTaskClear, vm::setStopOnTaskClear)
        }
    }
}

// ---------------- Storage ----------------

@Composable
fun StorageScreen(onBack: () -> Unit, vm: SettingsViewModel = hiltViewModel()) {
    val s by vm.settings.collectAsStateWithLifecycle()
    SettingsScaffold("Storage", onBack) { mod ->
        Column(mod) {
            SectionLabel("Max cache size")
            listOf(0 to "Unlimited", 512 to "512 MB", 1024 to "1 GB", 4096 to "4 GB", 8192 to "8 GB")
                .forEach { (mb, label) -> RadioRow(label, selected = s.maxCacheMb == mb) { vm.setMaxCacheMb(mb) } }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Text("Song cache and image cache are managed automatically within the limit above.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp))
        }
    }
}

// ---------------- Privacy ----------------

@Composable
fun PrivacyScreen(onBack: () -> Unit, vm: SettingsViewModel = hiltViewModel()) {
    val s by vm.settings.collectAsStateWithLifecycle()
    SettingsScaffold("Privacy", onBack) { mod ->
        Column(mod.padding(bottom = 24.dp)) {
            SectionLabel("History")
            SwitchRow("Pause listen history", "Stop recording what you play", s.pauseListenHistory, vm::setPauseListenHistory)
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                OutlinedButton(onClick = vm::clearListenHistory) { Text("Clear listen history") }
            }
            SwitchRow("Pause search history", "Stop saving your searches", s.pauseSearchHistory, vm::setPauseSearchHistory)
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SwitchRow("Disable screenshot", "Block screenshots and screen recording", s.disableScreenshot, vm::setDisableScreenshot)
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SectionLabel("Lyrics providers")
            RadioRow("LrcLib", selected = s.lyricsProvider == LyricsProvider.LRCLIB) { vm.setLyricsProvider(LyricsProvider.LRCLIB) }
            RadioRow("KuGou", selected = s.lyricsProvider == LyricsProvider.KUGOU) { vm.setLyricsProvider(LyricsProvider.KUGOU) }
        }
    }
}

// ---------------- Backup and restore ----------------

@Composable
fun BackupScreen(onBack: () -> Unit) {
    SettingsScaffold("Backup and restore", onBack) { mod ->
        Column(mod.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Export your liked songs, playlists and settings, or restore from a backup file.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Backup") }
            OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Restore") }
            Text("Backup file handling is coming soon.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ---------------- Music sources (Sonora-specific) ----------------

@Composable
fun SourcesScreen(onBack: () -> Unit, vm: SettingsViewModel = hiltViewModel()) {
    val s by vm.settings.collectAsStateWithLifecycle()
    SettingsScaffold("Music sources", onBack) { mod ->
        Column(mod.padding(bottom = 24.dp)) {
            SectionLabel("On-device")
            SwitchRow("Sync local music", "Include audio files stored on this device", s.localSyncEnabled, vm::setLocalSyncEnabled)
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SectionLabel("Providers")
            Text("Toggle a source on and paste its backend URL. Sources without a working backend stay off so playback doesn't skip.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            vm.configurableSources.forEachIndexed { i, type ->
                val enabled = s.sourceEnabled[type] ?: (type == SourceType.YOUTUBE_MUSIC || type == SourceType.JIOSAAVN)
                val url = s.sourceBaseUrl[type] ?: ""
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Source ${i + 1}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(qualityHint(type), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = enabled, onCheckedChange = { vm.setSourceEnabled(type, it) })
                    }
                    if (enabled) {
                        OutlinedTextField(url, { vm.setSourceBaseUrl(type, it) }, singleLine = true,
                            label = { Text("Backend URL (optional)") }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
                    }
                }
                HorizontalDivider()
            }
        }
    }
}

private fun qualityHint(type: SourceType) = when (type) {
    SourceType.QOBUZ, SourceType.TIDAL -> "Hi-Res"
    SourceType.AMAZON_MUSIC -> "Lossless"
    SourceType.APPLE_MUSIC, SourceType.JIOSAAVN -> "HD"
    else -> "Standard"
}

// ---------------- shared rows ----------------

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp))
}

@Composable
private fun RadioRow(label: String, subtitle: String? = null, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onClick)
        Column(Modifier.padding(start = 8.dp)) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
