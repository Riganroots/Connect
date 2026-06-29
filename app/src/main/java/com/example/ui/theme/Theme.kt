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

private val DarkColorScheme =
  darkColorScheme(
    primary = ConnectMint,
    secondary = ConnectLightGreen,
    tertiary = ConnectCream,
    background = ConnectGrayDark,
    surface = Color(0xFF1F2937),
    onPrimary = ConnectDarkGreen,
    onSecondary = ConnectWhite,
    onBackground = ConnectWhite,
    onSurface = ConnectWhite
  )

private val LightColorScheme =
  lightColorScheme(
    primary = ConnectDarkGreen,
    secondary = ConnectMidGreen,
    tertiary = ConnectLightGreen,
    background = ConnectMintLight, // #FCFDFB Pristine Cream-white
    surface = ConnectMintLight,
    surfaceVariant = ConnectCream, // #F0F5F0 Pebble/Sage input card
    onPrimary = ConnectWhite,
    onSecondary = ConnectWhite,
    onTertiary = ConnectWhite,
    onBackground = ConnectGrayDark, // #191C19 Primary Charcoal
    onSurface = ConnectGrayDark,
    outline = ConnectGrayLight // #E6F0E6 Moss silver subtle border
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disable dynamic colors to keep cohesive branding as requested
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
