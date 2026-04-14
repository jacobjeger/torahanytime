package com.torahanytime.audio.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.torahanytime.audio.ui.theme.TATBlue

@Composable
fun SleepTimerDialog(
    onDismiss: () -> Unit,
    onSelectMinutes: (Int) -> Unit,
    onSelectEndOfLecture: () -> Unit,
    onCancel: () -> Unit,
    currentTimerMinutes: Int?
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sleep Timer", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                if (currentTimerMinutes != null) {
                    Text("Timer active: $currentTimerMinutes min remaining", color = TATBlue)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onCancel, modifier = Modifier.focusable()) {
                        Text("Cancel Timer", color = MaterialTheme.colorScheme.error)
                    }
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                }
                listOf(15, 30, 45, 60, 90).forEach { mins ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusable()
                            .clickable { onSelectMinutes(mins) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("$mins minutes", fontWeight = FontWeight.Medium)
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusable()
                        .clickable { onSelectEndOfLecture() }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("End of current lecture", fontWeight = FontWeight.Medium)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
