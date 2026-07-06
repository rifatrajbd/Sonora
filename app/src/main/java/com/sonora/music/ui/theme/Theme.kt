package com.sonora.music.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val SonoraDark = darkColorScheme(
    primary = SonoraTeal,
    onPrimary = Ink,
    secondary = SonoraCyan,
    background = Ink,
    onBackground = OnInk,
    surface = InkElevated,
    onSurface = OnInk,
    surfaceVariant = InkSurface,
    onSurfaceVariant = OnInkMuted,
)

private val SonoraLight = lightColorScheme(
    primary = SonoraTealDeep,
    onPrimary = Color.White,
    secondary = SonoraTeal,
    background = Paper,
    onBackground = OnPaper,
    surface = Paper,
    onSurface = OnPaper,
)

/**
 * Material 3 Expressive theme. When [dynamicColor] is on (Android 12+) the palette is derived
 * from the wallpaper; the Now Playing screen additionally recolors from album art at runtime.
 * [amoled] forces a true-black background for OLED screens.
 */
@Composable
fun SonoraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    amoled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val scheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> SonoraDark
        else -> SonoraLight
    }.let { s ->
        if (darkTheme && amoled) s.copy(background = Ink, surface = Ink) else s
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = SonoraTypography,
    ) {
        // Root Surface so LocalContentColor defaults to onBackground instead of black —
        // without it, any Text with no explicit color is invisible in dark theme.
        androidx.compose.material3.Surface(
            color = scheme.background,
            contentColor = scheme.onBackground,
            content = content,
        )
    }
}
