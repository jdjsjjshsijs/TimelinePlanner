package com.example.timelineplanner.data.repository

import android.util.Log
import com.example.timelineplanner.data.ai.AiOperationExecutor
import com.example.timelineplanner.data.ai.AiPromptBuilder
import com.example.timelineplanner.data.ai.AiResponseParser
import com.example.timelineplanner.data.remote.DeepSeekChatMessage
import com.example.timelineplanner.data.remote.DeepSeekApi
import com.example.timelineplanner.data.remote.DeepSeekClient
import com.example.timelineplanner.model.Task
import android.content.SharedPreferences
import com.example.timelineplanner.util.ONE_DAY_MILLIS
import com.example.timelineplanner.util.formatDateFull
import com.example.timelineplanner.util.todayStartMillis
import javax.inject.Inject
import javax.inject.Singleton

data class ChatResult(
    val reply: String,
    val affectedDateMillis: Long? = null,
    val pendingDeleteTasks: List<Task> = emptyList()
)

@Singleton
class AiTaskRepository @Inject constructor(
    private val prefs: SharedPreferences,
    private val taskRepository: TaskRepository
) {
    private val responseParser = AiResponseParser()
    private val promptBuilder = AiPromptBuilder()
    private val operationExecutor = AiOperationExecutor(taskRepository)

    private var cachedApi: DeepSeekApi? = null
    private var cachedBaseUrl: String? = null
    private var cachedApiKey: String? = null

    private fun createApi(): DeepSeekApi {
        val baseUrl = prefs.getString("base_url", "http://115.190.253.67:3000") ?: "http://115.190.253.67:3000"
        val apiKey = prefs.getString("api_key", "") ?: ""
        if (cachedApi != null && cachedBaseUrl == baseUrl && cachedApiKey == apiKey) {
            return cachedApi!!
        }
        cachedApi = DeepSeekClient(baseUrl, apiKey).createApi()
        cachedBaseUrl = baseUrl
        cachedApiKey = apiKey
        return cachedApi!!
    }

    suspend fun deleteTasks(tasks: List<Task>): List<String> {
        val results = mutableListOf<String>()
        try {
            val ids = tasks.map { it.id }
            Log.d("AiTaskRepo", "Batch delete: ids=$ids")
            taskRepository.deleteTasksByIds(ids)
            for (task in tasks) {
                val stillExists = taskRepository.getTaskById(task.id)
                if (stillExists != null) {
                    results.add("删除失败「${task.title}」(id=${task.id})")
                } else {
                    results.add("已删除「${task.title}」")
                }
            }
        } catch (e: Exception) {
            Log.e("AiTaskRepo", "Batch delete exception: ${e.message}")
            results.add("批量删除异常：${e.message}")
        }
        return results
    }

    suspend fun processChat(
        userMessage: String,
        dateMillis: Long,
        historyMessages: List<DeepSeekChatMessage> = emptyList()
    ): ChatResult {
        val todayMillis = todayStartMillis()
        val todayStr = formatDateFull(todayMillis)
        val viewingDateStr = formatDateFull(dateMillis)

        val taskContext = promptBuilder.buildTaskContext(todayMillis, dateMillis) { millis ->
            taskRepository.getTasksByDateOnce(millis)
        }

        val systemPrompt = promptBuilder.buildSystemPrompt(todayStr, todayMillis, viewingDateStr, dateMillis)

        val messages = mutableListOf(
            DeepSeekChatMessage(role = "system", content = systemPrompt),
            DeepSeekChatMessage(role = "user", content = "各日期任务列表：\n$taskContext")
        )
        messages.addAll(historyMessages)
        messages.add(DeepSeekChatMessage(role = "user", content = userMessage))

        val api = createApi()
        val currentModel = prefs.getString("model", "deepseek-chat") ?: "deepseek-chat"
        var response = api.chat(
            request = com.example.timelineplanner.data.remote.ChatRequest(
                model = currentModel,
                messages = messages
            )
        )

        val apiError = response.error
        if (apiError != null) {
            return ChatResult("API 错误：${apiError.message}")
        }

        val rawContent = response.choices?.firstOrNull()?.message?.content
            ?: return ChatResult("AI 返回了空响应")

        Log.d("AiTaskRepo", "AI raw response: $rawContent")

        var aiResponse = responseParser.parse(rawContent)
        Log.d("AiTaskRepo", "Parsed response: $aiResponse")

        // 如果解析失败，重试一次
        if (aiResponse == null) {
            Log.w("AiTaskRepo", "First parse failed, retrying...")
            response = api.chat(
                request = com.example.timelineplanner.data.remote.ChatRequest(
                    model = currentModel,
                    messages = messages + DeepSeekChatMessage(
                        role = "user",
                        content = "你的上一条回复无法解析为 JSON。请只输出一个合法的 JSON 对象，格式：{\"natural_reply\":\"回复内容\",\"operations\":[]}  不要输出任何其他文字。"
                    )
                )
            )
            val retryContent = response.choices?.firstOrNull()?.message?.content ?: ""
            Log.d("AiTaskRepo", "Retry response: $retryContent")
            aiResponse = responseParser.parse(retryContent)
        }

        if (aiResponse == null) {
            return ChatResult("AI 暂无法处理，请稍后再试\n\n[调试] 原始响应：${rawContent.take(200)}")
        }

        val operations = aiResponse.operations ?: emptyList()
        val opResults = operationExecutor.execute(operations, dateMillis)

        val reply = aiResponse.naturalReply ?: "已完成操作"
        val fullReply = if (opResults.messages.isNotEmpty()) {
            "$reply\n\n${opResults.messages.joinToString("\n") { "• $it" }}"
        } else {
            reply
        }

        val deleteHint = if (opResults.pendingDeletes.isNotEmpty()) {
            val names = opResults.pendingDeletes.joinToString("、") { "「${it.title}」" }
            "\n\n待确认删除：$names"
        } else ""

        return ChatResult(
            reply = fullReply + deleteHint,
            affectedDateMillis = opResults.affectedDate,
            pendingDeleteTasks = opResults.pendingDeletes
        )
    }
}
