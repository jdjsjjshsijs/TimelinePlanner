package com.example.timelineplanner.ui.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.timelineplanner.data.repository.SyncRepository
import com.example.timelineplanner.data.repository.TaskRepository
import com.example.timelineplanner.model.Task
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.timelineplanner.util.todayStartMillis
import javax.inject.Inject

@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val syncRepository: SyncRepository
) : ViewModel() {

    private val todayStart: Long by lazy { todayStartMillis() }

    private val _selectedDate = MutableStateFlow(todayStart)
    val selectedDate: StateFlow<Long> = _selectedDate.asStateFlow()

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    private val _selectedTaskIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedTaskIds: StateFlow<Set<Long>> = _selectedTaskIds.asStateFlow()

    val isSelectionMode: Boolean get() = _selectedTaskIds.value.isNotEmpty()

    private var loadJob: Job? = null

    init {
        restoreFromServer()
        loadTasks()
    }

    private fun restoreFromServer() {
        viewModelScope.launch {
            val serverTasks = syncRepository.fetchAllTasks() ?: return@launch
            val localTasks = taskRepository.getTasksByDateOnce(_selectedDate.value)
            if (localTasks.isEmpty() && serverTasks.isNotEmpty()) {
                serverTasks.filter { it.dateMillis == _selectedDate.value }.forEach { task ->
                    taskRepository.insertTask(task)
                }
            }
        }
    }

    private fun loadTasks() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            taskRepository.getTasksByDate(_selectedDate.value).collect { taskList ->
                _tasks.value = taskList
            }
        }
    }

    fun selectDate(dateMillis: Long) {
        exitSelectionMode()
        _selectedDate.value = dateMillis
        loadTasks()
    }

    fun refreshTasks() {
        viewModelScope.launch {
            val taskList = taskRepository.getTasksByDateOnce(_selectedDate.value)
            _tasks.value = taskList
        }
    }

    fun startSelectionMode(taskId: Long) {
        _selectedTaskIds.value = setOf(taskId)
    }

    fun toggleSelection(taskId: Long) {
        val current = _selectedTaskIds.value
        _selectedTaskIds.value = if (taskId in current) {
            current - taskId
        } else {
            current + taskId
        }
    }

    fun exitSelectionMode() {
        _selectedTaskIds.value = emptySet()
    }

    fun deleteSelectedTasks() {
        val ids = _selectedTaskIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            taskRepository.deleteTasksByIds(ids)
            _selectedTaskIds.value = emptySet()
        }
    }
}
