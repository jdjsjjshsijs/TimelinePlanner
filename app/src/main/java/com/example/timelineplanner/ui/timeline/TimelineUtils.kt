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

    for (task in sorted) {
        val overlapping = result.filter { it.task.startMinute < task.endMinute && it.task.endMinute > task.startMinute }
        val usedColumns = overlapping.map { it.columnIndex }.toSet()
        val column = (0..usedColumns.size).first { it !in usedColumns }
        val overlapGroup = overlapping + listOf(TaskLayoutInfo(task, 0, column))
        val maxOverlap = overlapGroup.size

        result.add(TaskLayoutInfo(task, maxOverlap, column))
        for (i in result.indices) {
            val existing = result[i]
            if (existing.task.startMinute < task.endMinute && existing.task.endMinute > task.startMinute) {
                val groupMax = maxOf(maxOverlap, existing.maxOverlap)
                result[i] = existing.copy(maxOverlap = groupMax)
            }
        }
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
