package com.example.timelineplanner.ui.timeline

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.timelineplanner.ui.theme.TimelineGridLine
import com.example.timelineplanner.ui.theme.TimelineHourLine
import com.example.timelineplanner.ui.theme.TimelineHourText

const val BASE_HOUR_HEIGHT_DP = 80f
const val MINUTES_IN_DAY = 24 * 60
const val AXIS_WIDTH_DP = 60f
const val TIMELINE_START_HOUR = 8

@Composable
fun TimeLabels(
    hourHeightDp: Float,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        for (hour in TIMELINE_START_HOUR..23) {
            Box(
                modifier = Modifier
                    .height(hourHeightDp.dp)
                    .width(AXIS_WIDTH_DP.dp)
                    .padding(end = 8.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                Text(
                    text = String.format("%02d:00", hour),
                    style = MaterialTheme.typography.labelSmall,
                    color = TimelineHourText,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
fun TimeGrid(
    hourHeightDp: Float,
    modifier: Modifier = Modifier
) {
    val hourH = with(LocalDensity.current) { hourHeightDp.dp.toPx() }
    val minuteH = hourH / 60f
    val startMinute = TIMELINE_START_HOUR * 60
    val totalMinutes = MINUTES_IN_DAY
    val visibleMinutes = totalMinutes - startMinute

    Canvas(modifier = modifier.fillMaxWidth().height((visibleMinutes * hourHeightDp / 60f).dp)) {
        for (minute in startMinute..totalMinutes step 15) {
            val y = (minute - startMinute) * minuteH
            val isHour = minute % 60 == 0
            drawLine(
                color = if (isHour) TimelineHourLine else TimelineGridLine,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = if (isHour) 2f else 1f,
                pathEffect = if (!isHour) PathEffect.dashPathEffect(
                    floatArrayOf(6f, 4f), 0f
                ) else null
            )
        }
    }
}
