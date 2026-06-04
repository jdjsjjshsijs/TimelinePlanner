package com.example.timelineplanner.ui.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.timelineplanner.data.repository.CourseRepository
import com.example.timelineplanner.data.repository.SyncRepository
import com.example.timelineplanner.data.repository.TaskRepository
import com.example.timelineplanner.model.Task
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.timelineplanner.util.todayStartMillis
import javax.inject.Inject

sealed class UndoAction {
    data class Delete(val tasks: List<Task>) : UndoAction()
    data class Create(val taskId: Long) : UndoAction()
    data class Edit(val oldTask: Task) : UndoAction()
}

@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val courseRepository: CourseRepository,
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
    val isSyncing: StateFlow<Boolean> = syncRepository.isSyncing

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val undoStack = mutableListOf<UndoAction>()
    private companion object {
        const val MAX_UNDO_STACK_SIZE = 20
    }

    private var loadJob: Job? = null

    init {
        restoreFromServer()
        loadTasks()
    }

    private fun restoreFromServer() {
        viewModelScope.launch {
            val serverTasks = syncRepository.fetchAllTasks() ?: return@launch
            if (serverTasks.isEmpty()) return@launch
            val localTasks = taskRepository.getTasksByDateOnce(_selectedDate.value)
            if (localTasks.isEmpty()) {
                serverTasks.filter { it.dateMillis == _selectedDate.value }.forEach { task ->
                    taskRepository.restoreTask(task)
                }
                refreshTasks()
            }
        }
    }

    fun restoreAllFromServer(onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = syncRepository.restoreAllFromServer()
            if (ok) {
                loadTasks()
            }
            onDone(ok)
        }
    }

    private fun loadTasks() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            taskRepository.getTasksByDate(_selectedDate.value).collect { taskList ->
                val merged = withContext(Dispatchers.Default) {
                    val courseTasks = courseRepository.generateCourseTasksForDate(_selectedDate.value)
                    (taskList + courseTasks).sortedBy { it.startMinute }
                }
                _tasks.value = merged
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
            val merged = withContext(Dispatchers.Default) {
                val taskList = taskRepository.getTasksByDateOnce(_selectedDate.value)
                val courseTasks = courseRepository.generateCourseTasksForDate(_selectedDate.value)
                (taskList + courseTasks).sortedBy { it.startMinute }
            }
            _tasks.value = merged
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
            val deletedTasks = ids.mapNotNull { taskRepository.getTaskById(it) }
            taskRepository.deleteTasksByIds(ids)
            _selectedTaskIds.value = emptySet()
            pushUndo(UndoAction.Delete(deletedTasks))
        }
    }

    fun recordTaskCreated(taskId: Long) {
        pushUndo(UndoAction.Create(taskId))
    }

    fun recordTaskEdited(oldTask: Task) {
        pushUndo(UndoAction.Edit(oldTask))
    }

    private fun pushUndo(action: UndoAction) {
        if (undoStack.size >= MAX_UNDO_STACK_SIZE) {
            undoStack.removeAt(0)
        }
        undoStack.add(action)
        _canUndo.value = undoStack.isNotEmpty()
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val action = undoStack.removeAt(undoStack.lastIndex)
        _canUndo.value = undoStack.isNotEmpty()
        viewModelScope.launch {
            when (action) {
                is UndoAction.Delete -> {
                    action.tasks.forEach { taskRepository.restoreTask(it) }
                }
                is UndoAction.Create -> {
                    taskRepository.deleteTaskById(action.taskId)
                }
                is UndoAction.Edit -> {
                    taskRepository.updateTask(action.oldTask)
                }
            }
        }
    }
}
