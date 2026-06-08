package com.example.timelineplanner.ui.export

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.timelineplanner.data.repository.ExportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val exportRepository: ExportRepository,
    private val application: Application
) : ViewModel() {

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _exportResult = MutableStateFlow<String?>(null)
    val exportResult: StateFlow<String?> = _exportResult.asStateFlow()

    fun export() {
        if (_isExporting.value) return
        viewModelScope.launch {
            _isExporting.value = true
            try {
                val result = exportRepository.exportToExcel(application)
                result.onSuccess { path ->
                    _exportResult.value = "导出成功: $path"
                }.onFailure { e ->
                    _exportResult.value = "导出失败: ${e.message}"
                }
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun clearResult() {
        _exportResult.value = null
    }
}
