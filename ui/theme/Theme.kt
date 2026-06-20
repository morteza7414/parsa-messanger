package com.example.parsamessenger.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = BluePrimary,
    background = BgDark,
    surface = CardDark,
    onBackground = Color.White,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2C2F33).copy(alpha = 0.6f),
    onSurfaceVariant = Color.LightGray
)

private val LightColorScheme = lightColorScheme(
    primary = BluePrimary,
    background = BgLight,
    surface = CardLight,
    onBackground = Color.Black,
    onSurface = Color.Black,
    surfaceVariant = Color.White.copy(alpha = 0.5f),
    onSurfaceVariant = Color.DarkGray
)

@Composable
fun ParsaMessengerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
