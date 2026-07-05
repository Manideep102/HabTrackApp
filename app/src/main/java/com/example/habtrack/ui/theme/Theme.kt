package com.example.habtrack.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// Always-dark "Obsidian" scheme. Dynamic color intentionally disabled so the
// design stays consistent across devices.
private val ObsidianColorScheme = darkColorScheme(
    primary = Obsidian.Accent,
    onPrimary = Obsidian.Bg,
    secondary = Obsidian.TextMid,
    onSecondary = Obsidian.Bg,
    tertiary = Obsidian.Accent,
    background = Obsidian.Bg,
    onBackground = Obsidian.TextHi,
    surface = Obsidian.Surface,
    onSurface = Obsidian.TextHi,
    surfaceVariant = Obsidian.Surface2,
    onSurfaceVariant = Obsidian.TextMid,
    outline = Obsidian.TextLow,
    outlineVariant = Obsidian.Stroke,
    error = androidx.compose.ui.graphics.Color(0xFFF2B5B5),
    surfaceContainerLow = Obsidian.Surface,
    surfaceContainer = Obsidian.Surface,
    surfaceContainerHigh = Obsidian.Surface2,
    surfaceContainerHighest = Obsidian.Surface2,
)

@Composable
fun HabTrackTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ObsidianColorScheme,
        typography = Typography,
        content = content
    )
}
