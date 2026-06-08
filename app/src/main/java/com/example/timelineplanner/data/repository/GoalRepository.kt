package com.example.timelineplanner.data.repository

import com.example.timelineplanner.data.db.GoalDao
import com.example.timelineplanner.data.db.GoalEntity
import com.example.timelineplanner.model.Goal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoalRepository @Inject constructor(
    private val goalDao: GoalDao
) {
    fun getAllGoals(): Flow<List<Goal>> {
        return goalDao.getAllGoals().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getAllGoalsOnce(): List<Goal> {
        return goalDao.getAllGoalsOnce().map { it.toDomain() }
    }

    suspend fun getGoalById(id: Long): Goal? {
        return goalDao.getGoalById(id)?.toDomain()
    }

    suspend fun insertGoal(goal: Goal): Long {
        return goalDao.insertGoal(goal.toEntity())
    }

    suspend fun deleteGoalById(id: Long) {
        goalDao.deleteGoalById(id)
    }

    suspend fun insertGoalWithId(goal: Goal): Long {
        val existing = goalDao.getGoalById(goal.id)
        if (existing != null) {
            goalDao.insertGoal(goal.toEntity())
            return goal.id
        }
        return goalDao.insertGoal(goal.toEntity())
    }

    private fun GoalEntity.toDomain() = Goal(
        id = id, name = name, deadlineMillis = deadlineMillis,
        color = color, createdAt = createdAt
    )

    private fun Goal.toEntity() = GoalEntity(
        id = id, name = name, deadlineMillis = deadlineMillis,
        color = color, createdAt = createdAt
    )
}
