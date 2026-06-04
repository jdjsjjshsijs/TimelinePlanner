package com.example.timelineplanner.model

data class Course(
    val id: Long = 0,
    val title: String,
    val location: String = "",
    val teacher: String = "",
    val daysOfWeek: Set<Int>,      // 1=Monday ... 7=Sunday
    val startMinute: Int,
    val endMinute: Int,
    val color: String = "#4A90D9",
    val notes: String = "",
    val startDate: Long,
    val endDate: Long
)
