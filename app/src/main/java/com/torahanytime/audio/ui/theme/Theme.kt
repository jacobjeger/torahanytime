package com.torahanytime.audio.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.torahanytime.audio.ui.settings.SettingsKeys
import com.torahanytime.audio.ui.settings.settingsDataStore
import kotlinx.coroutines.flow.map

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

private val DarkColorScheme = darkColorScheme(
    primary = TATBlue,
    onPrimary = Color.White,
    secondary = TATOrange,
    onSecondary = Color.White,
    background = Color(0xFF121212),
    onBackground = Color(0xFFE0E0E0),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFF9E9E9E),
    outline = Color(0xFF3A3A3A)
)

@Composable
fun TATTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val darkMode by context.settingsDataStore.data.map {
        it[SettingsKeys.DARK_MODE] ?: false
    }.collectAsState(initial = false)

    val colorScheme = if (darkMode) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
