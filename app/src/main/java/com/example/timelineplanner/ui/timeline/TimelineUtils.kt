package com.example.timelineplanner.ui.timeline

import androidx.compose.ui.graphics.Color
import com.example.timelineplanner.model.Task

data class TaskLayoutInfo(
    val task: Task,
    val maxOverlap: Int,
    val columnIndex: Int
)

fun computeTaskLayout(tasks: List<Task>): List<TaskLayoutInfo> {
    if (tasks.isEmpty()) return emptyList()

    val sorted = tasks.sortedBy { it.startMinute }
    val result = mutableListOf<TaskLayoutInfo>()

    // Step 1: Assign columns greedily
    for (task in sorted) {
        val overlapping = result.filter { it.task.startMinute < task.endMinute && it.task.endMinute > task.startMinute }
        val usedColumns = overlapping.map { it.columnIndex }.toSet()
        val column = (0..usedColumns.size).first { it !in usedColumns }
        result.add(TaskLayoutInfo(task, 1, column))
    }

    // Step 2: Compute correct maxOverlap by sweep line per task
    for (i in result.indices) {
        val task = result[i].task
        val overlapping = result.filter { other ->
            other.task.startMinute < task.endMinute && other.task.endMinute > task.startMinute
        }
        val events = mutableListOf<Pair<Int, Int>>()
        for (other in overlapping) {
            val effStart = maxOf(other.task.startMinute, task.startMinute)
            val effEnd = minOf(other.task.endMinute, task.endMinute)
            if (effStart < effEnd) {
                events.add(effStart to 1)
                events.add(effEnd to -1)
            }
        }
        events.sortWith(compareBy({ it.first }, { -it.second }))

        var current = 0
        var maxConcurrent = 0
        for ((_, delta) in events) {
            current += delta
            maxConcurrent = maxOf(maxConcurrent, current)
        }
        result[i] = result[i].copy(maxOverlap = maxConcurrent.coerceAtLeast(1))
    }

    return result
}

fun parseColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        Color(0xFF4A90D9)
    }
}

fun minuteToTimeString(totalMinutes: Int): String {
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return String.format("%02d:%02d", h, m)
}
