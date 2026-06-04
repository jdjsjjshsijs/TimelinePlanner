package com.example.timelineplanner.data.repository

import android.util.Log
import com.example.timelineplanner.data.db.CourseDao
import com.example.timelineplanner.data.db.CourseEntity
import com.example.timelineplanner.data.db.PracticeDao
import com.example.timelineplanner.data.db.PracticeRecordEntity
import com.example.timelineplanner.data.db.PracticeSubjectEntity
import com.example.timelineplanner.data.db.TaskDao
import com.example.timelineplanner.data.remote.SyncApi
import com.example.timelineplanner.data.remote.SyncCourse
import com.example.timelineplanner.data.remote.SyncPracticeRecord
import com.example.timelineplanner.data.remote.SyncPracticeRequest
import com.example.timelineplanner.data.remote.SyncPracticeSubject
import com.example.timelineplanner.data.remote.SyncRequest
import com.example.timelineplanner.data.remote.SyncTask
import com.example.timelineplanner.model.Task
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepository @Inject constructor(
    private val syncApi: SyncApi,
    private val taskDao: TaskDao,
    private val practiceDao: PracticeDao,
    private val courseDao: CourseDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gson = Gson()

    private val pendingSyncs = mutableMapOf<Long, Job>()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    companion object {
        private val SEGMENT_LIST_TYPE = object : TypeToken<List<List<Int>>>() {}.type
        private const val SYNC_DEBOUNCE_MS = 500L
    }

    fun syncDate(dateMillis: Long) {
        pendingSyncs[dateMillis]?.cancel()
        pendingSyncs[dateMillis] = scope.launch {
            delay(SYNC_DEBOUNCE_MS)
            _isSyncing.value = true
            try {
                val entities = taskDao.getTasksByDateOnce(dateMillis)
                val tasks = entities.map { e ->
                    val segments: List<List<Int>> = try {
                        gson.fromJson(e.pauseSegments, SEGMENT_LIST_TYPE) ?: emptyList()
                    } catch (_: Exception) {
                        emptyList()
                    }
                    SyncTask(
                        id = e.id,
                        title = e.title,
                        dateMillis = e.dateMillis,
                        startMinute = e.startMinute,
                        endMinute = e.endMinute,
                        color = e.color,
                        notes = e.notes,
                        orderIndex = e.orderIndex,
                        pauseSegments = segments
                    )
                }
                syncApi.syncTasks(SyncRequest(dateMillis = dateMillis, tasks = tasks))
                Log.d("Sync", "Synced ${tasks.size} tasks for date $dateMillis")
            } catch (e: Exception) {
                Log.w("Sync", "Sync failed: ${e.message}")
            } finally {
                _isSyncing.value = false
                pendingSyncs.remove(dateMillis)
            }
        }
    }

    fun syncPractice() {
        scope.launch {
            _isSyncing.value = true
            try {
                val subjects = practiceDao.getAllSubjectsOnce().map { s ->
                    SyncPracticeSubject(s.id, s.name, s.color, s.createdAt)
                }
                val records = practiceDao.getAllRecordsOnce().map { r ->
                    SyncPracticeRecord(r.id, r.subjectId, r.totalQuestions, r.correctQuestions, r.accuracy, r.dateMillis, r.notes)
                }
                syncApi.syncPractice(SyncPracticeRequest(subjects, records))
                Log.d("Sync", "Synced ${subjects.size} practice subjects, ${records.size} records")
            } catch (e: Exception) {
                Log.w("Sync", "Practice sync failed: ${e.message}")
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun syncCourses() {
        scope.launch {
            _isSyncing.value = true
            try {
                val courses = courseDao.getAllCoursesOnce().map { c ->
                    SyncCourse(c.id, c.title, c.location, c.teacher, c.daysOfWeek, c.startMinute, c.endMinute, c.color, c.notes, c.startDate, c.endDate)
                }
                syncApi.syncCourses(courses)
                Log.d("Sync", "Synced ${courses.size} courses")
            } catch (e: Exception) {
                Log.w("Sync", "Course sync failed: ${e.message}")
            } finally {
                _isSyncing.value = false
            }
        }
    }

    suspend fun fetchAllTasks(): List<Task>? {
        return try {
            val response = syncApi.getAllTasks()
            response.tasks.map { st ->
                Task(
                    id = st.id,
                    title = st.title,
                    dateMillis = st.dateMillis,
                    startMinute = st.startMinute,
                    endMinute = st.endMinute,
                    color = st.color,
                    notes = st.notes,
                    orderIndex = st.orderIndex,
                    pauseSegments = st.pauseSegments.map { it[0] to it[1] }
                )
            }
        } catch (e: Exception) {
            Log.w("Sync", "Fetch failed: ${e.message}")
            null
        }
    }

    suspend fun restoreAllFromServer(): Boolean {
        _isSyncing.value = true
        try {
            // Restore tasks
            val serverTasks = fetchAllTasks()
            if (serverTasks != null) {
                for (task in serverTasks) {
                    val existing = taskDao.getTaskById(task.id)
                    if (existing == null) {
                        val segments = task.pauseSegments.map { listOf(it.first, it.second) }
                        taskDao.insertTaskWithId(
                            task.id, task.title, task.dateMillis,
                            task.startMinute, task.endMinute, task.color,
                            task.notes, task.orderIndex, gson.toJson(segments)
                        )
                    }
                }
            }

            // Restore practice data
            val practiceData = try { syncApi.getAllPractice() } catch (_: Exception) { null }
            if (practiceData != null) {
                for (s in practiceData.subjects) {
                    val existing = practiceDao.getSubjectById(s.id)
                    if (existing == null) {
                        practiceDao.insertSubject(
                            PracticeSubjectEntity(s.id, s.name, s.color, s.createdAt)
                        )
                    }
                }
                for (r in practiceData.records) {
                    practiceDao.insertRecord(
                        PracticeRecordEntity(r.id, r.subjectId, r.totalQuestions, r.correctQuestions, r.accuracy, r.dateMillis, r.notes)
                    )
                }
            }

            // Restore courses
            val courseData = try { syncApi.getAllCourses() } catch (_: Exception) { null }
            if (courseData != null) {
                for (c in courseData.courses) {
                    val existing = courseDao.getCourseById(c.id)
                    if (existing == null) {
                        courseDao.insertCourse(
                            CourseEntity(c.id, c.title, c.location, c.teacher, c.daysOfWeek, c.startMinute, c.endMinute, c.color, c.notes, c.startDate, c.endDate)
                        )
                    }
                }
            }

            Log.d("Sync", "Restore from server complete")
            return true
        } catch (e: Exception) {
            Log.w("Sync", "Restore failed: ${e.message}")
            return false
        } finally {
            _isSyncing.value = false
        }
    }
}
