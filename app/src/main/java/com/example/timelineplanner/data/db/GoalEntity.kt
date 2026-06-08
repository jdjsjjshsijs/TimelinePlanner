package com.example.timelineplanner.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val deadlineMillis: Long,
    val color: String = "#E74C3C",
    val createdAt: Long = System.currentTimeMillis()
)
