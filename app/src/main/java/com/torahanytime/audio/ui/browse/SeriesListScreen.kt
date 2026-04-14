package com.torahanytime.audio.ui.browse

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.torahanytime.audio.data.api.ApiClient
import com.torahanytime.audio.data.model.Series
import com.torahanytime.audio.ui.theme.TATBlue
import com.torahanytime.audio.ui.theme.TATTextSecondary
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Popular series IDs with known content
private val popularSeriesIds = listOf(
    700,  // Daily Mussar (651 lectures)
    200,  // The Jewish Destiny Series (75)
    900,  // High Holidays (74)
    350,  // Parenting (69)
    8,    // Shemonah Esrei (67)
    25,   // Maximize Your Life (33)
    15,   // Common Questions (32)
    500,  // Mesillat Yesharim (24)
    20,   // Weekly Tefillah Focus: Ahava Rabba (20)
    170,  // Shabbat Series V2.0 (17)
    750,  // The Three Weeks (10)
    450,  // Tefilla Series (10)
    650,  // Untold Story of Megillat Esther (9)
    850,  // The Basics Of Marriage (8)
    550,  // Hagada Insights (8)
    7,    // Six Constant Mitzvot (7)
    55,   // Know Yourself (7)
    75,   // Being B'Simcha (6)
    10,   // War Tactics for the Evil Inclination (4)
    5,    // Watching Your Eyes (3)
)

class SeriesListViewModel : ViewModel() {
    private val api = ApiClient.api

    private val _series = MutableStateFlow<List<Series>>(emptyList())
    val series: StateFlow<List<Series>> = _series

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    init { loadSeries() }

    private fun loadSeries() {
        viewModelScope.launch {
            _loading.value = true
            try {
                val results = popularSeriesIds.map { id ->
                    async {
                        try { api.getSeriesDetail(id) } catch (_: Exception) { null }
                    }
                }.awaitAll().filterNotNull()
                    .filter { it.displayActive && it.lectureCount > 0 }
                    .sortedByDescending { it.lectureCount }
                _series.value = results
            } catch (_: Exception) {}
            _loading.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeriesListScreen(
    onBack: () -> Unit,
    onSeriesClick: (Int) -> Unit = {},
    vm: SeriesListViewModel = viewModel()
) {
    val series by vm.series.collectAsState()
    val loading by vm.loading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Series", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.focusable()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        when {
            loading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = TATBlue)
            }
            series.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No series available", color = TATTextSecondary)
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                items(series, key = { it.id }) { s ->
                    SeriesRow(
                        series = s,
                        onClick = { onSeriesClick(s.id) }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SeriesRow(series: Series, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = series.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp
                ),
                color = if (isFocused) TATBlue else MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${series.lectureCount} lectures",
                style = MaterialTheme.typography.bodySmall,
                color = TATTextSecondary
            )
        }
    }
}
