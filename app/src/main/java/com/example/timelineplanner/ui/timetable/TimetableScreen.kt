package com.example.timelineplanner.ui.timetable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timelineplanner.model.Course
import com.example.timelineplanner.ui.timeline.parseColor

private val dayLabels = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
private val TIMETABLE_START_HOUR = 8
private val TIMETABLE_END_HOUR = 22
private val HOUR_HEIGHT_DP = 60
private val DAY_WIDTH_DP = 56

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    viewModel: TimetableViewModel,
    onAddCourse: () -> Unit,
    onEditCourse: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val courses by viewModel.courses.collectAsState()
    var showDeleteDialog by remember { mutableStateOf<Course?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("课表") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddCourse) {
                Icon(Icons.Default.Add, contentDescription = "添加课程")
            }
        }
    ) { padding ->
        if (courses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "暂无课程",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "点击右下角 + 添加课程",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                // Day header row
                Row(modifier = Modifier.fillMaxWidth()) {
                    // Time column spacer
                    Box(modifier = Modifier.width(40.dp))
                    dayLabels.forEach { label ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Timetable grid
                val totalHours = TIMETABLE_END_HOUR - TIMETABLE_START_HOUR
                val gridHeightDp = totalHours * HOUR_HEIGHT_DP

                Row(modifier = Modifier.fillMaxWidth()) {
                    // Time labels column
                    Column(
                        modifier = Modifier.width(40.dp)
                    ) {
                        for (hour in TIMETABLE_START_HOUR until TIMETABLE_END_HOUR) {
                            Box(
                                modifier = Modifier.height(HOUR_HEIGHT_DP.dp),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                Text(
                                    text = String.format("%02d:00", hour),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    // Course grid for each day
                    for (day in 1..7) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(gridHeightDp.dp)
                        ) {
                            // Hour grid lines
                            for (hour in TIMETABLE_START_HOUR until TIMETABLE_END_HOUR) {
                                val topDp = (hour - TIMETABLE_START_HOUR) * HOUR_HEIGHT_DP
                                Box(
                                    modifier = Modifier
                                        .padding(top = topDp.dp)
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                )
                            }

                            // Course blocks for this day
                            courses.filter { day in it.daysOfWeek }.forEach { course ->
                                val startHour = course.startMinute / 60
                                val startMin = course.startMinute % 60
                                val endHour = course.endMinute / 60
                                val endMin = course.endMinute % 60

                                val topOffset = ((startHour - TIMETABLE_START_HOUR) * 60 + startMin) * HOUR_HEIGHT_DP / 60f
                                val height = ((endHour - startHour) * 60 + (endMin - startMin)) * HOUR_HEIGHT_DP / 60f

                                if (topOffset >= 0 && height > 0) {
                                    Box(
                                        modifier = Modifier
                                            .padding(top = topOffset.dp)
                                            .fillMaxWidth()
                                            .padding(horizontal = 2.dp)
                                            .height(height.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(parseColor(course.color).copy(alpha = 0.85f))
                                            .clickable { onEditCourse(course.id) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.padding(2.dp)
                                        ) {
                                            Text(
                                                text = course.title,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = TextAlign.Center,
                                                fontSize = 10.sp
                                            )
                                            if (course.location.isNotBlank()) {
                                                Text(
                                                    text = course.location,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White.copy(alpha = 0.8f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    fontSize = 8.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Course list below the grid
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "全部课程",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                courses.forEach { course ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEditCourse(course.id) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(36.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(parseColor(course.color))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = course.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            val dayText = course.daysOfWeek.sorted().map { dayLabels[it - 1] }.joinToString(" ")
                            val timeText = String.format("%02d:%02d-%02d:%02d",
                                course.startMinute / 60, course.startMinute % 60,
                                course.endMinute / 60, course.endMinute % 60)
                            Text(
                                text = "$dayText $timeText",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { showDeleteDialog = course },
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = "删除",
                                modifier = Modifier.height(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    showDeleteDialog?.let { course ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("确认删除") },
            text = { Text("确定要删除「${course.title}」吗？时间线上的课程显示也会消失。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = null
                        viewModel.deleteCourse(course)
                    },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) { Text("取消") }
            }
        )
    }
}
