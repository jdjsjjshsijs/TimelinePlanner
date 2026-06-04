package com.example.timelineplanner.ui.aichat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    viewModel: AiChatViewModel,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val showApiKeySetup by viewModel.showApiKeySetup.collectAsState()
    val showDeleteConfirmDialog by viewModel.showDeleteConfirmDialog.collectAsState()
    val pendingDeleteTasks by viewModel.pendingDeleteTasks.collectAsState()
    val showEditSettings by viewModel.showEditSettings.collectAsState()

    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI 助手") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    IconButton(onClick = { viewModel.clearChat() }) {
                        Icon(
                            Icons.Outlined.DeleteSweep,
                            contentDescription = "清空对话"
                        )
                    }
                    IconButton(onClick = {
                        viewModel.openEditSettings()
                    }) {
                        Icon(
                            Icons.Outlined.Settings,
                            contentDescription = "API 设置"
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                state = listState
            ) {
                items(messages, key = { it.id }) { message ->
                    Spacer(modifier = Modifier.height(8.dp))
                    MessageBubble(message = message)
                }

                if (isLoading) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        LoadingBubble()
                    }
                }
            }

            ChatInputBar(
                value = inputText,
                onValueChange = viewModel::onInputChange,
                onSend = { viewModel.sendMessage() },
                enabled = !isLoading
            )
        }
    }

    // API 设置弹窗
    if (showApiKeySetup) {
        val config = viewModel.getConfig()
        ApiConfigDialog(
            config = config,
            onSave = { providerType, baseUrl, apiKey, model, srvUrl ->
                viewModel.saveConfig(providerType, baseUrl, apiKey, model, srvUrl)
            },
            onDismiss = { /* 首次设置时不允许跳过 */ }
        )
    }

    // API 编辑弹窗（点击齿轮图标）
    if (showEditSettings) {
        val config = viewModel.getConfig()
        ApiConfigDialog(
            config = config,
            onSave = { providerType, baseUrl, apiKey, model, srvUrl ->
                viewModel.saveConfig(providerType, baseUrl, apiKey, model, srvUrl)
            },
            onDismiss = { viewModel.closeEditSettings() }
        )
    }

    // 删除确认弹窗
    if (showDeleteConfirmDialog && pendingDeleteTasks.isNotEmpty()) {
        val taskNames = pendingDeleteTasks.joinToString("\n") { task ->
            val startH = task.startMinute / 60
            val startM = task.startMinute % 60
            val endH = task.endMinute / 60
            val endM = task.endMinute % 60
            "· ${task.title} (${String.format("%02d:%02d", startH, startM)}-${String.format("%02d:%02d", endH, endM)})"
        }
        AlertDialog(
            onDismissRequest = { viewModel.cancelDelete() },
            title = { Text("确认删除") },
            text = {
                Column {
                    Text("AI 建议删除以下任务：")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = taskNames,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmDelete() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("确认删除") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelDelete() }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun ApiConfigDialog(
    config: Map<String, String?>,
    onSave: (providerType: String, baseUrl: String, apiKey: String, model: String, serverUrl: String) -> Unit,
    onDismiss: () -> Unit
) {
    val initialProvider = config["provider_type"] ?: "deepseek"
    var selectedProvider by remember { mutableStateOf(initialProvider) }
    var baseUrl by remember { mutableStateOf(config["base_url"] ?: "http://115.190.253.67:3000") }
    var apiKey by remember { mutableStateOf(config["api_key"] ?: "") }
    var model by remember { mutableStateOf(config["model"] ?: "deepseek-chat") }
    var serverUrl by remember { mutableStateOf(config["server_url"] ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("API 设置") },
        text = {
            Column {
                Text(
                    text = "选择 AI 供应商",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                // DeepSeek 官方
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = selectedProvider == "deepseek",
                            onClick = { selectedProvider = "deepseek" }
                        )
                        .padding(vertical = 4.dp)
                ) {
                    RadioButton(
                        selected = selectedProvider == "deepseek",
                        onClick = { selectedProvider = "deepseek" }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("DeepSeek 官方", style = MaterialTheme.typography.bodyLarge)
                }

                // 自定义中转站
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = selectedProvider == "custom",
                            onClick = { selectedProvider = "custom" }
                        )
                        .padding(vertical = 4.dp)
                ) {
                    RadioButton(
                        selected = selectedProvider == "custom",
                        onClick = { selectedProvider = "custom" }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("自定义中转站", style = MaterialTheme.typography.bodyLarge)
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedProvider == "custom") {
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        label = { Text("Base URL") },
                        placeholder = { Text("https://your-proxy.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                if (selectedProvider == "custom") {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = model,
                        onValueChange = { model = it },
                        label = { Text("模型名称") },
                        placeholder = { Text("deepseek-chat") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                    label = { Text("同步服务器地址") },
                    placeholder = { Text("http://115.190.253.67:5000/") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "留空使用默认地址，用于数据同步和恢复。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "选择 DeepSeek 官方时使用默认模型 deepseek-chat。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            val effectiveBaseUrl = if (selectedProvider == "deepseek") "http://115.190.253.67:3000" else baseUrl
            val effectiveModel = if (selectedProvider == "deepseek") "deepseek-chat" else model
            Button(
                onClick = { onSave(selectedProvider, effectiveBaseUrl, apiKey, effectiveModel, serverUrl) },
                enabled = apiKey.isNotBlank() && (selectedProvider == "deepseek" || (baseUrl.isNotBlank() && model.isNotBlank()))
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            if (config["api_key"] != null) {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        }
    )
}
