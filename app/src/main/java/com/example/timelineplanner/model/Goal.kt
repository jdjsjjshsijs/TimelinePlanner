package com.example.timelineplanner.model

data class Goal(
    val id: Long = 0,
    val name: String,
    val deadlineMillis: Long,
    val color: String = "#E74C3C",
    val createdAt: Long = System.currentTimeMillis()
)
