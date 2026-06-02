package com.example.timelineplanner.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val dateFormat = SimpleDateFormat("yyyy年MM月dd日", Locale.CHINA)
private val weekDays = arrayOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")

fun todayStartMillis(): Long {
    return Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

fun formatDateLabel(dateMillis: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
    val month = cal.get(Calendar.MONTH) + 1
    val day = cal.get(Calendar.DAY_OF_MONTH)
    val weekDay = weekDays[cal.get(Calendar.DAY_OF_WEEK) - 1]
    return "${month}月${day}日 $weekDay"
}

fun formatDateFull(dateMillis: Long): String {
    return dateFormat.format(Date(dateMillis))
}

const val ONE_DAY_MILLIS = 86_400_000L
