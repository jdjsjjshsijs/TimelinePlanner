package com.example.timelineplanner.ui.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.timelineplanner.data.repository.TaskRepository
import com.example.timelineplanner.data.repository.CourseRepository
import com.example.timelineplanner.model.Task
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.timelineplanner.util.ONE_DAY_MILLIS
import com.example.timelineplanner.util.todayStartMillis
import javax.inject.Inject

data class TaskSummaryItem(
    val title: String,
    val color: String,
    val durationMinutes: Int,
    val percentage: Float,
    val timeRanges: List<Pair<Int, Int>>,
    val isCourse: Boolean = false
)

enum class SummaryPeriod(val label: String) {
    DAY("日"),
    WEEK("周"),
    MONTH("月")
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class DailySummaryViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val courseRepository: CourseRepository
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(todayStartMillis())
    val selectedDate: StateFlow<Long> = _selectedDate.asStateFlow()

    private val _period = MutableStateFlow(SummaryPeriod.DAY)
    val period: StateFlow<SummaryPeriod> = _period.asStateFlow()

    private val _summaryItems = MutableStateFlow<List<TaskSummaryItem>>(emptyList())
    val summaryItems: StateFlow<List<TaskSummaryItem>> = _summaryItems.asStateFlow()

    private val _totalTaskMinutes = MutableStateFlow(0)
    val totalTaskMinutes: StateFlow<Int> = _totalTaskMinutes.asStateFlow()

    init {
        viewModelScope.launch {
            _selectedDate.flatMapLatest { date ->
                taskRepository.getTasksByDate(date)
            }.collect { tasks ->
                if (_period.value == SummaryPeriod.DAY) {
                    val courseTasks = courseRepository.generateCourseTasksForDate(_selectedDate.value)
                    processTasks(tasks + courseTasks)
                }
            }
        }
    }

    fun setDate(dateMillis: Long) {
        _selectedDate.value = dateMillis
        if (_period.value != SummaryPeriod.DAY) {
            loadForPeriod()
        }
    }

    fun selectDate(dateMillis: Long) {
        _selectedDate.value = dateMillis
        if (_period.value != SummaryPeriod.DAY) {
            loadForPeriod()
        }
    }

    fun selectPeriod(newPeriod: SummaryPeriod) {
        _period.value = newPeriod
        if (newPeriod == SummaryPeriod.DAY) {
            // DAY uses Flow, re-trigger by reloading
            viewModelScope.launch {
                val tasks = taskRepository.getTasksByDateOnce(_selectedDate.value)
                val courseTasks = courseRepository.generateCourseTasksForDate(_selectedDate.value)
                processTasks(tasks + courseTasks)
            }
        } else {
            loadForPeriod()
        }
    }

    private fun loadForPeriod() {
        viewModelScope.launch {
            val range = getDateRange(_selectedDate.value, _period.value)
            val tasks = taskRepository.getTasksByDateRange(range.first, range.second)

            // Generate course tasks for each day in the range
            val courseTasks = mutableListOf<Task>()
            var currentDay = range.first
            while (currentDay < range.second) {
                courseTasks.addAll(courseRepository.generateCourseTasksForDate(currentDay))
                currentDay += ONE_DAY_MILLIS
            }

            processTasks(tasks + courseTasks)
        }
    }

    private fun getDateRange(dateMillis: Long, period: SummaryPeriod): Pair<Long, Long> {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = dateMillis }
        return when (period) {
            SummaryPeriod.DAY -> dateMillis to dateMillis + ONE_DAY_MILLIS
            SummaryPeriod.WEEK -> {
                cal.firstDayOfWeek = java.util.Calendar.MONDAY
                cal.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                val weekStart = cal.timeInMillis
                weekStart to weekStart + 7 * ONE_DAY_MILLIS
            }
            SummaryPeriod.MONTH -> {
                cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                val monthStart = cal.timeInMillis
                val monthEnd = cal.apply { add(java.util.Calendar.MONTH, 1) }.timeInMillis
                monthStart to monthEnd
            }
        }
    }



    private fun getEffectiveDuration(task: Task): Int {
        val total = task.endMinute - task.startMinute
        val pauseSum = task.pauseSegments.sumOf { it.second - it.first }
        return (total - pauseSum).coerceAtLeast(0)
    }

    private fun processTasks(tasks: List<Task>) {
        val totalMinutes = tasks.sumOf { getEffectiveDuration(it) }
        _totalTaskMinutes.value = totalMinutes

        _summaryItems.value = if (totalMinutes > 0) {
            tasks.groupBy { it.title }.map { (title, group) ->
                val duration = group.sumOf { getEffectiveDuration(it) }
                val color = group.first().color
                val isCourse = group.any { it.id < 0 }
                val ranges = group.map { it.startMinute to it.endMinute }
                    .sortedBy { it.first }
                TaskSummaryItem(
                    title = title,
                    color = color,
                    durationMinutes = duration,
                    percentage = duration.toFloat() / totalMinutes * 100f,
                    timeRanges = ranges,
                    isCourse = isCourse
                )
            }.sortedByDescending { it.durationMinutes }
        } else {
            emptyList()
        }
    }
}
