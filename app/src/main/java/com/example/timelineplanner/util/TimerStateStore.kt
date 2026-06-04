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
            apply()
        }
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun hasPausedState(): Boolean {
        if (!prefs.contains("task_id")) return false
        val pauseWallMillis = prefs.getLong("pause_wall_millis", 0L)
        val oneHourAgo = System.currentTimeMillis() - 60 * 60 * 1000L
        if (pauseWallMillis < oneHourAgo) {
            clear()
            return false
        }
        return true
    }

    fun getTaskId(): Long = prefs.getLong("task_id", -1)

    fun restore(): RestoredTimerState? {
        if (!hasPausedState()) return null
        val taskId = prefs.getLong("task_id", -1)
        val elapsedMillis = prefs.getLong("elapsed_millis", 0)
        val segments = deserializeSegments(prefs.getString("pause_segments", "") ?: "")
        val pauseStartMinute = prefs.getInt("pause_start_minute", 0)
        val startMinute = prefs.getInt("start_minute", 0)
        val pauseWallMillis = prefs.getLong("pause_wall_millis", System.currentTimeMillis())

        // app 关闭期间计时器处于暂停状态，elapsed 不增长
        // 暂停记录不在此处生成，等用户结束/继续时由 ViewModel 正确处理

        return RestoredTimerState(
            taskId = taskId,
            elapsedMillis = elapsedMillis,
            pauseSegments = segments,
            startMinute = startMinute
        )
    }

    private fun getCurrentMinuteOfDay(): Int {
        val cal = java.util.Calendar.getInstance()
        return cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
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
    val startMinute: Int
)
