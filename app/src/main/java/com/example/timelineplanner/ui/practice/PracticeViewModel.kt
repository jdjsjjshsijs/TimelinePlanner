package com.example.timelineplanner.ui.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.timelineplanner.data.repository.PracticeRepository
import com.example.timelineplanner.data.repository.SyncRepository
import com.example.timelineplanner.model.PracticeRecord
import com.example.timelineplanner.model.PracticeSubject
import com.example.timelineplanner.ui.theme.TaskColors
import com.example.timelineplanner.util.todayStartMillis
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PracticeViewModel @Inject constructor(
    private val practiceRepository: PracticeRepository,
    private val syncRepository: SyncRepository
) : ViewModel() {

    val subjects: StateFlow<List<PracticeSubject>> = practiceRepository.getAllSubjects()
        .let { flow ->
            val state = MutableStateFlow<List<PracticeSubject>>(emptyList())
            viewModelScope.launch { flow.collect { state.value = it } }
            state.asStateFlow()
        }

    private val _selectedSubject = MutableStateFlow<PracticeSubject?>(null)
    val selectedSubject: StateFlow<PracticeSubject?> = _selectedSubject.asStateFlow()

    private val _records = MutableStateFlow<List<PracticeRecord>>(emptyList())
    val records: StateFlow<List<PracticeRecord>> = _records.asStateFlow()

    // Add record dialog state
    private val _showAddRecordDialog = MutableStateFlow(false)
    val showAddRecordDialog: StateFlow<Boolean> = _showAddRecordDialog.asStateFlow()

    private val _showAddSubjectDialog = MutableStateFlow(false)
    val showAddSubjectDialog: StateFlow<Boolean> = _showAddSubjectDialog.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent = _toastEvent.asSharedFlow()

    fun selectSubject(subject: PracticeSubject?) {
        _selectedSubject.value = subject
        if (subject != null) {
            viewModelScope.launch {
                practiceRepository.getRecordsBySubject(subject.id).collect {
                    _records.value = it
                }
            }
        } else {
            _records.value = emptyList()
        }
    }

    fun showAddSubjectDialog() {
        _showAddSubjectDialog.value = true
    }

    fun dismissAddSubjectDialog() {
        _showAddSubjectDialog.value = false
    }

    fun showAddRecordDialog() {
        _showAddRecordDialog.value = true
    }

    fun dismissAddRecordDialog() {
        _showAddRecordDialog.value = false
    }

    fun addSubject(name: String, color: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            practiceRepository.insertSubject(
                PracticeSubject(name = name.trim(), color = color)
            )
            _showAddSubjectDialog.value = false
            syncRepository.syncPractice()
        }
    }

    fun addRecord(totalQuestions: Int, correctQuestions: Int, notes: String) {
        val subject = _selectedSubject.value ?: return
        if (totalQuestions <= 0 || correctQuestions < 0 || correctQuestions > totalQuestions) {
            viewModelScope.launch { _toastEvent.emit("请输入有效的题目数") }
            return
        }
        viewModelScope.launch {
            val accuracy = correctQuestions.toFloat() / totalQuestions * 100f
            practiceRepository.insertRecord(
                PracticeRecord(
                    subjectId = subject.id,
                    totalQuestions = totalQuestions,
                    correctQuestions = correctQuestions,
                    accuracy = accuracy,
                    dateMillis = todayStartMillis(),
                    notes = notes.trim()
                )
            )
            _showAddRecordDialog.value = false
            syncRepository.syncPractice()
        }
    }

    fun deleteSubject(subject: PracticeSubject) {
        viewModelScope.launch {
            practiceRepository.deleteSubjectById(subject.id)
            if (_selectedSubject.value?.id == subject.id) {
                _selectedSubject.value = null
                _records.value = emptyList()
            }
            syncRepository.syncPractice()
        }
    }

    fun deleteRecord(record: PracticeRecord) {
        viewModelScope.launch {
            practiceRepository.deleteRecordById(record.id)
            syncRepository.syncPractice()
        }
    }
}
