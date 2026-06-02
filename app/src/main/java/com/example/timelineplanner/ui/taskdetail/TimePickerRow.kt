package com.example.timelineplanner.ui.taskdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TimePickerRow(
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    enabled: Boolean = true
) {
    var hourText by remember { mutableStateOf(String.format("%02d", hour)) }
    var minuteText by remember { mutableStateOf(String.format("%02d", minute)) }
    var hourError by remember { mutableStateOf(false) }
    var minuteError by remember { mutableStateOf(false) }

    LaunchedEffect(hour) {
        if (hourText.toIntOrNull() != hour) {
            hourText = String.format("%02d", hour)
        }
    }
    LaunchedEffect(minute) {
        if (minuteText.toIntOrNull() != minute) {
            minuteText = String.format("%02d", minute)
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = hourText,
            onValueChange = { text ->
                val filtered = text.filter { it.isDigit() }.take(2)
                hourText = filtered
                val parsed = filtered.toIntOrNull()
                if (parsed != null && parsed in 0..23) {
                    hourError = false
                    onHourChange(parsed)
                } else {
                    hourError = filtered.isNotEmpty()
                }
            },
            label = { Text("时") },
            singleLine = true,
            isError = hourError,
            supportingText = if (hourError) {{ Text("0-23") }} else null,
            enabled = enabled,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = ":",
            style = MaterialTheme.typography.titleMedium
        )
        OutlinedTextField(
            value = minuteText,
            onValueChange = { text ->
                val filtered = text.filter { it.isDigit() }.take(2)
                minuteText = filtered
                val parsed = filtered.toIntOrNull()
                if (parsed != null && parsed in 0..59) {
                    minuteError = false
                    onMinuteChange(parsed)
                } else {
                    minuteError = filtered.isNotEmpty()
                }
            },
            label = { Text("分") },
            singleLine = true,
            isError = minuteError,
            supportingText = if (minuteError) {{ Text("0-59") }} else null,
            enabled = enabled,
            modifier = Modifier.weight(1f)
        )
    }
}
