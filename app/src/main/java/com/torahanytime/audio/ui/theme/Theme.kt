package com.torahanytime.audio.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val TATBlue = Color(0xFF1B6FA8)
val TATOrange = Color(0xFFE8762D)
val TATBgLight = Color(0xFFF5F7FA)
val TATTextPrimary = Color(0xFF1A1A1A)
val TATTextSecondary = Color(0xFF6B7280)
val TATDivider = Color(0xFFE5E7EB)
val TATBrowseAllText = Color(0xFF9CA3AF)

private val LightColorScheme = lightColorScheme(
    primary = TATBlue,
    onPrimary = Color.White,
    secondary = TATOrange,
    onSecondary = Color.White,
    background = Color.White,
    onBackground = TATTextPrimary,
    surface = Color.White,
    onSurface = TATTextPrimary,
    surfaceVariant = TATBgLight,
    onSurfaceVariant = TATTextSecondary,
    outline = TATDivider
)

@Composable
fun TATTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
