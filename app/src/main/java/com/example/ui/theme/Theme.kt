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

private val LightColorScheme = lightColorScheme(
  primary = EmeraldPrimary, // Flame Orange
  onPrimary = EmeraldOnPrimary, // Crisp White
  primaryContainer = EmeraldContainer, // Soft Orange
  onPrimaryContainer = EmeraldOnContainer, // Dark Charcoal / Black
  secondary = Color(0xFF121212), // Jet Black
  onSecondary = Color(0xFFFFFFFF), // Crisp White
  secondaryContainer = Color(0xFFF4F4F5),
  onSecondaryContainer = Color(0xFF121212),
  tertiary = Color(0xFFD32F2F), // Crimson Red
  tertiaryContainer = Color(0xFFFFCDD2),
  onTertiaryContainer = Color(0xFF410002),
  background = AppBackground, // Crisp Pure White
  surface = AppSurface, // Crisp Pure White
  surfaceVariant = AppSurfaceVariant,
  outline = AppOutline
)

private val DarkColorScheme = darkColorScheme(
  primary = Color(0xFFFF7043), // Vibrant Flame Orange
  onPrimary = Color(0xFF000000), // Jet Black
  primaryContainer = Color(0xFF3E1200),
  onPrimaryContainer = Color(0xFFFFE0B2),
  secondary = Color(0xFFFFFFFF), // White
  onSecondary = Color(0xFF000000), // Black
  secondaryContainer = Color(0xFF27272A),
  onSecondaryContainer = Color(0xFFFFFFFF),
  tertiary = Color(0xFFEF5350), // Red
  tertiaryContainer = Color(0xFF410002),
  onTertiaryContainer = Color(0xFFFFCDD2),
  background = Color(0xFF121212), // Jet Black
  surface = Color(0xFF18181B), // Dark Black Surface
  surfaceVariant = Color(0xFF27272A),
  outline = Color(0xFF3F3F46)
)

@Composable
fun HakeemTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
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

