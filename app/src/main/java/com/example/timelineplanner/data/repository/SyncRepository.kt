package com.example.timelineplanner.data.repository

import android.util.Log
import com.example.timelineplanner.data.db.TaskDao
import com.example.timelineplanner.data.remote.SyncApi
import com.example.timelineplanner.data.remote.SyncRequest
import com.example.timelineplanner.data.remote.SyncTask
import com.example.timelineplanner.model.Task
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepository @Inject constructor(
    private val syncApi: SyncApi,
    private val taskDao: TaskDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gson = Gson()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    fun syncDate(dateMillis: Long) {
        scope.launch {
            _isSyncing.value = true
            try {
                val entities = taskDao.getTasksByDateOnce(dateMillis)
                val tasks = entities.map { e ->
                    val type = object : TypeToken<List<List<Int>>>() {}.type
                    val segments: List<List<Int>> = try {
                        gson.fromJson(e.pauseSegments, type) ?: emptyList()
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
}
