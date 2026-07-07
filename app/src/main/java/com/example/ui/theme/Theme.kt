package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = RoyalGold,
    secondary = BrightBrass,
    tertiary = NeonCyan,
    background = RichDarkBg,
    surface = CardSurface,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = TextLight,
    onSurface = TextLight,
    surfaceVariant = CardBorder,
    onSurfaceVariant = TextMuted
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
