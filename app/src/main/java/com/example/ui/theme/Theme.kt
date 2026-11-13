package com.example.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = XiaomiOrange,
    onPrimary = Color.White,
    primaryContainer = XiaomiOrangeDark,
    onPrimaryContainer = Color.White,
    secondary = XiaomiOrangeLight,
    onSecondary = Color.Black,
    background = XiaomiSlateDark,
    onBackground = TextPrimaryDark,
    surface = XiaomiSurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = XiaomiSurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = Color(0xFF3B404E)
)

private val LightColorScheme = lightColorScheme(
    primary = XiaomiOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE8DC),
    onPrimaryContainer = XiaomiOrangeDark,
    secondary = XiaomiOrangeDark,
    onSecondary = Color.White,
    background = XiaomiSlateLight,
    onBackground = TextPrimaryLight,
    surface = XiaomiSurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = XiaomiSurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = Color(0xFFD4D8E2)
)

@Composable
fun XiaomiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep Xiaomi branding consistent by default
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
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
