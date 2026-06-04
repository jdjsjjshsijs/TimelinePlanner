package com.example.timelineplanner.ui.taskdetail

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import android.content.pm.PackageManager
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.foundation.Image
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
    var showPinSettingsDialog by remember { mutableStateOf(false) }
    var showWhitelistDialog by remember { mutableStateOf(false) }
    var showAddAppDialog by remember { mutableStateOf(false) }
    val activity = LocalContext.current as? com.example.timelineplanner.MainActivity

    val isKioskActive = timerState == TimerState.RUNNING

    // 专注模式：计时器启动时锁定应用，结束时解锁
    LaunchedEffect(timerState) {
        if (timerState == TimerState.RUNNING) {
            try {
                activity?.enterKioskMode()
            } catch (_: Exception) {
                showPinSettingsDialog = true
            }
        } else if (timerState == TimerState.ENDED || timerState == TimerState.IDLE || timerState == TimerState.PAUSED) {
            activity?.exitKioskMode()
        }
    }

    // 退出时确保解锁
    DisposableEffect(Unit) {
        onDispose {
            activity?.exitKioskMode()
        }
    }

    // 拦截返回键
    BackHandler {
        if (isKioskActive) {
            // kiosk 模式下什么都不做
        } else {
            onDismiss()
        }
    }

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

    // 共享的内容 composable
    val content: @Composable () -> Unit = {
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
                if (timerState == TimerState.RUNNING) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showWhitelistDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Apps, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("切换应用")
                        }
                        OutlinedButton(
                            onClick = { showAddAppDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("管理白名单")
                        }
                    }
                }
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

    // kiosk 模式：全屏不可关闭的布局
    if (isKioskActive) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(top = 48.dp)
        ) {
            content()
        }
    } else {
        // 正常模式：底部弹窗
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            content()
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

    if (showPinSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showPinSettingsDialog = false },
            title = { Text("开启屏幕固定") },
            text = { Text("专注模式需要开启「屏幕固定」功能。\n\n请在设置中搜索「屏幕固定」或「屏幕锁定」，开启后即可在计时期间锁定应用。") },
            confirmButton = {
                TextButton(onClick = {
                    showPinSettingsDialog = false
                    try {
                        activity?.startActivity(Intent(Settings.ACTION_SETTINGS))
                    } catch (_: Exception) {}
                }) { Text("去设置") }
            },
            dismissButton = {
                TextButton(onClick = { showPinSettingsDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // Whitelist: switch app dialog
    if (showWhitelistDialog && activity != null) {
        val whitelist = remember { mutableStateOf(activity.getWhitelistedPackages()) }
        val pm = activity.packageManager
        val apps = whitelist.value.mapNotNull { pkg ->
            try {
                val info = pm.getApplicationInfo(pkg, 0)
                Triple(pkg, pm.getApplicationLabel(info).toString(), info.loadIcon(pm))
            } catch (_: Exception) { null }
        }
        AlertDialog(
            onDismissRequest = { showWhitelistDialog = false },
            title = { Text("切换到白名单应用") },
            text = {
                if (apps.isEmpty()) {
                    Text("暂无白名单应用，请先点击「管理白名单」添加。")
                } else {
                    LazyColumn {
                        items(apps) { (pkg, label, icon) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showWhitelistDialog = false
                                        activity.launchWhitelistedApp(pkg)
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    bitmap = icon.toBitmap().asImageBitmap(),
                                    contentDescription = label,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(label, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showWhitelistDialog = false }) { Text("取消") }
            }
        )
    }

    // Whitelist: manage apps dialog
    if (showAddAppDialog && activity != null) {
        val pm = activity.packageManager
        val installedApps = remember {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledApplications(android.content.pm.PackageManager.ApplicationInfoFlags.of(PackageManager.MATCH_ALL.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledApplications(PackageManager.GET_META_DATA)
            }
                .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
                .sortedBy { pm.getApplicationLabel(it).toString() }
                .map { it.packageName to pm.getApplicationLabel(it).toString() }
        }
        val whitelist = remember { mutableStateOf(activity.getWhitelistedPackages()) }
        AlertDialog(
            onDismissRequest = { showAddAppDialog = false },
            title = { Text("管理白名单") },
            text = {
                LazyColumn {
                    items(installedApps) { (pkg, label) ->
                        val isAdded = pkg in whitelist.value
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(label, style = MaterialTheme.typography.bodyMedium)
                                Text(pkg, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = {
                                val newSet = whitelist.value.toMutableSet()
                                if (isAdded) newSet.remove(pkg) else newSet.add(pkg)
                                activity.setWhitelistedPackages(newSet)
                                whitelist.value = newSet
                            }) {
                                Icon(
                                    if (isAdded) Icons.Default.Remove else Icons.Default.Add,
                                    contentDescription = if (isAdded) "移除" else "添加",
                                    tint = if (isAdded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddAppDialog = false }) { Text("完成") }
            }
        )
    }
}
