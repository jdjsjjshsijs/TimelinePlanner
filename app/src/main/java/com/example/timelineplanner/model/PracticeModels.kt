package com.example.timelineplanner.model

data class PracticeSubject(
    val id: Long = 0,
    val name: String,
    val color: String = "#4A90D9",
    val createdAt: Long = System.currentTimeMillis()
)

data class PracticeRecord(
    val id: Long = 0,
    val subjectId: Long,
    val totalQuestions: Int,
    val correctQuestions: Int,
    val accuracy: Float,
    val dateMillis: Long,
    val notes: String = ""
)
