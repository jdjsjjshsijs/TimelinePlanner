package com.example.timelineplanner.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.outlined.Analytics
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
    summaryViewModel: DailySummaryViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var editingTaskId by remember { mutableStateOf<Long?>(null) }
    var showTaskSheet by remember { mutableStateOf(false) }
    val selectedDate by timelineViewModel.selectedDate.collectAsState()

    LaunchedEffect(selectedDate) {
        aiChatViewModel.setCurrentDate(selectedDate)
        summaryViewModel.setDate(selectedDate)
    }

    // AI 助手操作涉及其他日期时，自动跳转到该日期
    LaunchedEffect(Unit) {
        aiChatViewModel.navigateToDateEvent.collect { date ->
            timelineViewModel.selectDate(date)
            // 强制刷新确保删除等操作立即反映
            timelineViewModel.refreshTasks()
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
                        editingTaskId = task.id
                        taskDetailViewModel.setCurrentDate(selectedDate)
                        showTaskSheet = true
                    },
                    onEmptyAreaClick = { startMinute ->
                        editingTaskId = null
                        taskDetailViewModel.setCurrentDate(selectedDate)
                        showTaskSheet = true
                    }
                )
                FloatingActionButton(
                    onClick = {
                        editingTaskId = null
                        taskDetailViewModel.setCurrentDate(selectedDate)
                        showTaskSheet = true
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "新建日程")
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
}
