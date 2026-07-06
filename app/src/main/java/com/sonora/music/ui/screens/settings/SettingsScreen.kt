package com.sonora.music.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenContent: () -> Unit,
    onOpenPlayer: () -> Unit,
    onOpenStorage: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenSources: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            SettingRow(Icons.Rounded.Palette, "Appearance", "Theme, dark mode, tabs, lyrics", onOpenAppearance)
            SettingRow(Icons.Rounded.Language, "Content", "Language, country, explicit content", onOpenContent)
            SettingRow(Icons.Rounded.GraphicEq, "Player and audio", "Quality, queue, normalization, equalizer", onOpenPlayer)
            SettingRow(Icons.Rounded.Storage, "Storage", "Cache & downloads", onOpenStorage)
            SettingRow(Icons.Rounded.Security, "Privacy", "History, screenshots, lyrics providers", onOpenPrivacy)
            SettingRow(Icons.Rounded.Tune, "Music sources", "Enable & configure your providers", onOpenSources)
            SettingRow(Icons.Rounded.Backup, "Backup and restore", "Export or import your library", onOpenBackup)
            SettingRow(Icons.Rounded.Info, "About Sonora", "Version, credits & acknowledgements", onOpenAbout)
        }
    }
}

@Composable
private fun SettingRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    ListItem(
        leadingContent = { Icon(icon, contentDescription = null) },
        headlineContent = { Text(title, style = MaterialTheme.typography.titleMedium) },
        supportingContent = { Text(subtitle) },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 8.dp),
    )
}
