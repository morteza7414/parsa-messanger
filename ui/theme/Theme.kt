package com.example.parsamessenger.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/* -------------------- DARK COLORS -------------------- */

private val DarkColorScheme = darkColorScheme(
    primary = BluePrimary,
    onPrimary = Color.White,

    background = BgDark,
    onBackground = Color.White,

    surface = CardDark,
    onSurface = Color.White,

    surfaceVariant = Color(0xFF2A2D32),
    onSurfaceVariant = Color(0xFFB0B3B8),

    outline = Color(0xFF3A3F45)
)

/* -------------------- LIGHT COLORS -------------------- */

private val LightColorScheme = lightColorScheme(
    primary = BluePrimary,
    onPrimary = Color.White,

    background = BgLight,
    onBackground = Color.Black,

    surface = CardLight,
    onSurface = Color.Black,

    surfaceVariant = Color(0xFFF1F3F6),
    onSurfaceVariant = Color(0xFF5F6368),

    outline = Color(0xFFE0E0E0)
)

/* -------------------- THEME -------------------- */

@Composable
fun ParsaMessengerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {

    val colorScheme =
        when {

            // Android 12+ dynamic colors
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme)
                    dynamicDarkColorScheme(context)
                else
                    dynamicLightColorScheme(context)
            }

            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
