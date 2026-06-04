package com.example.timelineplanner.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val location: String = "",
    val teacher: String = "",
    val daysOfWeek: String,        // comma-separated, e.g. "1,3,5" for Mon,Wed,Fri
    val startMinute: Int,
    val endMinute: Int,
    val color: String = "#4A90D9",
    val notes: String = "",
    val startDate: Long,
    val endDate: Long
)
