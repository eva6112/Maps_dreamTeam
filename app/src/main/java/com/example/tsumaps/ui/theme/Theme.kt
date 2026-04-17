package com.example.tsumaps.ui.theme


import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = TsuBlue,
    onPrimary = TsuWhite,
    primaryContainer = TsuLightBlue,
    onPrimaryContainer = TsuWhite,
    secondary = TsuBlue,
    onSecondary = TsuWhite,
    tertiary = TsuLightBlue,
    onTertiary = TsuWhite,
    background = TsuWhite,
    onBackground = TsuDark,
    surface = TsuWhite,
    onSurface = TsuDark,
    surfaceVariant = TsuLightBlue,
    onSurfaceVariant = TsuWhite
)

private val DarkColorScheme = darkColorScheme(
    primary = TsuBlue,
    onPrimary = TsuWhite,
    primaryContainer = TsuDarkBlue,
    onPrimaryContainer = TsuWhite,
    secondary = TsuBlue,
    onSecondary = TsuWhite,
    tertiary = TsuDarkBlue,
    onTertiary = TsuWhite,
    background = TsuDark,
    onBackground = TsuWhite,
    surface = TsuDark,
    onSurface = TsuWhite,
    surfaceVariant = TsuDarkBlue,
    onSurfaceVariant = TsuWhite
)

@Composable
fun TSUMapsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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