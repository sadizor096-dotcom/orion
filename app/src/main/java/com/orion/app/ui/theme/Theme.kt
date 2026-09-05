package com.orion.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val OrionColorScheme = darkColorScheme(
    primary = CoreBlue,
    secondary = CoreCyan,
    background = VoidBlack,
    surface = PanelDark,
    onBackground = InkPrimary,
    onSurface = InkPrimary,
    error = AlertRed
)

@Composable
fun OrionTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = OrionColorScheme,
        typography = OrionTypography,
        content = content
    )
}
