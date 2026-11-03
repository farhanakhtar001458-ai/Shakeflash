package com.shakeflash.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ShakeFlashDarkScheme = darkColorScheme(
    primary = SFColors.Primary,
    onPrimary = SFColors.OnPrimary,
    primaryContainer = SFColors.SurfaceElevated,
    onPrimaryContainer = SFColors.Foreground,
    secondary = SFColors.Accent,
    onSecondary = SFColors.OnPrimary,
    background = SFColors.Background,
    onBackground = SFColors.Foreground,
    surface = SFColors.Surface,
    onSurface = SFColors.Foreground,
    surfaceVariant = SFColors.SurfaceElevated,
    onSurfaceVariant = SFColors.Muted,
    outline = SFColors.Border,
    outlineVariant = SFColors.BorderStrong,
    error = SFColors.Danger,
    onError = Color(0xFF2B0B0B),
    scrim = Color(0xCC000000),
)

/**
 * ShakeFlash is intentionally dark-only: a flashlight app is used in the dark, and a
 * white flash from the UI would ruin night vision.
 */
@Composable
fun ShakeFlashTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ShakeFlashDarkScheme,
        typography = SFTypography,
        content = content,
    )
}
