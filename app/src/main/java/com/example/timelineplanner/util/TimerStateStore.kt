package com.example.timelineplanner.util

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimerStateStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("timer_state", Context.MODE_PRIVATE)

    fun savePausedState(
        taskId: Long,
        elapsedMillis: Long,
        pauseSegments: List<Pair<Int, Int>>,
        pauseStartMinute: Int,
        startMinute: Int
    ) {
        prefs.edit().apply {
            putLong("task_id", taskId)
            putLong("elapsed_millis", elapsedMillis)
            putString("pause_segments", serializeSegments(pauseSegments))
            putInt("pause_start_minute", pauseStartMinute)
            putInt("start_minute", startMinute)
            putLong("pause_wall_millis", System.currentTimeMillis())
            putBoolean("was_running", false)
            apply()
        }
    }

    fun saveRunningState(
        taskId: Long,
        elapsedMillis: Long,
        pauseSegments: List<Pair<Int, Int>>,
        startMinute: Int
    ) {
        prefs.edit().apply {
            putLong("task_id", taskId)
            putLong("elapsed_millis", elapsedMillis)
            putString("pause_segments", serializeSegments(pauseSegments))
            putInt("start_minute", startMinute)
            putLong("pause_wall_millis", System.currentTimeMillis())
            putBoolean("was_running", true)
            apply()
        }
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun hasPausedState(): Boolean {
        if (!prefs.contains("task_id")) return false
        val wallMillis = prefs.getLong("pause_wall_millis", 0L)
        val oneHourAgo = System.currentTimeMillis() - 60 * 60 * 1000L
        if (wallMillis < oneHourAgo) {
            clear()
            return false
        }
        return true
    }

    fun wasRunning(): Boolean = prefs.getBoolean("was_running", false)

    fun getTaskId(): Long = prefs.getLong("task_id", -1)

    fun restore(): RestoredTimerState? {
        if (!hasPausedState()) return null
        val taskId = prefs.getLong("task_id", -1)
        val elapsedMillis = prefs.getLong("elapsed_millis", 0)
        val segments = deserializeSegments(prefs.getString("pause_segments", "") ?: "")
        val pauseStartMinute = prefs.getInt("pause_start_minute", 0)
        val startMinute = prefs.getInt("start_minute", 0)
        val pauseWallMillis = prefs.getLong("pause_wall_millis", System.currentTimeMillis())

        // app close period: timer was paused or running, elapsed depends on was_running
        // If was_running, calculate extra elapsed from wall time difference
        val wasRunning = prefs.getBoolean("was_running", false)
        val extraElapsed = if (wasRunning) {
            (System.currentTimeMillis() - pauseWallMillis).coerceAtLeast(0)
        } else 0L

        return RestoredTimerState(
            taskId = taskId,
            elapsedMillis = elapsedMillis + extraElapsed,
            pauseSegments = segments,
            startMinute = startMinute,
            wasRunning = wasRunning
        )
    }

    private fun serializeSegments(segments: List<Pair<Int, Int>>): String {
        return segments.joinToString(";") { "${it.first},${it.second}" }
    }

    private fun deserializeSegments(s: String): List<Pair<Int, Int>> {
        if (s.isBlank()) return emptyList()
        return s.split(";").mapNotNull { part ->
            val parts = part.split(",")
            if (parts.size == 2) parts[0].toIntOrNull()?.let { a ->
                parts[1].toIntOrNull()?.let { b -> a to b }
            } else null
        }
    }
}

data class RestoredTimerState(
    val taskId: Long,
    val elapsedMillis: Long,
    val pauseSegments: List<Pair<Int, Int>>,
    val startMinute: Int,
    val wasRunning: Boolean = false
)
