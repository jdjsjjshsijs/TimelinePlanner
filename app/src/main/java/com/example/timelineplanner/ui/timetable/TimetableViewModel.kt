package com.example.timelineplanner.ui.timetable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.timelineplanner.data.repository.CourseRepository
import com.example.timelineplanner.data.repository.SyncRepository
import com.example.timelineplanner.model.Course
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TimetableViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val syncRepository: SyncRepository
) : ViewModel() {

    private val _courses = MutableStateFlow<List<Course>>(emptyList())
    val courses: StateFlow<List<Course>> = _courses.asStateFlow()

    init {
        viewModelScope.launch {
            courseRepository.getAllCourses().collect {
                _courses.value = it
            }
        }
    }

    fun deleteCourse(course: Course) {
        viewModelScope.launch {
            courseRepository.deleteCourseById(course.id)
            syncRepository.syncCourses()
        }
    }
}
