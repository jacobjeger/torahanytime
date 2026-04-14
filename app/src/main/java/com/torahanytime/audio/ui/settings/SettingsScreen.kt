package com.torahanytime.audio.ui.settings

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.torahanytime.audio.ui.theme.TATBlue
import com.torahanytime.audio.ui.theme.TATTextSecondary
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

val Context.settingsDataStore by preferencesDataStore(name = "settings")

object SettingsKeys {
    val DARK_MODE = booleanPreferencesKey("dark_mode")
    val LANGUAGE_FILTER = stringPreferencesKey("language_filter")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val darkMode by context.settingsDataStore.data.map {
        it[SettingsKeys.DARK_MODE] ?: false
    }.collectAsState(initial = false)

    val languageFilter by context.settingsDataStore.data.map {
        it[SettingsKeys.LANGUAGE_FILTER] ?: "All"
    }.collectAsState(initial = "All")

    var showLanguagePicker by remember { mutableStateOf(false) }
    val languages = listOf("All", "English", "Hebrew", "Yiddish", "French", "Spanish")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.focusable()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Dark Mode
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Dark Mode", fontWeight = FontWeight.Medium)
                    Text("Use dark theme", fontSize = 12.sp, color = TATTextSecondary)
                }
                Switch(
                    checked = darkMode,
                    onCheckedChange = { enabled ->
                        scope.launch {
                            context.settingsDataStore.edit { it[SettingsKeys.DARK_MODE] = enabled }
                        }
                    },
                    modifier = Modifier.focusable(),
                    colors = SwitchDefaults.colors(checkedTrackColor = TATBlue)
                )
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Language Filter
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusable()
                    .clickable { showLanguagePicker = true }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Language", fontWeight = FontWeight.Medium)
                    Text("Filter lectures by language", fontSize = 12.sp, color = TATTextSecondary)
                }
                Text(languageFilter, color = TATBlue, fontWeight = FontWeight.Medium)
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        }

        if (showLanguagePicker) {
            AlertDialog(
                onDismissRequest = { showLanguagePicker = false },
                title = { Text("Select Language") },
                text = {
                    Column {
                        languages.forEach { lang ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusable()
                                    .clickable {
                                        scope.launch {
                                            context.settingsDataStore.edit { it[SettingsKeys.LANGUAGE_FILTER] = lang }
                                        }
                                        showLanguagePicker = false
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = lang == languageFilter,
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(selectedColor = TATBlue)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(lang)
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = { TextButton(onClick = { showLanguagePicker = false }) { Text("Cancel") } }
            )
        }
    }
}
