package com.example.timelineplanner.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val dateMillis: Long,
    val startMinute: Int,
    val endMinute: Int,
    val color: String = "#4A90D9",
    val notes: String = "",
    val orderIndex: Int = 0,
    val pauseSegments: String = "[]"
)
