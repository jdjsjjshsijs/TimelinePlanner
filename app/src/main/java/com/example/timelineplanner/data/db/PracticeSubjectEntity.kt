package com.example.timelineplanner.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "practice_subjects")
data class PracticeSubjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val color: String = "#4A90D9",
    val createdAt: Long = System.currentTimeMillis()
)
