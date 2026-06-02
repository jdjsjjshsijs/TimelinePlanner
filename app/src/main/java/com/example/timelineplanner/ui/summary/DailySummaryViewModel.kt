package com.example.timelineplanner.ui.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.timelineplanner.data.repository.TaskRepository
import com.example.timelineplanner.model.Task
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.timelineplanner.util.todayStartMillis
import javax.inject.Inject

data class TaskSummaryItem(
    val title: String,
    val color: String,
    val durationMinutes: Int,
    val percentage: Float,
    val timeRanges: List<Pair<Int, Int>>
)

@HiltViewModel
class DailySummaryViewModel @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(todayStartMillis())
    val selectedDate: StateFlow<Long> = _selectedDate.asStateFlow()

    private val _summaryItems = MutableStateFlow<List<TaskSummaryItem>>(emptyList())
    val summaryItems: StateFlow<List<TaskSummaryItem>> = _summaryItems.asStateFlow()

    private val _totalTaskMinutes = MutableStateFlow(0)
    val totalTaskMinutes: StateFlow<Int> = _totalTaskMinutes.asStateFlow()

    init {
        loadTasks()
    }

    fun setDate(dateMillis: Long) {
        _selectedDate.value = dateMillis
        loadTasks()
    }

    fun selectDate(dateMillis: Long) {
        _selectedDate.value = dateMillis
        loadTasks()
    }

    private fun getEffectiveDuration(task: Task): Int {
        val total = task.endMinute - task.startMinute
        val pauseSum = task.pauseSegments.sumOf { it.second - it.first }
        return (total - pauseSum).coerceAtLeast(0)
    }

    private fun loadTasks() {
        viewModelScope.launch {
            val tasks = taskRepository.getTasksByDateOnce(_selectedDate.value)
            val totalMinutes = tasks.sumOf { getEffectiveDuration(it) }
            _totalTaskMinutes.value = totalMinutes

            _summaryItems.value = if (totalMinutes > 0) {
                tasks.groupBy { it.title }.map { (title, group) ->
                    val duration = group.sumOf { getEffectiveDuration(it) }
                    val color = group.first().color
                    val ranges = group.map { it.startMinute to it.endMinute }
                        .sortedBy { it.first }
                    TaskSummaryItem(
                        title = title,
                        color = color,
                        durationMinutes = duration,
                        percentage = duration.toFloat() / totalMinutes * 100f,
                        timeRanges = ranges
                    )
                }.sortedByDescending { it.durationMinutes }
            } else {
                emptyList()
            }
        }
    }
}
