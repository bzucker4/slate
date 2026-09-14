package com.bzucker4.slate.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF2C2C2C),
    onPrimary = Color(0xFFF5F1EA),
    background = Color(0xFFF5F1EA),
    onBackground = Color(0xFF1C1C1C),
    surface = Color(0xFFF5F1EA),
    onSurface = Color(0xFF1C1C1C),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE8E4DC),
    onPrimary = Color(0xFF1C1C1C),
    background = Color(0xFF121212),
    onBackground = Color(0xFFF5F1EA),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFF5F1EA),
)

@Composable
fun SlateTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
