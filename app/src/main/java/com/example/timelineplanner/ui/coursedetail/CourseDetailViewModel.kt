package com.example.timelineplanner.ui.coursedetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.SharedPreferences
import com.example.timelineplanner.data.repository.CourseRepository
import com.example.timelineplanner.model.Course
import com.example.timelineplanner.ui.theme.TaskColors
import com.example.timelineplanner.util.ONE_DAY_MILLIS
import com.example.timelineplanner.util.todayStartMillis
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

private const val KEY_SEMESTER_START = "course_semester_start"
private const val KEY_SEMESTER_END = "course_semester_end"
private const val DEFAULT_SEMESTER_DAYS = 120L

@HiltViewModel
class CourseDetailViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    @Named("semester_prefs") private val semesterPrefs: SharedPreferences
) : ViewModel() {

    private fun loadSavedSemesterStart(): Long {
        val saved = semesterPrefs.getLong(KEY_SEMESTER_START, 0L)
        return if (saved > 0L) saved else todayStartMillis()
    }

    private fun loadSavedSemesterEnd(): Long {
        val saved = semesterPrefs.getLong(KEY_SEMESTER_END, 0L)
        return if (saved > 0L) saved else todayStartMillis() + DEFAULT_SEMESTER_DAYS * ONE_DAY_MILLIS
    }

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _location = MutableStateFlow("")
    val location: StateFlow<String> = _location.asStateFlow()

    private val _teacher = MutableStateFlow("")
    val teacher: StateFlow<String> = _teacher.asStateFlow()

    private val _selectedDays = MutableStateFlow<Set<Int>>(emptySet())
    val selectedDays: StateFlow<Set<Int>> = _selectedDays.asStateFlow()

    private val _startHour = MutableStateFlow(8)
    val startHour: StateFlow<Int> = _startHour.asStateFlow()

    private val _startMinute = MutableStateFlow(0)
    val startMinute: StateFlow<Int> = _startMinute.asStateFlow()

    private val _endHour = MutableStateFlow(9)
    val endHour: StateFlow<Int> = _endHour.asStateFlow()

    private val _endMinute = MutableStateFlow(40)
    val endMinute: StateFlow<Int> = _endMinute.asStateFlow()

    private val _selectedColorIndex = MutableStateFlow(0)
    val selectedColorIndex: StateFlow<Int> = _selectedColorIndex.asStateFlow()

    val availableColors = TaskColors

    private val _notes = MutableStateFlow("")
    val notes: StateFlow<String> = _notes.asStateFlow()

    private val _startDate = MutableStateFlow(loadSavedSemesterStart())
    val startDate: StateFlow<Long> = _startDate.asStateFlow()

    private val _endDate = MutableStateFlow(loadSavedSemesterEnd())
    val endDate: StateFlow<Long> = _endDate.asStateFlow()

    private val _editingCourseId = MutableStateFlow<Long?>(null)
    val editingCourseId: StateFlow<Long?> = _editingCourseId.asStateFlow()

    private val _dismissEvent = MutableSharedFlow<Unit>()
    val dismissEvent = _dismissEvent.asSharedFlow()

    private val _errorEvent = MutableSharedFlow<String>()
    val errorEvent = _errorEvent.asSharedFlow()

    fun resetForNew() {
        _editingCourseId.value = null
        _title.value = ""
        _location.value = ""
        _teacher.value = ""
        _selectedDays.value = emptySet()
        _startHour.value = 8
        _startMinute.value = 0
        _endHour.value = 9
        _endMinute.value = 40
        _notes.value = ""
        _selectedColorIndex.value = 0
        _startDate.value = loadSavedSemesterStart()
        _endDate.value = loadSavedSemesterEnd()
    }

    fun loadCourse(courseId: Long) {
        viewModelScope.launch {
            courseRepository.getCourseById(courseId)?.let { course ->
                _editingCourseId.value = course.id
                _title.value = course.title
                _location.value = course.location
                _teacher.value = course.teacher
                _selectedDays.value = course.daysOfWeek
                _startHour.value = course.startMinute / 60
                _startMinute.value = course.startMinute % 60
                _endHour.value = course.endMinute / 60
                _endMinute.value = course.endMinute % 60
                _notes.value = course.notes
                val colorIdx = availableColors.indexOf(course.color).let {
                    if (it >= 0) it else 0
                }
                _selectedColorIndex.value = colorIdx
                _startDate.value = course.startDate
                _endDate.value = course.endDate
            }
        }
    }

    fun save() {
        val titleVal = _title.value.trim()
        if (titleVal.isEmpty()) {
            viewModelScope.launch { _errorEvent.emit("课程名称不能为空") }
            return
        }
        if (_selectedDays.value.isEmpty()) {
            viewModelScope.launch { _errorEvent.emit("请至少选择一天") }
            return
        }
        val startMin = _startHour.value * 60 + _startMinute.value
        val endMin = _endHour.value * 60 + _endMinute.value
        if (startMin >= endMin) {
            viewModelScope.launch { _errorEvent.emit("开始时间必须早于结束时间") }
            return
        }
        if (_startDate.value > _endDate.value) {
            viewModelScope.launch { _errorEvent.emit("学期开始日期不能晚于结束日期") }
            return
        }

        val existingId = _editingCourseId.value
        val course = Course(
            id = existingId ?: 0,
            title = titleVal,
            location = _location.value.trim(),
            teacher = _teacher.value.trim(),
            daysOfWeek = _selectedDays.value,
            startMinute = startMin,
            endMinute = endMin,
            color = availableColors[_selectedColorIndex.value],
            notes = _notes.value.trim(),
            startDate = _startDate.value,
            endDate = _endDate.value
        )

        viewModelScope.launch {
            if (existingId != null) {
                courseRepository.updateCourse(course)
            } else {
                courseRepository.insertCourse(course)
            }
            // 记住学期范围，下次新建课程时自动填入
            semesterPrefs.edit()
                .putLong(KEY_SEMESTER_START, _startDate.value)
                .putLong(KEY_SEMESTER_END, _endDate.value)
                .apply()
            _dismissEvent.emit(Unit)
        }
    }

    fun delete() {
        val existingId = _editingCourseId.value ?: return
        viewModelScope.launch {
            courseRepository.deleteCourseById(existingId)
            _dismissEvent.emit(Unit)
        }
    }

    fun onTitleChange(value: String) { _title.value = value }
    fun onLocationChange(value: String) { _location.value = value }
    fun onTeacherChange(value: String) { _teacher.value = value }
    fun onStartHourChange(value: Int) { _startHour.value = value }
    fun onStartMinuteChange(value: Int) { _startMinute.value = value }
    fun onEndHourChange(value: Int) { _endHour.value = value }
    fun onEndMinuteChange(value: Int) { _endMinute.value = value }
    fun onColorIndexChange(value: Int) { _selectedColorIndex.value = value }
    fun onNotesChange(value: String) { _notes.value = value }
    fun onStartDateChange(value: Long) { _startDate.value = value }
    fun onEndDateChange(value: Long) { _endDate.value = value }

    fun toggleDay(day: Int) {
        val current = _selectedDays.value
        _selectedDays.value = if (day in current) current - day else current + day
    }
}
