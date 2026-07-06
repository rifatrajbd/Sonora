package com.sonora.music.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sonora.music.core.model.AudioQuality

/**
 * Spotify-style quality badge: "HiFi", "Hi-Res", "HD"… Lossless tiers get the coral accent,
 * lossy tiers a muted outline — a quick visual cue of fidelity on rows and Now Playing.
 */
@Composable
fun QualityChip(quality: AudioQuality, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    val lossless = quality.lossless
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = if (lossless) accent.copy(alpha = 0.16f) else Color.Transparent,
        border = if (lossless) null else BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)),
    ) {
        Text(
            text = quality.chipLabel.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = if (lossless) accent else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

