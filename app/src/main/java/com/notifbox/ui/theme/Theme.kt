package com.notifbox.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Palette drawn from the Best-Flutter-UI-Templates design language:
// cool off-white canvas (#F2F3F8), white cards, deep-blue → cyan accent.
object NB {
    val Background = Color(0xFFF2F3F8)
    val DarkBlue = Color(0xFF2633C5)
    val Blue = Color(0xFF00B6F0)
    val Teal = Color(0xFF54D3C2)
    val DarkerText = Color(0xFF17262A)
    val LightText = Color(0xFF4A6572)
    val Grey = Color(0xFF3A5160)
}

private val LightColors = lightColorScheme(
    primary = NB.DarkBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDFE0FF),
    onPrimaryContainer = Color(0xFF030865),
    secondary = NB.Blue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD0F2FF),
    onSecondaryContainer = Color(0xFF001F29),
    tertiary = NB.Teal,
    background = NB.Background,
    onBackground = NB.DarkerText,
    surface = Color.White,
    onSurface = NB.DarkerText,
    surfaceVariant = Color(0xFFEAECF3),
    onSurfaceVariant = NB.LightText,
    outlineVariant = Color(0xFFD9DCE6),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFBEC2FF),
    onPrimary = Color(0xFF12158A),
    primaryContainer = Color(0xFF2B33B0),
    onPrimaryContainer = Color(0xFFE0E0FF),
    secondary = Color(0xFF5BD5FF),
    tertiary = NB.Teal,
    background = Color(0xFF101418),
    onBackground = Color(0xFFE2E2E6),
    surface = Color(0xFF1A1F24),
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = Color(0xFF42474E),
    onSurfaceVariant = Color(0xFFC2C7CF),
    outlineVariant = Color(0xFF42474E),
)

@Composable
fun NotifBoxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = NotifTypography,
        content = content,
    )
}
