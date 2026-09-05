package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = SkyBluePrimary,
    onPrimary = PureWhite,
    primaryContainer = SkyBlueContainer,
    onPrimaryContainer = DarkText,
    secondary = SkyBlueSecondary,
    onSecondary = PureWhite,
    secondaryContainer = SkyBlueLight,
    onSecondaryContainer = DarkText,
    tertiary = SkyBlueTertiary,
    onTertiary = PureWhite,
    background = PureWhite,
    onBackground = DarkText,
    surface = PureWhite,
    onSurface = DarkText,
    surfaceVariant = SkyBlueContainer,
    onSurfaceVariant = DarkText,
    error = AlertRed,
    onError = PureWhite,
    errorContainer = AlertRedLight,
    onErrorContainer = AlertRedDark,
    outline = SkyBlueBorder
)

private val DarkColorScheme = LightColorScheme

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun PharmaBillProTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) = MyApplicationTheme(darkTheme, dynamicColor, content)

