package com.example.timelineplanner.model

data class Task(
    val id: Long = 0,
    val title: String,
    val dateMillis: Long,
    val startMinute: Int,
    val endMinute: Int,
    val color: String = "#4A90D9",
    val notes: String = "",
    val orderIndex: Int = 0,
    val pauseSegments: List<Pair<Int, Int>> = emptyList()
)
