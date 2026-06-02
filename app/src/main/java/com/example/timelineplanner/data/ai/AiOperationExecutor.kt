package com.example.timelineplanner.data.ai

import android.util.Log
import com.example.timelineplanner.data.repository.TaskRepository
import com.example.timelineplanner.model.Task

data class OperationResult(
    val messages: List<String>,
    val affectedDate: Long? = null,
    val pendingDeletes: List<Task> = emptyList()
)

class AiOperationExecutor(
    private val taskRepository: TaskRepository
) {

    suspend fun execute(
        operations: List<AiOperation>,
        defaultDateMillis: Long
    ): OperationResult {
        val messages = mutableListOf<String>()
        var affectedDate: Long? = null
        val pendingDeletes = mutableListOf<Task>()
        val alreadyMatchedIds = mutableSetOf<Long>()

        for (op in operations) {
            try {
                when (op.type) {
                    "create" -> executeCreate(op, defaultDateMillis, messages).let {
                        affectedDate = it
                    }
                    "update" -> executeUpdate(op, defaultDateMillis, messages).let {
                        if (it != null) affectedDate = it
                    }
                    "delete" -> executeDelete(op, defaultDateMillis, alreadyMatchedIds, messages, pendingDeletes).let {
                        if (it != null) affectedDate = it
                    }
                }
            } catch (e: Exception) {
                messages.add("执行操作失败：${e.message}")
            }
        }

        return OperationResult(messages, affectedDate, pendingDeletes)
    }

    private suspend fun executeCreate(
        op: AiOperation,
        defaultDateMillis: Long,
        messages: MutableList<String>
    ): Long? {
        val data = op.data ?: return null
        val title = data["title"] as? String ?: return null
        val startMinute = (data["startMinute"] as? Double)?.toInt() ?: return null
        val endMinute = (data["endMinute"] as? Double)?.toInt() ?: return null
        val color = data["color"] as? String ?: colorForTitle(title)
        val notes = data["notes"] as? String ?: ""
        val taskDate = (data["dateMillis"] as? Double)?.toLong() ?: defaultDateMillis

        val task = Task(
            title = title,
            dateMillis = taskDate,
            startMinute = startMinute.coerceIn(0, 1439),
            endMinute = endMinute.coerceIn(0, 1439),
            color = color,
            notes = notes
        )
        taskRepository.insertTask(task)
        messages.add("已创建任务：$title")
        return taskDate
    }

    private suspend fun executeUpdate(
        op: AiOperation,
        defaultDateMillis: Long,
        messages: MutableList<String>
    ): Long? {
        val criteria = op.criteria ?: return null
        val data = op.data ?: return null
        val criteriaTitle = criteria["title"] as? String ?: return null
        val criteriaDate = (criteria["dateMillis"] as? Double)?.toLong() ?: defaultDateMillis

        val allTasks = taskRepository.getTasksByDateOnce(criteriaDate)
        val matchedTasks = allTasks.filter {
            it.title.equals(criteriaTitle, ignoreCase = true)
        }

        if (matchedTasks.isEmpty()) {
            messages.add("未找到匹配任务：$criteriaTitle")
            return null
        }

        for (matchedTask in matchedTasks) {
            val newTitle = data["title"] as? String ?: matchedTask.title
            val newStart = (data["startMinute"] as? Double)?.toInt() ?: matchedTask.startMinute
            val newEnd = (data["endMinute"] as? Double)?.toInt() ?: matchedTask.endMinute
            val newColor = data["color"] as? String ?: matchedTask.color
            val newNotes = data["notes"] as? String ?: matchedTask.notes

            val updatedTask = matchedTask.copy(
                title = newTitle,
                startMinute = newStart.coerceIn(0, 1439),
                endMinute = newEnd.coerceIn(0, 1439),
                color = newColor,
                notes = newNotes
            )
            taskRepository.updateTask(updatedTask)
        }
        messages.add("已修改 ${matchedTasks.size} 个「$criteriaTitle」任务")
        return matchedTasks.first().dateMillis
    }

    private suspend fun executeDelete(
        op: AiOperation,
        defaultDateMillis: Long,
        alreadyMatchedIds: MutableSet<Long>,
        messages: MutableList<String>,
        pendingDeletes: MutableList<Task>
    ): Long? {
        val criteria = op.criteria ?: return null
        val criteriaTitle = criteria["title"] as? String ?: return null
        val criteriaDate = (criteria["dateMillis"] as? Double)?.toLong() ?: defaultDateMillis

        Log.d("AiOpExecutor", "Delete op: title=$criteriaTitle, dateMillis=$criteriaDate")

        val allTasks = taskRepository.getTasksByDateOnce(criteriaDate)
        Log.d("AiOpExecutor", "Tasks on $criteriaDate: ${allTasks.map { "${it.title}(id=${it.id})" }}")

        // 先精确匹配，再模糊匹配，排除已匹配的
        val availableTasks = allTasks.filter { it.id !in alreadyMatchedIds }
        val matchedTask = availableTasks.find {
            it.title.equals(criteriaTitle, ignoreCase = true)
        } ?: availableTasks.find {
            it.title.contains(criteriaTitle, ignoreCase = true) ||
                criteriaTitle.contains(it.title, ignoreCase = true)
        }

        if (matchedTask == null) {
            Log.w("AiOpExecutor", "No match found for: $criteriaTitle")
            messages.add("未找到匹配任务：$criteriaTitle")
            return null
        }

        Log.d("AiOpExecutor", "Matched: ${matchedTask.title} (id=${matchedTask.id})")
        pendingDeletes.add(matchedTask)
        alreadyMatchedIds.add(matchedTask.id)
        return matchedTask.dateMillis
    }

    companion object {
        private val colorPalette = listOf(
            "#4A90D9", "#5B9BD5", "#ED7D31", "#70AD47",
            "#FFC000", "#4472C4", "#9B59B6", "#45B39D",
            "#E74C3C", "#1ABC9C", "#F39C12", "#2ECC71"
        )

        fun colorForTitle(title: String): String {
            val index = (title.hashCode() and 0x7FFFFFFF) % colorPalette.size
            return colorPalette[index]
        }
    }
}
