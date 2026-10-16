package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ProxXQuepusColorScheme = darkColorScheme(
    primary = CyanPrimary,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF004D61),
    onPrimaryContainer = CyanPrimary,
    secondary = VioletSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF381E72),
    onSecondaryContainer = Color(0xFFD0BCFF),
    tertiary = CyanVariant,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkSurfaceBorder,
    error = AccentError,
    onError = Color.Black
)

@Composable
fun ProxXQuepusTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ProxXQuepusColorScheme,
        typography = Typography(),
        content = content
    )
}
