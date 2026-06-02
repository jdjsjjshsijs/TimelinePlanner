package com.example.timelineplanner.ui.taskdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.timelineplanner.data.repository.TaskRepository
import com.example.timelineplanner.model.Task
import com.example.timelineplanner.ui.theme.TaskColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.timelineplanner.util.todayStartMillis
import java.util.Calendar
import javax.inject.Inject

enum class TimerState { IDLE, RUNNING, PAUSED, ENDED }

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModel() {

    private var currentDateMillis: Long = todayStartMillis()

    fun setCurrentDate(dateMillis: Long) {
        currentDateMillis = dateMillis
    }

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _startHour = MutableStateFlow(9)
    val startHour: StateFlow<Int> = _startHour.asStateFlow()

    private val _startMinute = MutableStateFlow(0)
    val startMinute: StateFlow<Int> = _startMinute.asStateFlow()

    private val _endHour = MutableStateFlow(10)
    val endHour: StateFlow<Int> = _endHour.asStateFlow()

    private val _endMinute = MutableStateFlow(0)
    val endMinute: StateFlow<Int> = _endMinute.asStateFlow()

    private val _selectedColorIndex = MutableStateFlow(0)
    val selectedColorIndex: StateFlow<Int> = _selectedColorIndex.asStateFlow()

    val availableColors = TaskColors

    private val _notes = MutableStateFlow("")
    val notes: StateFlow<String> = _notes.asStateFlow()

    private val _editingTaskId = MutableStateFlow<Long?>(null)
    val editingTaskId: StateFlow<Long?> = _editingTaskId.asStateFlow()

    val isEditing: Boolean get() = _editingTaskId.value != null

    private val _dismissEvent = MutableSharedFlow<Unit>()
    val dismissEvent = _dismissEvent.asSharedFlow()

    private val _errorEvent = MutableSharedFlow<String>()
    val errorEvent = _errorEvent.asSharedFlow()

    // Timer state
    private val _timerState = MutableStateFlow(TimerState.IDLE)
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private val _elapsedDisplay = MutableStateFlow("00:00")
    val elapsedDisplay: StateFlow<String> = _elapsedDisplay.asStateFlow()

    private val _pauseSegments = MutableStateFlow<List<Pair<Int, Int>>>(emptyList())
    val pauseSegments: StateFlow<List<Pair<Int, Int>>> = _pauseSegments.asStateFlow()

    private val _endConfirmVisible = MutableStateFlow(false)
    val endConfirmVisible: StateFlow<Boolean> = _endConfirmVisible.asStateFlow()

    private var timerJob: Job? = null
    private var timerStartMillis: Long = 0L
    private var pausedElapsedMillis: Long = 0L
    private var pendingPauseStartMinute: Int = 0

    private fun getCurrentMinute(): Int {
        val now = Calendar.getInstance()
        return now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
    }

    fun startTimer() {
        val nowMinute = getCurrentMinute()
        _startHour.value = nowMinute / 60
        _startMinute.value = nowMinute % 60
        _timerState.value = TimerState.RUNNING
        _pauseSegments.value = emptyList()
        pausedElapsedMillis = 0L
        timerStartMillis = System.currentTimeMillis()

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(500)
                updateElapsedDisplay()
            }
        }
    }

    fun pauseTimer() {
        pendingPauseStartMinute = getCurrentMinute()
        pausedElapsedMillis += System.currentTimeMillis() - timerStartMillis
        _timerState.value = TimerState.PAUSED
        timerJob?.cancel()
        updateElapsedDisplay()
    }

    fun resumeTimer() {
        val pauseEndMinute = getCurrentMinute()
        val segment = pendingPauseStartMinute to pauseEndMinute
        _pauseSegments.value = _pauseSegments.value + segment
        _timerState.value = TimerState.RUNNING
        timerStartMillis = System.currentTimeMillis()
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(500)
                updateElapsedDisplay()
            }
        }
    }

    private fun updateElapsedDisplay() {
        val currentElapsed = pausedElapsedMillis +
            if (_timerState.value == TimerState.RUNNING) System.currentTimeMillis() - timerStartMillis else 0L
        val totalSeconds = currentElapsed / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        _elapsedDisplay.value = String.format("%02d:%02d", minutes, seconds)
    }

    fun requestEndTimer() {
        if (_timerState.value == TimerState.PAUSED) {
            val nowMinute = getCurrentMinute()
            val segment = pendingPauseStartMinute to nowMinute
            _pauseSegments.value = _pauseSegments.value + segment
        }
        _endConfirmVisible.value = true
    }

    fun onEndTimerConfirmed() {
        val nowMinute = getCurrentMinute()
        val startMinTotal = _startHour.value * 60 + _startMinute.value
        // 结束时间至少比开始时间多1分钟，保证任务在时间轴上可见
        val endMinTotal = if (nowMinute <= startMinTotal) startMinTotal + 1 else nowMinute
        _endHour.value = endMinTotal / 60
        _endMinute.value = endMinTotal % 60
        _timerState.value = TimerState.ENDED
        timerJob?.cancel()
        timerJob = null
        _endConfirmVisible.value = false
    }

    fun onEndTimerCancelled() {
        _endConfirmVisible.value = false
    }

    fun saveTimerResult() {
        val taskId = _editingTaskId.value ?: return
        val startMin = _startHour.value * 60 + _startMinute.value
        val endMin = _endHour.value * 60 + _endMinute.value
        val segments = _pauseSegments.value

        viewModelScope.launch {
            taskRepository.updateTaskTimer(taskId, startMin, endMin, segments)
            _dismissEvent.emit(Unit)
        }
    }

    fun loadTask(taskId: Long) {
        _timerState.value = TimerState.IDLE
        _elapsedDisplay.value = "00:00"
        _pauseSegments.value = emptyList()
        _endConfirmVisible.value = false
        pausedElapsedMillis = 0L
        timerJob?.cancel()
        timerJob = null

        viewModelScope.launch {
            taskRepository.getTaskById(taskId)?.let { task ->
                _editingTaskId.value = task.id
                _title.value = task.title
                _startHour.value = task.startMinute / 60
                _startMinute.value = task.startMinute % 60
                _endHour.value = task.endMinute / 60
                _endMinute.value = task.endMinute % 60
                _notes.value = task.notes
                val colorIdx = availableColors.indexOf(task.color).let {
                    if (it >= 0) it else 0
                }
                _selectedColorIndex.value = colorIdx
                _pauseSegments.value = task.pauseSegments
            }
        }
    }

    fun resetForNew() {
        _editingTaskId.value = null
        _title.value = ""
        _startHour.value = 9
        _startMinute.value = 0
        _endHour.value = 10
        _endMinute.value = 0
        _notes.value = ""
        _selectedColorIndex.value = 0
        _timerState.value = TimerState.IDLE
        _elapsedDisplay.value = "00:00"
        _pauseSegments.value = emptyList()
        _endConfirmVisible.value = false
        pausedElapsedMillis = 0L
        timerJob?.cancel()
        timerJob = null
    }

    fun onTitleChange(value: String) { _title.value = value }
    fun onStartHourChange(value: Int) { _startHour.value = value }
    fun onStartMinuteChange(value: Int) { _startMinute.value = value }
    fun onEndHourChange(value: Int) { _endHour.value = value }
    fun onEndMinuteChange(value: Int) { _endMinute.value = value }
    fun onColorIndexChange(value: Int) { _selectedColorIndex.value = value }
    fun onNotesChange(value: String) { _notes.value = value }

    fun save() {
        val title = _title.value.trim()
        if (title.isEmpty()) {
            viewModelScope.launch { _errorEvent.emit("标题不能为空") }
            return
        }

        val startMin = _startHour.value * 60 + _startMinute.value
        val endMin = _endHour.value * 60 + _endMinute.value

        if (startMin >= endMin) {
            viewModelScope.launch { _errorEvent.emit("开始时间必须早于结束时间") }
            return
        }

        if (endMin > 1439) {
            viewModelScope.launch { _errorEvent.emit("结束时间不能超过 23:59") }
            return
        }

        val existingId = _editingTaskId.value
        val task = Task(
            id = existingId ?: 0,
            title = title,
            dateMillis = currentDateMillis,
            startMinute = startMin,
            endMinute = endMin,
            color = availableColors[_selectedColorIndex.value],
            notes = _notes.value.trim(),
            orderIndex = 0,
            pauseSegments = _pauseSegments.value
        )

        viewModelScope.launch {
            if (existingId != null) {
                taskRepository.updateTask(task)
            } else {
                taskRepository.insertTask(task)
            }
            _dismissEvent.emit(Unit)
        }
    }

    fun delete() {
        val existingId = _editingTaskId.value ?: return
        viewModelScope.launch {
            taskRepository.deleteTaskById(existingId)
            _dismissEvent.emit(Unit)
        }
    }

    fun getFormattedStartTime(): String {
        return String.format("%02d:%02d", _startHour.value, _startMinute.value)
    }

    fun getFormattedEndTime(): String {
        return String.format("%02d:%02d", _endHour.value, _endMinute.value)
    }
}