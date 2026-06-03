package com.example.timelineplanner.data.repository

import com.example.timelineplanner.data.db.TaskDao
import com.example.timelineplanner.data.db.TaskEntity
import com.example.timelineplanner.model.Task
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val syncRepository: SyncRepository
) {
    private val gson = Gson()
    fun getTasksByDate(dateMillis: Long): Flow<List<Task>> {
        return taskDao.getTasksByDate(dateMillis).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    suspend fun getTasksByDateOnce(dateMillis: Long): List<Task> {
        return taskDao.getTasksByDateOnce(dateMillis).map { it.toDomainModel() }
    }

    suspend fun getTaskById(id: Long): Task? {
        return taskDao.getTaskById(id)?.toDomainModel()
    }

    suspend fun insertTask(task: Task): Long {
        val entity = task.toEntity()
        val id = taskDao.insertTask(entity)
        syncRepository.syncDate(task.dateMillis)
        return id
    }

    suspend fun restoreTask(task: Task) {
        val segments = task.pauseSegments.map { listOf(it.first, it.second) }
        val existing = taskDao.getTaskById(task.id)
        if (existing != null) return
        taskDao.insertTaskWithId(
            id = task.id,
            title = task.title,
            dateMillis = task.dateMillis,
            startMinute = task.startMinute,
            endMinute = task.endMinute,
            color = task.color,
            notes = task.notes,
            orderIndex = task.orderIndex,
            pauseSegments = gson.toJson(segments)
        )
    }

    suspend fun updateTask(task: Task) {
        taskDao.updateTask(task.toEntity())
        syncRepository.syncDate(task.dateMillis)
    }

    suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task.toEntity())
    }

    suspend fun deleteTaskById(id: Long) {
        val task = taskDao.getTaskById(id)
        taskDao.deleteTaskById(id)
        task?.let { syncRepository.syncDate(it.dateMillis) }
    }

    suspend fun deleteTasksByIds(ids: List<Long>) {
        val dates = ids.mapNotNull { taskDao.getTaskById(it)?.dateMillis }.distinct()
        taskDao.deleteTasksByIds(ids)
        dates.forEach { syncRepository.syncDate(it) }
    }

    suspend fun updateTaskTimer(
        taskId: Long,
        startMinute: Int,
        endMinute: Int,
        pauseSegments: List<Pair<Int, Int>>
    ) {
        val existing = taskDao.getTaskById(taskId) ?: return
        val segments = pauseSegments.map { listOf(it.first, it.second) }
        taskDao.updateTask(
            existing.copy(
                startMinute = startMinute,
                endMinute = endMinute,
                pauseSegments = gson.toJson(segments)
            )
        )
        syncRepository.syncDate(existing.dateMillis)
    }

    private fun TaskEntity.toDomainModel(): Task {
        val type = object : TypeToken<List<List<Int>>>() {}.type
        val segments: List<List<Int>> = try {
            gson.fromJson(pauseSegments, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
        return Task(
            id = id,
            title = title,
            dateMillis = dateMillis,
            startMinute = startMinute,
            endMinute = endMinute,
            color = color,
            notes = notes,
            orderIndex = orderIndex,
            pauseSegments = segments.map { (it[0] to it[1]) }
        )
    }

    private fun Task.toEntity(): TaskEntity {
        val segments = pauseSegments.map { listOf(it.first, it.second) }
        return TaskEntity(
            id = id,
            title = title,
            dateMillis = dateMillis,
            startMinute = startMinute,
            endMinute = endMinute,
            color = color,
            notes = notes,
            orderIndex = orderIndex,
            pauseSegments = gson.toJson(segments)
        )
    }
}
