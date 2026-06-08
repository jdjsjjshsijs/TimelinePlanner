package com.example.timelineplanner.data.repository

import android.content.Context
import android.util.Log
import com.example.timelineplanner.data.db.GoalDao
import com.example.timelineplanner.data.db.PracticeDao
import com.example.timelineplanner.data.db.TaskDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val practiceDao: PracticeDao,
    private val goalDao: GoalDao
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    suspend fun exportToExcel(context: Context): Result<String> = withContext(Dispatchers.IO) {
        try {
            val workbook = XSSFWorkbook()

            // Sheet 1: Tasks
            val taskSheet = workbook.createSheet("任务记录")
            val taskHeader = taskSheet.createRow(0)
            taskHeader.createCell(0).setCellValue("日期")
            taskHeader.createCell(1).setCellValue("标题")
            taskHeader.createCell(2).setCellValue("开始时间")
            taskHeader.createCell(3).setCellValue("结束时间")
            taskHeader.createCell(4).setCellValue("时长(分钟)")
            taskHeader.createCell(5).setCellValue("备注")

            val allTasks = taskDao.getAllTasksOnce()
            allTasks.forEachIndexed { index, task ->
                val row = taskSheet.createRow(index + 1)
                row.createCell(0).setCellValue(dateFormat.format(Date(task.dateMillis)))
                row.createCell(1).setCellValue(task.title)
                row.createCell(2).setCellValue(String.format("%02d:%02d", task.startMinute / 60, task.startMinute % 60))
                row.createCell(3).setCellValue(String.format("%02d:%02d", task.endMinute / 60, task.endMinute % 60))
                row.createCell(4).setCellValue((task.endMinute - task.startMinute).toDouble())
                row.createCell(5).setCellValue(task.notes)
            }

            // Sheet 2: Practice Records
            val practiceSheet = workbook.createSheet("刷题记录")
            val practiceHeader = practiceSheet.createRow(0)
            practiceHeader.createCell(0).setCellValue("日期")
            practiceHeader.createCell(1).setCellValue("科目")
            practiceHeader.createCell(2).setCellValue("总题数")
            practiceHeader.createCell(3).setCellValue("正确数")
            practiceHeader.createCell(4).setCellValue("准确率")
            practiceHeader.createCell(5).setCellValue("备注")

            val subjects = practiceDao.getAllSubjectsOnce()
            val subjectMap = subjects.associateBy { it.id }
            val records = practiceDao.getAllRecordsOnce()
            records.forEachIndexed { index, record ->
                val row = practiceSheet.createRow(index + 1)
                row.createCell(0).setCellValue(dateFormat.format(Date(record.dateMillis)))
                row.createCell(1).setCellValue(subjectMap[record.subjectId]?.name ?: "未知")
                row.createCell(2).setCellValue(record.totalQuestions.toDouble())
                row.createCell(3).setCellValue(record.correctQuestions.toDouble())
                row.createCell(4).setCellValue(String.format("%.1f%%", record.accuracy * 100))
                row.createCell(5).setCellValue(record.notes)
            }

            // Sheet 3: Goals
            val goalSheet = workbook.createSheet("目标倒计时")
            val goalHeader = goalSheet.createRow(0)
            goalHeader.createCell(0).setCellValue("目标名称")
            goalHeader.createCell(1).setCellValue("截止日期")
            goalHeader.createCell(2).setCellValue("剩余天数")

            val goals = goalDao.getAllGoalsOnce()
            val now = System.currentTimeMillis()
            goals.forEachIndexed { index, goal ->
                val row = goalSheet.createRow(index + 1)
                row.createCell(0).setCellValue(goal.name)
                row.createCell(1).setCellValue(dateFormat.format(Date(goal.deadlineMillis)))
                val remaining = ((goal.deadlineMillis - now) / (1000 * 60 * 60 * 24)).toInt()
                row.createCell(2).setCellValue(remaining.toDouble())
            }

            // Save file
            val fileName = "TimelinePlanner_${dateFormat.format(Date())}.xlsx"
            val dir = context.getExternalFilesDir(null)
            ?: return@withContext Result.failure(IllegalStateException("外部存储不可用"))
            val file = File(dir, fileName)
            file.outputStream().use { workbook.write(it) }
            workbook.close()

            Log.d("Export", "Exported to ${file.absolutePath}")
            Result.success(file.absolutePath)
        } catch (e: Exception) {
            Log.e("Export", "Export failed", e)
            Result.failure(e)
        }
    }
}
