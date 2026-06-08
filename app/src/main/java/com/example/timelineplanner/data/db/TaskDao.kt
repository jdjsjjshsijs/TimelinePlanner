package com.example.timelineplanner.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks WHERE dateMillis = :dateMillis ORDER BY startMinute ASC")
    fun getTasksByDate(dateMillis: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE dateMillis = :dateMillis ORDER BY startMinute ASC")
    suspend fun getTasksByDateOnce(dateMillis: Long): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Query("INSERT OR REPLACE INTO tasks (id, title, dateMillis, startMinute, endMinute, color, notes, orderIndex, pauseSegments) VALUES (:id, :title, :dateMillis, :startMinute, :endMinute, :color, :notes, :orderIndex, :pauseSegments)")
    suspend fun insertTaskWithId(id: Long, title: String, dateMillis: Long, startMinute: Int, endMinute: Int, color: String, notes: String, orderIndex: Int, pauseSegments: String): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    @Query("DELETE FROM tasks WHERE id IN (:ids)")
    suspend fun deleteTasksByIds(ids: List<Long>)

    @Query("SELECT * FROM tasks ORDER BY dateMillis ASC, startMinute ASC")
    suspend fun getAllTasksOnce(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE dateMillis >= :startMillis AND dateMillis < :endMillis ORDER BY dateMillis ASC, startMinute ASC")
    suspend fun getTasksByDateRange(startMillis: Long, endMillis: Long): List<TaskEntity>
}
