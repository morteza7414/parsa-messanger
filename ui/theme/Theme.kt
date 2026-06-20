package com.example.parsamessenger.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(

    primary = Color(0xFF2962FF),

    background = Color(0xFF070B12),

    surface = Color(0xFF111827),

    surfaceVariant = Color(0xFF161E2E)

)

private val LightColors = lightColorScheme(

    primary = Color(0xFF2962FF),

    background = Color(0xFFF7F8FC),

    surface = Color.White,

    surfaceVariant = Color(0xFFF2F4F8)

)

@Composable
fun ParsaMessengerTheme(

    darkTheme:

    Boolean =

        isSystemInDarkTheme(),

    content:

    @Composable
        () -> Unit

) {

    MaterialTheme(

        colorScheme =

            if (darkTheme)

                DarkColors

            else

                LightColors,

        content = content

    )

}