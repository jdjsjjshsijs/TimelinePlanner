package com.example.timelineplanner.ui.summary

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import com.example.timelineplanner.ui.timeline.parseColor
import com.example.timelineplanner.util.formatDateLabel
import com.example.timelineplanner.util.ONE_DAY_MILLIS
import com.example.timelineplanner.util.todayStartMillis
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailySummaryScreen(
    viewModel: DailySummaryViewModel,
    modifier: Modifier = Modifier
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val summaryItems by viewModel.summaryItems.collectAsState()
    val totalTaskMinutes by viewModel.totalTaskMinutes.collectAsState()
    val period by viewModel.period.collectAsState()

    var showDatePicker by remember { mutableStateOf(false) }

    val todayStart = remember { todayStartMillis() }
    val isToday = selectedDate == todayStart
    val dateLabel = remember(selectedDate, period) {
        when (period) {
            SummaryPeriod.DAY -> formatDateLabel(selectedDate)
            SummaryPeriod.WEEK -> {
                val cal = java.util.Calendar.getInstance().apply { timeInMillis = selectedDate }
                cal.set(java.util.Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                val weekStart = formatDateLabel(cal.timeInMillis)
                cal.add(java.util.Calendar.DAY_OF_WEEK, 6)
                val weekEnd = formatDateLabel(cal.timeInMillis)
                "$weekStart ~ $weekEnd"
            }
            SummaryPeriod.MONTH -> {
                val cal = java.util.Calendar.getInstance().apply { timeInMillis = selectedDate }
                "${cal.get(java.util.Calendar.YEAR)}年${cal.get(java.util.Calendar.MONTH) + 1}月"
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // 日期选择栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = {
                val delta = when (period) {
                    SummaryPeriod.DAY -> ONE_DAY_MILLIS
                    SummaryPeriod.WEEK -> 7 * ONE_DAY_MILLIS
                    SummaryPeriod.MONTH -> {
                        val cal = java.util.Calendar.getInstance().apply { timeInMillis = selectedDate }
                        cal.add(java.util.Calendar.MONTH, -1)
                        cal.timeInMillis - selectedDate
                    }
                }
                viewModel.selectDate(selectedDate - delta)
            }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "前一${period.label}")
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { showDatePicker = true }
            ) {
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (isToday) {
                        Text(
                            text = "今天",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            IconButton(onClick = {
                val delta = when (period) {
                    SummaryPeriod.DAY -> ONE_DAY_MILLIS
                    SummaryPeriod.WEEK -> 7 * ONE_DAY_MILLIS
                    SummaryPeriod.MONTH -> {
                        val cal = java.util.Calendar.getInstance().apply { timeInMillis = selectedDate }
                        cal.add(java.util.Calendar.MONTH, 1)
                        cal.timeInMillis - selectedDate
                    }
                }
                viewModel.selectDate(selectedDate + delta)
            }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "后一${period.label}")
            }
        }

        // 周期切换
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            SummaryPeriod.entries.forEach { p ->
                val selected = period == p
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (selected) MaterialTheme.colorScheme.primaryContainer
                            else Color.Transparent
                        )
                        .clickable { viewModel.selectPeriod(p) }
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = p.label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (summaryItems.isEmpty()) {
            // 空状态
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无任务",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 圆环图
                item {
                    DonutChart(
                        items = summaryItems,
                        totalMinutes = totalTaskMinutes,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    )
                }

                // 统计摘要
                item {
                    val hours = totalTaskMinutes / 60
                    val mins = totalTaskMinutes % 60
                    val summaryText = if (period == SummaryPeriod.DAY) {
                        val freeMinutes = 24 * 60 - totalTaskMinutes
                        val freeHours = freeMinutes / 60
                        val freeMins = freeMinutes % 60
                        "已安排 ${hours}时${mins}分  |  空闲 ${freeHours}时${freeMins}分"
                    } else {
                        val days = when (period) {
                            SummaryPeriod.WEEK -> 7
                            SummaryPeriod.MONTH -> {
                                val cal = java.util.Calendar.getInstance().apply { timeInMillis = selectedDate }
                                cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
                            }
                            else -> 1
                        }
                        val avgPerDay = if (days > 0) totalTaskMinutes / days else 0
                        val avgH = avgPerDay / 60
                        val avgM = avgPerDay % 60
                        "共 ${hours}时${mins}分  |  日均 ${avgH}时${avgM}分"
                    }
                    Text(
                        text = summaryText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                // 任务列表
                items(summaryItems) { item ->
                    TaskSummaryRow(item = item)
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { utcMillis ->
                        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                            timeInMillis = utcMillis
                        }
                        val localCal = Calendar.getInstance().apply {
                            clear()
                            set(utcCal.get(Calendar.YEAR), utcCal.get(Calendar.MONTH), utcCal.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        viewModel.selectDate(localCal.timeInMillis)
                    }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun DonutChart(
    items: List<TaskSummaryItem>,
    totalMinutes: Int,
    modifier: Modifier = Modifier
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    var selectedIndex by remember(items) { mutableIntStateOf(-1) }
    val animProgress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    val durationText = remember(items, selectedIndex) {
        if (selectedIndex in items.indices) {
            val item = items[selectedIndex]
            val h = item.durationMinutes / 60
            val m = item.durationMinutes % 60
            if (h > 0) "${h}时${m}分" else "${m}分"
        } else ""
    }

    Box(modifier = modifier.size(300.dp), contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(items) {
                    detectTapGestures { offset ->
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val dx = offset.x - cx
                        val dy = offset.y - cy
                        val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                        val strokeWidthPx = 36.dp.toPx()
                        val dim = minOf(size.width, size.height).toFloat()
                        val outerR = (dim - strokeWidthPx) / 2
                        val innerR = outerR - strokeWidthPx

                        if (dist < innerR || dist > outerR + strokeWidthPx / 2) return@detectTapGestures

                        val angle = (Math.toDegrees(atan2(dy, dx).toDouble()).toFloat() + 90f + 360f) % 360f

                        var acc = 0f
                        var found = -1
                        for (i in items.indices) {
                            val sweep = items[i].durationMinutes.toFloat() / totalMinutes * 360f
                            if (angle >= acc && angle < acc + sweep) {
                                found = i
                                break
                            }
                            acc += sweep
                        }

                        if (found == selectedIndex) {
                            selectedIndex = -1
                            scope.launch { animProgress.animateTo(0f, spring()) }
                        } else {
                            selectedIndex = found
                            scope.launch { animProgress.animateTo(1f, spring()) }
                        }
                    }
                }
        ) {
            val baseStroke = 36.dp.toPx()
            val selectedStroke = 42.dp.toPx()
            val radius = (size.minDimension - baseStroke) / 2
            val center = Offset(size.width / 2f, size.height / 2f)

            // 背景环
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = baseStroke, cap = StrokeCap.Butt)
            )

            // 各任务弧段
            var startAngle = -90f
            val segmentData = mutableListOf<Triple<Float, Float, TaskSummaryItem>>()

            for ((index, item) in items.withIndex()) {
                val sweep = item.durationMinutes.toFloat() / totalMinutes * 360f
                val isSelected = index == selectedIndex
                val currentStroke = if (isSelected) {
                    baseStroke + (selectedStroke - baseStroke) * animProgress.value
                } else {
                    baseStroke
                }

                drawArc(
                    color = parseColor(item.color),
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = currentStroke, cap = StrokeCap.Butt)
                )

                segmentData.add(Triple(startAngle, sweep, item))
                startAngle += sweep
            }

            // 选中时画标注
            if (selectedIndex in items.indices && animProgress.value > 0.1f) {
                val item = items[selectedIndex]
                val segStart = segmentData[selectedIndex].first
                val segSweep = segmentData[selectedIndex].second
                val midAngleDeg = segStart + segSweep / 2f
                val midAngleRad = Math.toRadians(midAngleDeg.toDouble())

                val outerEdge = radius + selectedStroke / 2

                // 线起点：圆环外边缘
                val startX = center.x + cos(midAngleRad).toFloat() * outerEdge
                val startY = center.y + sin(midAngleRad).toFloat() * outerEdge

                // 折线：先径向延伸，再水平延伸到右侧固定位置
                val extendLen = 24.dp.toPx()
                val elbowX = center.x + cos(midAngleRad).toFloat() * (outerEdge + extendLen)
                val elbowY = center.y + sin(midAngleRad).toFloat() * (outerEdge + extendLen)

                // 终点固定在右侧
                val labelX = size.width - 4.dp.toPx()
                val endY = elbowY.coerceIn(20.dp.toPx(), size.height - 20.dp.toPx())

                val lineColor = parseColor(item.color)
                val lineWidth = 1.5f.dp.toPx()

                // 径向线段
                drawLine(lineColor, Offset(startX, startY), Offset(elbowX, elbowY), lineWidth)
                // 水平线段
                drawLine(lineColor, Offset(elbowX, elbowY), Offset(labelX, endY), lineWidth)
                // 起点小圆点
                drawCircle(lineColor, 3.dp.toPx(), Offset(startX, startY))

                // 文字标签背景 + 文字
                val label = "${item.title}  $durationText"
                val textSizePx = 11.dp.toPx()
                val textPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = textSizePx
                    isAntiAlias = true
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
                val textWidth = textPaint.measureText(label)
                val bgPadH = 8.dp.toPx()
                val bgPadV = 4.dp.toPx()
                val bgW = textWidth + bgPadH * 2
                val bgH = textSizePx + bgPadV * 2

                val bgRight = labelX
                val bgLeft = bgRight - bgW
                val bgTop = endY - bgH / 2
                val bgBottom = bgTop + bgH

                val bgRect = android.graphics.RectF(bgLeft, bgTop, bgRight, bgBottom)
                val bgPaint = android.graphics.Paint().apply {
                    color = parseColor(item.color).copy(alpha = 0.9f).toArgb()
                    isAntiAlias = true
                }
                drawContext.canvas.nativeCanvas.drawRoundRect(
                    bgRect, 6.dp.toPx(), 6.dp.toPx(), bgPaint
                )

                // 文字
                textPaint.textAlign = android.graphics.Paint.Align.LEFT
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    bgLeft + bgPadH,
                    endY + textSizePx / 3f,
                    textPaint
                )
            }
        }

        // 中心文字
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${items.size}",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "个任务",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TaskSummaryRow(item: TaskSummaryItem) {
    val durationH = item.durationMinutes / 60
    val durationM = item.durationMinutes % 60
    val durationText = if (durationH > 0) "${durationH}时${durationM}分" else "${durationM}分"

    val timeRangeText = item.timeRanges.joinToString("、") { (s, e) ->
        String.format("%02d:%02d-%02d:%02d", s / 60, s % 60, e / 60, e % 60)
    }

    var showTimeRange by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(parseColor(item.color))
                .clickable { showTimeRange = !showTimeRange }
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            if (showTimeRange) {
                Text(
                    text = timeRangeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = durationText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = String.format("%.0f%%", item.percentage),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
