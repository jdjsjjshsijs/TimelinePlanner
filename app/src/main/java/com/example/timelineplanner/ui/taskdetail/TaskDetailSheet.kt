package com.example.timelineplanner.ui.taskdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.timelineplanner.ui.timeline.parseColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailSheet(
    viewModel: TaskDetailViewModel,
    taskId: Long?,
    onDismiss: () -> Unit
) {
    val title by viewModel.title.collectAsState()
    val startHour by viewModel.startHour.collectAsState()
    val startMinute by viewModel.startMinute.collectAsState()
    val endHour by viewModel.endHour.collectAsState()
    val endMinute by viewModel.endMinute.collectAsState()
    val selectedColorIndex by viewModel.selectedColorIndex.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val editingTaskId by viewModel.editingTaskId.collectAsState()
    val isEditing = editingTaskId != null
    val timerState by viewModel.timerState.collectAsState()
    val elapsedDisplay by viewModel.elapsedDisplay.collectAsState()
    val pauseSegments by viewModel.pauseSegments.collectAsState()
    val endConfirmVisible by viewModel.endConfirmVisible.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(taskId) {
        if (taskId != null && taskId > 0) {
            viewModel.loadTask(taskId)
        } else {
            viewModel.resetForNew()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.dismissEvent.collect { onDismiss() }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 标题行 + 计时器开关
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEditing) "编辑任务" else "新建任务",
                    style = MaterialTheme.typography.headlineSmall
                )

                if (isEditing && timerState == TimerState.IDLE) {
                    IconButton(
                        onClick = { viewModel.startTimer() },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = "计时器",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            // Timer display when active
            if (timerState != TimerState.IDLE) {
                Spacer(modifier = Modifier.height(8.dp))
                TimerPanel(
                    timerState = timerState,
                    elapsedDisplay = elapsedDisplay,
                    pauseSegments = pauseSegments,
                    onPause = { viewModel.pauseTimer() },
                    onResume = { viewModel.resumeTimer() },
                    onEnd = { viewModel.requestEndTimer() },
                    onSave = { viewModel.saveTimerResult() }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 任务名称
            OutlinedTextField(
                value = title,
                onValueChange = viewModel::onTitleChange,
                label = { Text("任务名称") },
                placeholder = { Text("例如：晨会") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = timerState == TimerState.IDLE
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 开始时间
            Text(
                text = "开始时间",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            TimePickerRow(
                hour = startHour,
                minute = startMinute,
                onHourChange = viewModel::onStartHourChange,
                onMinuteChange = viewModel::onStartMinuteChange,
                enabled = timerState == TimerState.IDLE
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 结束时间
            Text(
                text = "结束时间",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            TimePickerRow(
                hour = endHour,
                minute = endMinute,
                onHourChange = viewModel::onEndHourChange,
                onMinuteChange = viewModel::onEndMinuteChange,
                enabled = timerState == TimerState.IDLE
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 颜色选择
            Text(
                text = "颜色",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(viewModel.availableColors) { index, colorHex ->
                    val color = parseColor(colorHex)
                    val isSelected = index == selectedColorIndex
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .then(
                                if (isSelected) Modifier.border(
                                    3.dp,
                                    MaterialTheme.colorScheme.onSurface,
                                    CircleShape
                                ) else Modifier
                            )
                            .clickable(enabled = timerState == TimerState.IDLE) {
                                viewModel.onColorIndexChange(index)
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 备注
            OutlinedTextField(
                value = notes,
                onValueChange = viewModel::onNotesChange,
                label = { Text("备注（可选）") },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
                enabled = timerState == TimerState.IDLE
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 按钮行
            if (timerState == TimerState.IDLE) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isEditing) {
                        OutlinedButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("删除")
                        }
                    }

                    Button(
                        onClick = { viewModel.save() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isEditing) "保存" else "创建")
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除这个任务吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.delete()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (endConfirmVisible) {
        AlertDialog(
            onDismissRequest = { viewModel.onEndTimerCancelled() },
            title = { Text("结束计时") },
            text = { Text("确定要结束计时吗？结束时间将记录为当前时间。") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.onEndTimerConfirmed() }
                ) { Text("结束") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEndTimerCancelled() }) {
                    Text("取消")
                }
            }
        )
    }
}
