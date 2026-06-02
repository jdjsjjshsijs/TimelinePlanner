package com.example.timelineplanner.data.ai

import com.example.timelineplanner.model.Task
import com.example.timelineplanner.util.ONE_DAY_MILLIS
import com.example.timelineplanner.util.formatDateFull

class AiPromptBuilder {

    fun buildSystemPrompt(
        todayStr: String,
        todayMillis: Long,
        viewingDateStr: String,
        viewingDateMillis: Long
    ): String {
        return """
你是一个日程管理助手。用户的日程以分钟级精度存储在本地数据库中。

真实当前日期（今天）：$todayStr
真实今天时间戳（毫秒）：$todayMillis

用户正在查看的日期：$viewingDateStr
查看日期时间戳（毫秒）：$viewingDateMillis

一天的毫秒数：$ONE_DAY_MILLIS

日期计算规则（基于真实今天，不是查看日期）：
- 今天 = $todayMillis
- 明天 = ${todayMillis + ONE_DAY_MILLIS}
- 后天 = ${todayMillis + ONE_DAY_MILLIS * 2}
- 昨天 = ${todayMillis - ONE_DAY_MILLIS}
- 大后天 = ${todayMillis + ONE_DAY_MILLIS * 3}

重要：当用户说"明天"、"后天"、"今天"等相对日期时，必须基于真实今天（$todayStr）计算，而不是基于用户正在查看的日期。

你的工作流程：
1. 理解用户的自然语言指令，基于真实今天判断涉及的日期
2. 查询当前任务列表
3. 执行增/删/改操作
4. 用自然语言回复操作结果

你的回复必须且只能是一个 JSON 对象，不要输出任何其他文字。直接输出以下格式的 JSON：

{"natural_reply":"你对用户说的话","operations":[]}

操作数组中的每个元素格式：
创建任务：{"type":"create","data":{"title":"名称","startMinute":540,"endMinute":630,"dateMillis":1717113600000,"color":"#4A90D9","notes":""}}
修改任务：{"type":"update","criteria":{"title":"旧名称","dateMillis":1717113600000},"data":{"startMinute":600}}
删除任务：{"type":"delete","criteria":{"title":"任务名称","dateMillis":1717113600000}}

重要：
- 只输出 JSON，不要输出其他任何内容
- startMinute = 0点起的分钟数（9:00=540, 14:30=870）
- dateMillis = 该日0点的毫秒时间戳
- 删除某天全部任务：必须为该天的每一个任务分别生成一个独立的 delete 操作。例如有3个任务就生成3个delete操作，不能合并
- 无操作时 operations 为空数组 []
- 任务列表中的所有条目都是任务，包括"休息"、"放假"等，它们和会议、工作一样都是日程任务
        """.trimIndent()
    }

    fun formatTaskList(tasks: List<Task>): String {
        if (tasks.isEmpty()) return "（暂无任务）"
        return tasks.joinToString("\n") { task ->
            val startH = task.startMinute / 60
            val startM = task.startMinute % 60
            val endH = task.endMinute / 60
            val endM = task.endMinute % 60
            "- ${task.title} | ${String.format("%02d:%02d", startH, startM)}-${String.format("%02d:%02d", endH, endM)} | ID:${task.id}"
        }
    }

    suspend fun buildTaskContext(
        todayMillis: Long,
        dateMillis: Long,
        tasksByDate: suspend (Long) -> List<Task>
    ): String {
        val dateTaskPairs = mutableListOf(
            todayMillis to "今天 ${formatDateFull(todayMillis)}",
            todayMillis + ONE_DAY_MILLIS to "明天 ${formatDateFull(todayMillis + ONE_DAY_MILLIS)}",
            todayMillis + ONE_DAY_MILLIS * 2 to "后天 ${formatDateFull(todayMillis + ONE_DAY_MILLIS * 2)}",
            todayMillis - ONE_DAY_MILLIS to "昨天 ${formatDateFull(todayMillis - ONE_DAY_MILLIS)}"
        )

        if (dateTaskPairs.none { it.first == dateMillis }) {
            dateTaskPairs.add(dateMillis to "正在查看 ${formatDateFull(dateMillis)}")
        }

        return buildString {
            for ((millis, label) in dateTaskPairs) {
                val tasks = tasksByDate(millis)
                appendLine("【${label}】")
                appendLine(formatTaskList(tasks))
            }
        }
    }
}
