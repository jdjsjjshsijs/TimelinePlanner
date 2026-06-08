package com.example.timelineplanner.data.repository

import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import com.example.timelineplanner.data.db.CourseDao
import com.example.timelineplanner.data.db.CourseEntity
import com.example.timelineplanner.data.db.GoalDao
import com.example.timelineplanner.data.db.GoalEntity
import com.example.timelineplanner.data.db.PracticeDao
import com.example.timelineplanner.data.db.PracticeRecordEntity
import com.example.timelineplanner.data.db.PracticeSubjectEntity
import com.example.timelineplanner.data.db.TaskDao
import com.example.timelineplanner.data.remote.SyncApi
import com.example.timelineplanner.data.remote.SyncCourse
import com.example.timelineplanner.data.remote.SyncGoal
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepository @Inject constructor(
    private val syncApi: SyncApi,
    private val taskDao: TaskDao,
    private val practiceDao: PracticeDao,
    private val courseDao: CourseDao,
    private val goalDao: GoalDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gson = Gson()

    private fun api(): SyncApi = syncApi

    private val pendingSyncs = ConcurrentHashMap<Long, Job>()

    private val _syncCountAtomic = AtomicInteger(0)
    private val _syncCount = MutableStateFlow(0)
    val isSyncing: StateFlow<Boolean> = _syncCount.map { it > 0 }.stateIn(scope, SharingStarted.Eagerly, false)

    private val _lastSyncResult = MutableStateFlow<SyncResult>(SyncResult.Unknown)
    val lastSyncResult: StateFlow<SyncResult> = _lastSyncResult.asStateFlow()

    companion object {
        private val SEGMENT_LIST_TYPE = object : TypeToken<List<List<Int>>>() {}.type
        private const val SYNC_DEBOUNCE_MS = 500L
    }

    fun syncDate(dateMillis: Long) {
        pendingSyncs[dateMillis]?.cancel()
        pendingSyncs[dateMillis] = scope.launch {
            delay(SYNC_DEBOUNCE_MS)
            _syncCount.value = _syncCountAtomic.incrementAndGet()
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
                api().syncTasks(SyncRequest(dateMillis = dateMillis, tasks = tasks))
                Log.d("Sync", "Synced ${tasks.size} tasks for date $dateMillis")
                _lastSyncResult.value = SyncResult.Success
            } catch (e: Exception) {
                Log.w("Sync", "Sync failed: ${e.message}")
                _lastSyncResult.value = SyncResult.Failed
            } finally {
                _syncCount.value = _syncCountAtomic.decrementAndGet().coerceAtLeast(0)
                pendingSyncs.remove(dateMillis)
            }
        }
    }

    fun syncPractice() {
        scope.launch {
            _syncCount.value = _syncCountAtomic.incrementAndGet()
            try {
                val subjects = practiceDao.getAllSubjectsOnce().map { s ->
                    SyncPracticeSubject(s.id, s.name, s.color, s.createdAt)
                }
                val records = practiceDao.getAllRecordsOnce().map { r ->
                    SyncPracticeRecord(r.id, r.subjectId, r.totalQuestions, r.correctQuestions, r.accuracy, r.dateMillis, r.notes, r.createdAtMillis)
                }
                api().syncPractice(SyncPracticeRequest(subjects, records))
                Log.d("Sync", "Synced ${subjects.size} practice subjects, ${records.size} records")
                _lastSyncResult.value = SyncResult.Success
            } catch (e: Exception) {
                Log.w("Sync", "Practice sync failed: ${e.message}")
                _lastSyncResult.value = SyncResult.Failed
            } finally {
                _syncCount.value = _syncCountAtomic.decrementAndGet().coerceAtLeast(0)
            }
        }
    }

    fun deletePracticeSubject(subjectId: Long) {
        scope.launch {
            try {
                api().deletePracticeSubject(subjectId)
                Log.d("Sync", "Deleted practice subject $subjectId from server")
            } catch (e: Exception) {
                Log.w("Sync", "Delete practice subject failed: ${e.message}")
            }
        }
    }

    fun deletePracticeRecord(recordId: Long) {
        scope.launch {
            try {
                api().deletePracticeRecord(recordId)
                Log.d("Sync", "Deleted practice record $recordId from server")
            } catch (e: Exception) {
                Log.w("Sync", "Delete practice record failed: ${e.message}")
            }
        }
    }

    fun syncCourses() {
        scope.launch {
            _syncCount.value = _syncCountAtomic.incrementAndGet()
            try {
                val courses = courseDao.getAllCoursesOnce().map { c ->
                    SyncCourse(c.id, c.title, c.location, c.teacher, c.daysOfWeek, c.startMinute, c.endMinute, c.color, c.notes, c.startDate, c.endDate)
                }
                api().syncCourses(courses)
                Log.d("Sync", "Synced ${courses.size} courses")
                _lastSyncResult.value = SyncResult.Success
            } catch (e: Exception) {
                Log.w("Sync", "Course sync failed: ${e.message}")
                _lastSyncResult.value = SyncResult.Failed
            } finally {
                _syncCount.value = _syncCountAtomic.decrementAndGet().coerceAtLeast(0)
            }
        }
    }

    fun deleteCourse(courseId: Long) {
        scope.launch {
            try {
                api().deleteCourse(courseId)
                Log.d("Sync", "Deleted course $courseId from server")
            } catch (e: Exception) {
                Log.w("Sync", "Delete course failed: ${e.message}")
            }
        }
    }

    fun deleteGoal(goalId: Long) {
        scope.launch {
            try {
                api().deleteGoal(goalId)
                Log.d("Sync", "Deleted goal $goalId from server")
            } catch (e: Exception) {
                Log.w("Sync", "Delete goal failed: ${e.message}")
            }
        }
    }

    fun syncGoals() {
        scope.launch {
            _syncCount.value = _syncCountAtomic.incrementAndGet()
            try {
                val goals = goalDao.getAllGoalsOnce().map { g ->
                    SyncGoal(g.id, g.name, g.deadlineMillis, g.color, g.createdAt)
                }
                api().syncGoals(goals)
                Log.d("Sync", "Synced ${goals.size} goals")
                _lastSyncResult.value = SyncResult.Success
            } catch (e: Exception) {
                Log.w("Sync", "Goal sync failed: ${e.message}")
                _lastSyncResult.value = SyncResult.Failed
            } finally {
                _syncCount.value = _syncCountAtomic.decrementAndGet().coerceAtLeast(0)
            }
        }
    }

    suspend fun fetchAllTasks(): List<Task>? {
        return try {
            val response = api().getAllTasks()
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
                    pauseSegments = st.pauseSegments.filter { it.size >= 2 }.map { it[0] to it[1] }
                )
            }
        } catch (e: Exception) {
            Log.w("Sync", "Fetch failed: ${e.message}")
            null
        }
    }

    suspend fun restoreAllFromServer(): Boolean {
        Log.d("Sync", "restoreAllFromServer called")
        _syncCount.value = _syncCountAtomic.incrementAndGet()
        try {
            // Restore tasks
            val serverTasks = fetchAllTasks()
            Log.d("Sync", "serverTasks: ${serverTasks?.size} tasks")
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
            val practiceData = try { api().getAllPractice() } catch (_: Exception) { null }
            if (practiceData != null) {
                for (s in practiceData.subjects) {
                    val existing = practiceDao.getSubjectById(s.id)
                    if (existing == null) {
                        practiceDao.insertSubject(
                            PracticeSubjectEntity(s.id, s.name, s.color, s.createdAt)
                        )
                    }
                }
                val existingRecords = practiceDao.getAllRecordsOnce().map { it.id }.toSet()
                for (r in practiceData.records) {
                    if (r.id !in existingRecords) {
                        practiceDao.insertRecord(
                            PracticeRecordEntity(
                                r.id, r.subjectId, r.totalQuestions, r.correctQuestions,
                                r.accuracy, r.dateMillis, r.notes,
                                if (r.createdAtMillis > 0) r.createdAtMillis else System.currentTimeMillis()
                            )
                        )
                    }
                }
            }

            // Restore courses
            val courseData = try { api().getAllCourses() } catch (e: Exception) { Log.w("Sync", "Course fetch failed: ${e.message}"); null }
            if (courseData != null) {
                Log.d("Sync", "Restoring ${courseData.courses.size} courses from server")
                for (c in courseData.courses) {
                    val existing = courseDao.getCourseById(c.id)
                    if (existing == null) {
                        courseDao.insertCourse(
                            CourseEntity(c.id, c.title, c.location, c.teacher, c.daysOfWeek, c.startMinute, c.endMinute, c.color, c.notes, c.startDate, c.endDate)
                        )
                        Log.d("Sync", "Restored course: ${c.title}")
                    }
                }
            }

            // Restore goals
            val goalData = try { api().getAllGoals() } catch (_: Exception) { null }
            if (goalData != null) {
                for (g in goalData.goals) {
                    val existing = goalDao.getGoalById(g.id)
                    if (existing == null) {
                        goalDao.insertGoal(GoalEntity(g.id, g.name, g.deadlineMillis, g.color, if (g.createdAt > 0) g.createdAt else System.currentTimeMillis()))
                    }
                }
            }

            Log.d("Sync", "Restore from server complete - returning true")
            return true
        } catch (e: Exception) {
            Log.e("Sync", "Restore failed", e)
            return false
        } finally {
            _syncCount.value = _syncCountAtomic.decrementAndGet().coerceAtLeast(0)
        }
    }
}

enum class SyncResult { Unknown, Success, Failed }
