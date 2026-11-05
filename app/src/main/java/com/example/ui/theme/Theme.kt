package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ApocalipsisColorScheme = darkColorScheme(
  primary = WastelandOrange,
  onPrimary = WastelandDarkBg,
  primaryContainer = WastelandOrangeDark,
  onPrimaryContainer = WastelandText,
  secondary = WastelandGold,
  onSecondary = WastelandDarkBg,
  background = WastelandDarkBg,
  onBackground = WastelandText,
  surface = WastelandSurface,
  onSurface = WastelandText,
  surfaceVariant = WastelandSurfaceVariant,
  onSurfaceVariant = WastelandTextSecondary,
  error = WastelandRed,
  onError = WastelandText
)

@Composable
fun MyApplicationTheme(
  content: @Composable () -> Unit
) {
  MaterialTheme(
    colorScheme = ApocalipsisColorScheme,
    typography = Typography,
    content = content
  )
}
