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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.sonora.music.data.settings.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScaffold(title: String, onBack: () -> Unit, content: @Composable (Modifier) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        content(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()))
    }
}

@Composable
fun AppearanceScreen(onBack: () -> Unit, vm: SettingsViewModel = hiltViewModel()) {
    val s by vm.settings.collectAsStateWithLifecycle()
    SettingsScaffold("Appearance", onBack) { mod ->
        Column(mod) {
            SectionLabel("Theme")
            ThemeMode.entries.forEach { mode ->
                RadioRow(
                    label = mode.name.lowercase().replaceFirstChar { it.uppercase() },
                    selected = s.themeMode == mode,
                    onClick = { vm.setThemeMode(mode) },
                )
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SwitchRow("Pure black (AMOLED)", "True-black background in dark mode", s.pureBlack, vm::setPureBlack)
            SwitchRow("Dynamic color", "Use colors from your wallpaper (Android 12+)", s.dynamicColor, vm::setDynamicColor)
        }
    }
}

@Composable
fun AudioQualityScreen(onBack: () -> Unit, vm: SettingsViewModel = hiltViewModel()) {
    val s by vm.settings.collectAsStateWithLifecycle()
    SettingsScaffold("Audio quality", onBack) { mod ->
        Column(mod) {
            SectionLabel("Preferred quality")
            listOf(
                AudioQuality.HI_RES to "Hi-Res · up to 24-bit",
                AudioQuality.LOSSLESS to "HiFi · lossless",
                AudioQuality.HIGH to "HD · 320 kbps",
                AudioQuality.NORMAL to "Normal · saves data",
            ).forEach { (q, desc) ->
                RadioRow(
                    label = q.chipLabel,
                    subtitle = desc,
                    selected = s.audioQuality == q,
                    onClick = { vm.setAudioQuality(q) },
                )
            }
        }
    }
}

@Composable
fun SourcesScreen(onBack: () -> Unit, vm: SettingsViewModel = hiltViewModel()) {
    val s by vm.settings.collectAsStateWithLifecycle()
    SettingsScaffold("Music sources", onBack) { mod ->
        Column(mod.padding(bottom = 24.dp)) {
            SectionLabel("On-device")
            SwitchRow(
                "Sync local music",
                "Include audio files stored on this device in your library",
                s.localSyncEnabled,
                vm::setLocalSyncEnabled,
            )
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SectionLabel("Providers")
            Text(
                "Toggle a source on and paste its backend URL. Sources without a working " +
                    "backend stay off so playback doesn't skip.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            vm.configurableSources.forEachIndexed { i, type ->
                val enabled = s.sourceEnabled[type] ?: (type == com.sonora.music.core.model.SourceType.YOUTUBE_MUSIC || type == com.sonora.music.core.model.SourceType.JIOSAAVN)
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
                        OutlinedTextField(
                            value = url,
                            onValueChange = { vm.setSourceBaseUrl(type, it) },
                            singleLine = true,
                            label = { Text("Backend URL (optional)") },
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        )
                    }
                }
                HorizontalDivider()
            }
        }
    }
}

private fun qualityHint(type: com.sonora.music.core.model.SourceType) = when (type) {
    com.sonora.music.core.model.SourceType.QOBUZ, com.sonora.music.core.model.SourceType.TIDAL -> "Hi-Res"
    com.sonora.music.core.model.SourceType.AMAZON_MUSIC -> "Lossless"
    com.sonora.music.core.model.SourceType.APPLE_MUSIC, com.sonora.music.core.model.SourceType.JIOSAAVN -> "HD"
    else -> "Standard"
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun RadioRow(label: String, subtitle: String? = null, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column(Modifier.padding(start = 8.dp)) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
