package com.example.timelineplanner.data.ai

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName

data class AiResponse(
    @SerializedName("natural_reply") val naturalReply: String? = null,
    val operations: List<AiOperation>? = null
)

data class AiOperation(
    val type: String,
    val data: Map<String, Any>? = null,
    val criteria: Map<String, Any>? = null
)

class AiResponseParser {

    private val lenientGson: Gson = GsonBuilder().setLenient().create()

    fun parse(rawContent: String): AiResponse? {
        // 尝试直接用 lenient 解析（处理尾部逗号、注释等）
        try {
            return lenientGson.fromJson(rawContent, AiResponse::class.java)
        } catch (_: Exception) {}

        // 从代码块或文本中提取 JSON
        val jsonContent = extractJson(rawContent) ?: return null

        try {
            return lenientGson.fromJson(jsonContent, AiResponse::class.java)
        } catch (_: Exception) {}

        // 最后尝试用 JsonObject 手动解析
        try {
            val jsonObj = lenientGson.fromJson(jsonContent, com.google.gson.JsonObject::class.java)
            val naturalReply = jsonObj.get("natural_reply")?.asString ?: ""
            val operations = jsonObj.getAsJsonArray("operations")?.let { arr ->
                lenientGson.fromJson(arr, Array<AiOperation>::class.java).toList()
            } ?: emptyList()
            return AiResponse(naturalReply = naturalReply, operations = operations)
        } catch (_: Exception) {}

        return null
    }

    private fun extractJson(raw: String): String? {
        // 匹配 ```json ... ``` 代码块
        val jsonBlockRegex = Regex("```json\\s*([\\s\\S]*?)\\s*```", RegexOption.IGNORE_CASE)
        jsonBlockRegex.find(raw)?.let { return it.groupValues[1].trim() }

        // 匹配 ``` ... ``` 代码块
        val codeBlockRegex = Regex("```\\s*([\\s\\S]*?)\\s*```")
        codeBlockRegex.find(raw)?.let { return it.groupValues[1].trim() }

        // 提取包含 natural_reply 的 JSON 对象
        val jsonObjRegex = Regex("""\{[\s\S]*"natural_reply"\s*:[\s\S]*\}""")
        jsonObjRegex.find(raw)?.let { return it.value }

        // 提取任意 JSON 对象
        val anyJsonRegex = Regex("""\{[^{}]*\{[^{}]*\}[^{}]*\}|\{[^{}]*\}""")
        anyJsonRegex.find(raw)?.let { return it.value }

        return null
    }
}
