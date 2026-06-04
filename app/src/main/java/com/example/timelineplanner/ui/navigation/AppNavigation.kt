package com.example.timelineplanner.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.timelineplanner.model.Task
import com.example.timelineplanner.ui.aichat.AiChatScreen
import com.example.timelineplanner.ui.aichat.AiChatViewModel
import com.example.timelineplanner.ui.coursedetail.CourseDetailSheet
import com.example.timelineplanner.ui.coursedetail.CourseDetailViewModel
import com.example.timelineplanner.ui.taskdetail.TaskDetailSheet
import com.example.timelineplanner.ui.taskdetail.TaskDetailViewModel
import com.example.timelineplanner.ui.summary.DailySummaryScreen
import com.example.timelineplanner.ui.summary.DailySummaryViewModel
import com.example.timelineplanner.ui.timeline.TimelineScreen
import com.example.timelineplanner.ui.timeline.TimelineViewModel

data class BottomNavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun AppNavigation(
    timelineViewModel: TimelineViewModel = hiltViewModel(),
    aiChatViewModel: AiChatViewModel = hiltViewModel(),
    taskDetailViewModel: TaskDetailViewModel = hiltViewModel(),
    summaryViewModel: DailySummaryViewModel = hiltViewModel(),
    courseDetailViewModel: CourseDetailViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var editingTaskId by remember { mutableStateOf<Long?>(null) }
    var showTaskSheet by remember { mutableStateOf(false) }
    var editingCourseId by remember { mutableStateOf<Long?>(null) }
    var showCourseSheet by remember { mutableStateOf(false) }
    val selectedDate by timelineViewModel.selectedDate.collectAsState()
    val canUndo by timelineViewModel.canUndo.collectAsState()

    LaunchedEffect(selectedDate) {
        aiChatViewModel.setCurrentDate(selectedDate)
        summaryViewModel.setDate(selectedDate)
    }

    // AI 助手操作涉及其他日期时，自动跳转到该日期
    LaunchedEffect(Unit) {
        aiChatViewModel.navigateToDateEvent.collect { date ->
            timelineViewModel.selectDate(date)
            timelineViewModel.refreshTasks()
        }
    }

    // 任务创建/编辑撤销事件
    LaunchedEffect(Unit) {
        taskDetailViewModel.taskCreatedEvent.collect { taskId ->
            timelineViewModel.recordTaskCreated(taskId)
        }
    }
    LaunchedEffect(Unit) {
        taskDetailViewModel.taskEditedEvent.collect { oldTask ->
            timelineViewModel.recordTaskEdited(oldTask)
        }
    }

    val tabs = listOf(
        BottomNavItem("日程", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
        BottomNavItem("总结", Icons.Filled.Analytics, Icons.Outlined.Analytics),
        BottomNavItem("AI 助手", Icons.Filled.Chat, Icons.Outlined.Chat)
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                tabs.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == index)
                                    item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label
                            )
                        },
                        label = { Text(item.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        when (selectedTab) {
            0 -> Box(modifier = Modifier.padding(paddingValues)) {
                TimelineScreen(
                    viewModel = timelineViewModel,
                    onTaskClick = { task ->
                        if (task.id < 0) {
                            // Course task — open course editor
                            val courseId = (-task.id) / 10
                            editingCourseId = courseId
                            showCourseSheet = true
                        } else {
                            editingTaskId = task.id
                            taskDetailViewModel.setCurrentDate(selectedDate)
                            showTaskSheet = true
                        }
                    },
                    onEmptyAreaClick = { startMinute ->
                        editingTaskId = null
                        taskDetailViewModel.setCurrentDate(selectedDate)
                        showTaskSheet = true
                    }
                )
                var fabMenuExpanded by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    DropdownMenu(
                        expanded = fabMenuExpanded,
                        onDismissRequest = { fabMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("新建任务") },
                            onClick = {
                                fabMenuExpanded = false
                                editingTaskId = null
                                taskDetailViewModel.setCurrentDate(selectedDate)
                                showTaskSheet = true
                            },
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("新建课程") },
                            onClick = {
                                fabMenuExpanded = false
                                editingCourseId = null
                                showCourseSheet = true
                            },
                            leadingIcon = { Icon(Icons.Default.MenuBook, contentDescription = null) }
                        )
                        if (canUndo) {
                            DropdownMenuItem(
                                text = { Text("撤销") },
                                onClick = {
                                    fabMenuExpanded = false
                                    timelineViewModel.undo()
                                },
                                leadingIcon = { Icon(Icons.Default.Undo, contentDescription = null) }
                            )
                        }
                    }
                    FloatingActionButton(
                        onClick = { fabMenuExpanded = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "菜单")
                    }
                }
            }
            1 -> DailySummaryScreen(
                viewModel = summaryViewModel,
                modifier = Modifier.padding(paddingValues)
            )
            2 -> AiChatScreen(
                viewModel = aiChatViewModel,
                modifier = Modifier.padding(paddingValues)
            )
        }
    }

    if (showTaskSheet) {
        TaskDetailSheet(
            viewModel = taskDetailViewModel,
            taskId = editingTaskId,
            onDismiss = {
                showTaskSheet = false
                editingTaskId = null
                timelineViewModel.refreshTasks()
            }
        )
    }

    if (showCourseSheet) {
        CourseDetailSheet(
            viewModel = courseDetailViewModel,
            courseId = editingCourseId,
            onDismiss = {
                showCourseSheet = false
                editingCourseId = null
                timelineViewModel.refreshTasks()
            }
        )
    }
}
