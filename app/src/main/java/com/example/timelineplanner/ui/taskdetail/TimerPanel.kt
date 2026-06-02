package com.example.timelineplanner.ui.taskdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.timelineplanner.ui.timeline.minuteToTimeString

@Composable
fun TimerPanel(
    timerState: TimerState,
    elapsedDisplay: String,
    pauseSegments: List<Pair<Int, Int>>,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onEnd: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = elapsedDisplay,
            style = MaterialTheme.typography.displaySmall,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = if (timerState == TimerState.PAUSED)
                MaterialTheme.colorScheme.onSurfaceVariant
            else
                MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = when (timerState) {
                TimerState.RUNNING -> "计时中..."
                TimerState.PAUSED -> "已暂停"
                TimerState.ENDED -> "计时结束"
                else -> ""
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (pauseSegments.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            pauseSegments.forEachIndexed { index, (start, end) ->
                Text(
                    text = "暂停 ${index + 1}: ${minuteToTimeString(start)} - ${minuteToTimeString(end)} (${(end - start)}分钟)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (timerState) {
                TimerState.RUNNING -> {
                    IconButton(
                        onClick = onPause,
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.Pause,
                            contentDescription = "暂停",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    IconButton(
                        onClick = onEnd,
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = MaterialTheme.colorScheme.error,
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.Stop,
                            contentDescription = "结束",
                            tint = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                TimerState.PAUSED -> {
                    IconButton(
                        onClick = onResume,
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "继续",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    IconButton(
                        onClick = onEnd,
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = MaterialTheme.colorScheme.error,
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.Stop,
                            contentDescription = "结束",
                            tint = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                TimerState.ENDED -> {
                    Button(onClick = onSave) {
                        Text("保存计时结果")
                    }
                }
                else -> {}
            }
        }
    }
}
