package com.example.timelineplanner.ui.aichat

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.example.timelineplanner.data.db.ChatMessageDao
import com.example.timelineplanner.data.db.ChatMessageEntity
import com.example.timelineplanner.data.repository.AiTaskRepository
import com.example.timelineplanner.model.Task
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.example.timelineplanner.util.todayStartMillis
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AiChatViewModel @Inject constructor(
    private val aiTaskRepository: AiTaskRepository,
    private val chatMessageDao: ChatMessageDao,
    private val prefs: SharedPreferences
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _showApiKeySetup = MutableStateFlow(false)
    val showApiKeySetup: StateFlow<Boolean> = _showApiKeySetup.asStateFlow()

    private val _errorEvent = MutableSharedFlow<String>()
    val errorEvent = _errorEvent.asSharedFlow()

    private val _navigateToDateEvent = MutableSharedFlow<Long>()
    val navigateToDateEvent = _navigateToDateEvent.asSharedFlow()

    private val _pendingDeleteTasks = MutableStateFlow<List<Task>>(emptyList())
    val pendingDeleteTasks: StateFlow<List<Task>> = _pendingDeleteTasks.asStateFlow()

    private val _showDeleteConfirmDialog = MutableStateFlow(false)
    val showDeleteConfirmDialog: StateFlow<Boolean> = _showDeleteConfirmDialog.asStateFlow()

    private val _showEditSettings = MutableStateFlow(false)
    val showEditSettings: StateFlow<Boolean> = _showEditSettings.asStateFlow()

    private var currentDateMillis: Long = todayStartMillis()

    fun setCurrentDate(dateMillis: Long) {
        currentDateMillis = dateMillis
    }

    init {
        val hasKey = getApiKey() != null
        viewModelScope.launch {
            val savedMessages = chatMessageDao.getAllMessages().first()
            if (savedMessages.isNotEmpty()) {
                _messages.value = savedMessages.map {
                    ChatMessage(
                        id = it.id,
                        content = it.content,
                        isUser = it.isUser,
                        timestamp = it.timestamp
                    )
                }
            } else if (!hasKey) {
                _showApiKeySetup.value = true
            } else {
                val welcome = ChatMessage(
                    id = "welcome",
                    content = "你好！我是你的日程管理助手。你可以用自然语言告诉我：\n\n• 新建任务：\"帮我加一个9:00-9:30的晨会\"\n• 修改任务：\"把晨会改到10点\"\n• 删除任务：\"删除下午的会议\"\n• 查询任务：\"今天有什么安排？\"",
                    isUser = false
                )
                _messages.value = listOf(welcome)
                chatMessageDao.insertMessage(
                    ChatMessageEntity(welcome.id, welcome.content, welcome.isUser, welcome.timestamp)
                )
            }
        }
    }

    fun onInputChange(text: String) {
        _inputText.value = text
    }

    private suspend fun saveMessage(message: ChatMessage) {
        chatMessageDao.insertMessage(
            ChatMessageEntity(message.id, message.content, message.isUser, message.timestamp)
        )
    }

    private fun launchSaveMessage(message: ChatMessage) {
        viewModelScope.launch { saveMessage(message) }
    }

    fun sendMessage() {
        val text = _inputText.value.trim()
        if (text.isEmpty()) return

        if (getApiKey() == null) {
            _showApiKeySetup.value = true
            return
        }

        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            content = text,
            isUser = true
        )

        // 构建历史消息（跳过欢迎消息，限制最近 20 条）
        val history = _messages.value
            .filter { it.id != "welcome" }
            .takeLast(20)
            .map {
                com.example.timelineplanner.data.remote.DeepSeekChatMessage(
                    role = if (it.isUser) "user" else "assistant",
                    content = it.content
                )
            }

        _messages.value = _messages.value + userMsg
        _inputText.value = ""
        _isLoading.value = true
        launchSaveMessage(userMsg)

        viewModelScope.launch {
            try {
                val result = aiTaskRepository.processChat(
                    userMessage = text,
                    dateMillis = currentDateMillis,
                    historyMessages = history
                )

                val aiMsg = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    content = result.reply,
                    isUser = false
                )
                _messages.value = _messages.value + aiMsg
                saveMessage(aiMsg)

                // 如果有待删除任务，弹出确认对话框
                Log.d("AiChatVM", "pendingDeleteTasks: ${result.pendingDeleteTasks.size}")
                if (result.pendingDeleteTasks.isNotEmpty()) {
                    _pendingDeleteTasks.value = result.pendingDeleteTasks
                    _showDeleteConfirmDialog.value = true
                }

                // 如果任务操作涉及其他日期，自动跳转
                result.affectedDateMillis?.let { date ->
                    if (date != currentDateMillis) {
                        _navigateToDateEvent.emit(date)
                    }
                }
            } catch (e: Exception) {
                val errorMsg = when {
                    e.message?.contains("timeout", ignoreCase = true) == true ->
                        "网络连接超时，请检查后重试"
                    e.message?.contains("Unable to resolve host", ignoreCase = true) == true ->
                        "网络连接失败，请检查后重试"
                    else -> "AI 暂无法处理：${e.message}"
                }
                val errorChatMsg = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    content = errorMsg,
                    isUser = false
                )
                _messages.value = _messages.value + errorChatMsg
                saveMessage(errorChatMsg)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveConfig(providerType: String, baseUrl: String, apiKey: String, model: String, serverUrl: String = "") {
        prefs.edit()
            .putString("provider_type", providerType)
            .putString("base_url", baseUrl)
            .putString("api_key", apiKey.trim())
            .putString("model", model.trim())
            .putString("server_url", serverUrl.trim())
            .apply()
        _showApiKeySetup.value = false
        _showEditSettings.value = false
        if (_messages.value.isEmpty()) {
            val welcome = ChatMessage(
                id = "welcome",
                content = "你好！我是你的日程管理助手。你可以用自然语言告诉我：\n\n• 新建任务：\"帮我加一个9:00-9:30的晨会\"\n• 修改任务：\"把晨会改到10点\"\n• 删除任务：\"删除下午的会议\"\n• 查询任务：\"今天有什么安排？\"",
                isUser = false
            )
            _messages.value = listOf(welcome)
            viewModelScope.launch { saveMessage(welcome) }
        }
    }

    fun openEditSettings() {
        _showEditSettings.value = true
    }

    fun closeEditSettings() {
        _showEditSettings.value = false
    }

    fun getConfig(): Map<String, String?> {
        return mapOf(
            "provider_type" to prefs.getString("provider_type", "deepseek"),
            "base_url" to prefs.getString("base_url", "http://115.190.253.67:3000"),
            "api_key" to prefs.getString("api_key", null),
            "model" to prefs.getString("model", "deepseek-chat"),
            "server_url" to prefs.getString("server_url", "")
        )
    }

    fun getApiKey(): String? {
        return prefs.getString("api_key", null)
    }

    fun clearChat() {
        _messages.value = _messages.value.take(1)
        viewModelScope.launch {
            chatMessageDao.deleteAllMessages()
            if (_messages.value.isNotEmpty()) {
                saveMessage(_messages.value.first())
            }
        }
    }

    fun confirmDelete() {
        val tasks = _pendingDeleteTasks.value
        if (tasks.isEmpty()) return
        viewModelScope.launch {
            val results = aiTaskRepository.deleteTasks(tasks)
            val resultMsg = ChatMessage(
                id = UUID.randomUUID().toString(),
                content = results.joinToString("\n"),
                isUser = false
            )
            _messages.value = _messages.value + resultMsg
            saveMessage(resultMsg)
            _pendingDeleteTasks.value = emptyList()
            _showDeleteConfirmDialog.value = false
            tasks.firstOrNull()?.dateMillis?.let { date ->
                _navigateToDateEvent.emit(date)
            }
        }
    }

    fun cancelDelete() {
        _pendingDeleteTasks.value = emptyList()
        _showDeleteConfirmDialog.value = false
        val cancelMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            content = "已取消删除",
            isUser = false
        )
        _messages.value = _messages.value + cancelMsg
        viewModelScope.launch { saveMessage(cancelMsg) }
    }
}
