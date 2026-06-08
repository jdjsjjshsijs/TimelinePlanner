package com.example.timelineplanner.ui.goal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.timelineplanner.data.repository.GoalRepository
import com.example.timelineplanner.data.repository.SyncRepository
import com.example.timelineplanner.model.Goal
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GoalViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val syncRepository: SyncRepository
) : ViewModel() {

    val goals: StateFlow<List<Goal>> = goalRepository.getAllGoals().let { flow ->
        val state = MutableStateFlow<List<Goal>>(emptyList())
        viewModelScope.launch { flow.collect { state.value = it } }
        state.asStateFlow()
    }

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog.asStateFlow()

    fun showAddDialog() { _showAddDialog.value = true }
    fun dismissAddDialog() { _showAddDialog.value = false }

    fun addGoal(name: String, deadlineMillis: Long, color: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            goalRepository.insertGoal(
                Goal(name = name.trim(), deadlineMillis = deadlineMillis, color = color)
            )
            _showAddDialog.value = false
            syncRepository.syncGoals()
        }
    }

    fun deleteGoal(goal: Goal) {
        viewModelScope.launch {
            goalRepository.deleteGoalById(goal.id)
            syncRepository.syncGoals()
        }
    }
}
