package com.example.timelineplanner.ui.practice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.timelineplanner.model.PracticeRecord
import com.example.timelineplanner.model.PracticeSubject
import com.example.timelineplanner.ui.theme.TaskColors
import com.example.timelineplanner.ui.theme.accuracyHighColor
import com.example.timelineplanner.ui.theme.accuracyMidColor
import com.example.timelineplanner.ui.theme.accuracyLowColor
import com.example.timelineplanner.ui.theme.chartDotBgColor
import com.example.timelineplanner.ui.timeline.parseColor
import com.example.timelineplanner.util.formatDateLabel
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeScreen(
    viewModel: PracticeViewModel,
    modifier: Modifier = Modifier
) {
    val subjects by viewModel.subjects.collectAsState()
    val selectedSubject by viewModel.selectedSubject.collectAsState()
    val records by viewModel.records.collectAsState()
    val showAddSubjectDialog by viewModel.showAddSubjectDialog.collectAsState()
    val showAddRecordDialog by viewModel.showAddRecordDialog.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.toastEvent.collectLatest { msg ->
            // Toast handled via snackbar in parent if needed
        }
    }

    AnimatedVisibility(
        visible = selectedSubject == null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        SubjectListScreen(
            subjects = subjects,
            onSubjectClick = { viewModel.selectSubject(it) },
            onAddSubject = { viewModel.showAddSubjectDialog() },
            onDeleteSubject = { viewModel.deleteSubject(it) },
            modifier = modifier
        )
    }

    AnimatedVisibility(
        visible = selectedSubject != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        selectedSubject?.let { subject ->
            SubjectDetailScreen(
                subject = subject,
                records = records,
                onBack = { viewModel.selectSubject(null) },
                onAddRecord = { viewModel.showAddRecordDialog() },
                onDeleteRecord = { viewModel.deleteRecord(it) },
                modifier = modifier
            )
        }
    }

    if (showAddSubjectDialog) {
        AddSubjectDialog(
            onDismiss = { viewModel.dismissAddSubjectDialog() },
            onConfirm = { name, color -> viewModel.addSubject(name, color) }
        )
    }

    if (showAddRecordDialog) {
        AddRecordDialog(
            onDismiss = { viewModel.dismissAddRecordDialog() },
            onConfirm = { total, correct, notes -> viewModel.addRecord(total, correct, notes) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubjectListScreen(
    subjects: List<PracticeSubject>,
    onSubjectClick: (PracticeSubject) -> Unit,
    onAddSubject: () -> Unit,
    onDeleteSubject: (PracticeSubject) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteConfirm by remember { mutableStateOf<PracticeSubject?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(title = { Text("刷题记录") })

            if (subjects.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "暂无刷题科目",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "点击右下角 + 添加",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                    items(subjects) { subject ->
                        SubjectCard(
                            subject = subject,
                            onClick = { onSubjectClick(subject) },
                            onLongPress = { showDeleteConfirm = subject }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }

        FloatingActionButton(
            onClick = onAddSubject,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "添加科目")
        }
    }

    showDeleteConfirm?.let { subject ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("确认删除") },
            text = { Text("确定要删除「${subject.name}」及其所有记录吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = null
                        onDeleteSubject(subject)
                    },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun SubjectCard(
    subject: PracticeSubject,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(parseColor(subject.color))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = subject.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Default.Delete,
                contentDescription = "删除",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .size(20.dp)
                    .clickable(onClick = onLongPress)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubjectDetailScreen(
    subject: PracticeSubject,
    records: List<PracticeRecord>,
    onBack: () -> Unit,
    onAddRecord: () -> Unit,
    onDeleteRecord: (PracticeRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalQuestions = records.sumOf { it.totalQuestions }
    val totalCorrect = records.sumOf { it.correctQuestions }
    val overallAccuracy = if (totalQuestions > 0) totalCorrect.toFloat() / totalQuestions * 100f else 0f

    // Clicked data point for chart
    var clickedRecord by remember { mutableStateOf<PracticeRecord?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text(subject.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                // Overall stats card
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "总正确率",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = String.format("%.1f%%", overallAccuracy),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "共${records.size} 次 | ${totalCorrect}/${totalQuestions} 题",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // Accuracy trend chart
                if (records.size >= 2) {
                    item {
                        Text(
                            text = "正确率趋势",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    item {
                        AccuracyLineChart(
                            records = records.sortedBy { it.createdAtMillis },
                            onPointClick = { clickedRecord = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        )
                    }
                }

                // Records list
                item {
                    Text(
                        text = "练习记录",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (records.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "暂无记录，点击右下角 + 添加",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(records) { record ->
                        RecordRow(
                            record = record,
                            onDelete = { onDeleteRecord(record) }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        FloatingActionButton(
            onClick = onAddRecord,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "添加记录")
        }
    }

    // Chart data point detail dialog
    clickedRecord?.let { record ->
        AlertDialog(
            onDismissRequest = { clickedRecord = null },
            title = { Text("练习详情") },
            text = {
                Column {
                    Text("时间: ${formatRecordTime(record.createdAtMillis)}")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("总题数: ${record.totalQuestions}")
                    Text("正确数: ${record.correctQuestions}")
                    Text("正确率: ${String.format("%.1f%%", record.accuracy)}")
                    if (record.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("备注: ${record.notes}")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { clickedRecord = null }) { Text("确定") }
            }
        )
    }
}

@Composable
private fun RecordRow(
    record: PracticeRecord,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatRecordTime(record.createdAtMillis),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${record.correctQuestions}/${record.totalQuestions} 题",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                if (record.notes.isNotBlank()) {
                    Text(
                        text = record.notes,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            Text(
                text = String.format("%.0f%%", record.accuracy),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = when {
                    record.accuracy >= 80 -> accuracyHighColor()
                    record.accuracy >= 60 -> accuracyMidColor()
                    else -> accuracyLowColor()
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun AccuracyLineChart(
    records: List<PracticeRecord>,
    onPointClick: (PracticeRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant

    // Store computed points for hit-testing
    var computedPoints by remember { mutableStateOf<List<Pair<Offset, PracticeRecord>>>(emptyList()) }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(records) {
                    detectTapGestures { tapOffset ->
                        // Find the nearest point within a threshold
                        val threshold = 24.dp.toPx()
                        val nearest = computedPoints.minByOrNull { (point, _) ->
                            val dx = tapOffset.x - point.x
                            val dy = tapOffset.y - point.y
                            abs(dx) + abs(dy)
                        }
                        if (nearest != null) {
                            val (point, record) = nearest
                            val dx = tapOffset.x - point.x
                            val dy = tapOffset.y - point.y
                            val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                            if (dist <= threshold) {
                                onPointClick(record)
                            }
                        }
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            val padLeft = 40.dp.toPx()
            val padRight = 16.dp.toPx()
            val padTop = 8.dp.toPx()
            val padBottom = 24.dp.toPx()
            val chartW = w - padLeft - padRight
            val chartH = h - padTop - padBottom

            // Grid lines at 0%, 25%, 50%, 75%, 100%
            for (i in 0..4) {
                val y = padTop + chartH * (1f - i / 4f)
                drawLine(gridColor, Offset(padLeft, y), Offset(w - padRight, y), 1f)
                val textPaint = android.graphics.Paint().apply {
                    color = textColor.hashCode()
                    textSize = 10.dp.toPx()
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.RIGHT
                }
                drawContext.canvas.nativeCanvas.drawText(
                    "${i * 25}",
                    padLeft - 4.dp.toPx(),
                    y + 4.dp.toPx(),
                    textPaint
                )
            }

            if (records.isEmpty()) return@Canvas

            val stepX = if (records.size > 1) chartW / (records.size - 1) else chartW
            val points = records.mapIndexed { index, record ->
                val x = padLeft + stepX * index
                val y = padTop + chartH * (1f - record.accuracy / 100f)
                Offset(x, y)
            }

            // Store for hit-testing
            computedPoints = points.zip(records)

            // Draw line
            if (points.size >= 2) {
                val path = Path().apply {
                    moveTo(points[0].x, points[0].y)
                    for (i in 1 until points.size) {
                        lineTo(points[i].x, points[i].y)
                    }
                }
                drawPath(path, lineColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
            }

            // Draw dots
            points.forEach { point ->
                drawCircle(lineColor, 4.dp.toPx(), point)
                drawCircle(Color.White, 2.5.dp.toPx(), point)
            }
        }
    }
}

@Composable
private fun AddSubjectDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedColorIndex by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加刷题科目") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("科目名称") },
                    placeholder = { Text("例如：民法练习题") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "颜色",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TaskColors.take(8).forEachIndexed { index, colorHex ->
                        val color = parseColor(colorHex)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .then(
                                    if (index == selectedColorIndex) Modifier.background(
                                        chartDotBgColor(), CircleShape
                                    ) else Modifier
                                )
                                .clickable { selectedColorIndex = index }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, TaskColors[selectedColorIndex]) },
                enabled = name.isNotBlank()
            ) { Text("添加") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun AddRecordDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int, Int, String) -> Unit
) {
    var totalText by remember { mutableStateOf("") }
    var correctText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加练习记录") },
        text = {
            Column {
                OutlinedTextField(
                    value = totalText,
                    onValueChange = { totalText = it.filter { c -> c.isDigit() } },
                    label = { Text("总题目数") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = correctText,
                    onValueChange = { correctText = it.filter { c -> c.isDigit() } },
                    label = { Text("正确题目数") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("备注（可选）") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val total = totalText.toIntOrNull() ?: 0
                    val correct = correctText.toIntOrNull() ?: 0
                    onConfirm(total, correct, notes)
                },
                enabled = totalText.isNotBlank() && correctText.isNotBlank()
            ) { Text("添加") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

private val recordTimeFormat = SimpleDateFormat("M月d日 HH:mm", Locale.CHINA)

private fun formatRecordTime(millis: Long): String {
    if (millis <= 0) return ""
    return recordTimeFormat.format(Date(millis))
}
