package com.example.timelineplanner.data.repository

import android.util.Log
import com.example.timelineplanner.data.db.CourseDao
import com.example.timelineplanner.data.db.CourseEntity
import com.example.timelineplanner.data.remote.SyncApi
import com.example.timelineplanner.data.remote.SyncCourse
import com.example.timelineplanner.model.Course
import com.example.timelineplanner.model.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Calendar

class CourseRepository(
    private val courseDao: CourseDao
) {
    private var syncApi: SyncApi? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var cachedCourses: List<Course>? = null

    fun setSyncApi(api: SyncApi) { syncApi = api }

    fun getAllCourses(): Flow<List<Course>> {
        return courseDao.getAllCourses().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    suspend fun getAllCoursesOnce(): List<Course> {
        return cachedCourses ?: courseDao.getAllCoursesOnce().map { it.toDomainModel() }.also {
            cachedCourses = it
        }
    }

    suspend fun getCourseById(id: Long): Course? {
        return courseDao.getCourseById(id)?.toDomainModel()
    }

    suspend fun insertCourse(course: Course): Long {
        val id = courseDao.insertCourse(course.toEntity())
        cachedCourses = null
        syncAllCourses()
        return id
    }

    suspend fun updateCourse(course: Course) {
        courseDao.updateCourse(course.toEntity())
        cachedCourses = null
        syncAllCourses()
    }

    suspend fun deleteCourseById(id: Long) {
        courseDao.deleteCourseById(id)
        cachedCourses = null
        syncAllCourses()
    }

    suspend fun generateCourseTasksForDate(dateMillis: Long): List<Task> {
        val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val isoDayOfWeek = if (dayOfWeek == Calendar.SUNDAY) 7 else dayOfWeek - 1

        val courses = courseDao.getAllCoursesOnce()
        return courses.filter { course ->
            isoDayOfWeek in parseDaysOfWeek(course.daysOfWeek) &&
                dateMillis in course.startDate..course.endDate
        }.map { course ->
            Task(
                id = -(course.id * 10 + isoDayOfWeek),
                title = course.title,
                dateMillis = dateMillis,
                startMinute = course.startMinute,
                endMinute = course.endMinute,
                color = course.color,
                notes = buildString {
                    if (course.location.isNotBlank()) append("📍${course.location}")
                    if (course.teacher.isNotBlank()) {
                        if (isNotEmpty()) append(" | ")
                        append("👤${course.teacher}")
                    }
                    if (course.notes.isNotBlank()) {
                        if (isNotEmpty()) append("\n")
                        append(course.notes)
                    }
                },
                orderIndex = 0,
                pauseSegments = emptyList()
            )
        }
    }

    private fun syncAllCourses() {
        val api = syncApi ?: return
        scope.launch {
            try {
                val courses = courseDao.getAllCoursesOnce().map { cr ->
                    SyncCourse(cr.id, cr.title, cr.location, cr.teacher, cr.daysOfWeek, cr.startMinute, cr.endMinute, cr.color, cr.notes, cr.startDate, cr.endDate)
                }
                api.syncCourses(courses)
                Log.d("CourseRepo", "Synced ${courses.size} courses")
            } catch (e: Exception) {
                Log.w("CourseRepo", "Course sync failed: ${e.message}")
            }
        }
    }

    private fun parseDaysOfWeek(value: String): Set<Int> {
        return value.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
    }

    private fun CourseEntity.toDomainModel(): Course = Course(
        id = id, title = title, location = location, teacher = teacher,
        daysOfWeek = parseDaysOfWeek(daysOfWeek), startMinute = startMinute,
        endMinute = endMinute, color = color, notes = notes,
        startDate = startDate, endDate = endDate
    )

    private fun Course.toEntity(): CourseEntity = CourseEntity(
        id = id, title = title, location = location, teacher = teacher,
        daysOfWeek = daysOfWeek.sorted().joinToString(","),
        startMinute = startMinute, endMinute = endMinute,
        color = color, notes = notes, startDate = startDate, endDate = endDate
    )
}
