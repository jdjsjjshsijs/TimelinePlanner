package com.example.timelineplanner.ui.navigation

import androidx.lifecycle.ViewModel
import com.example.timelineplanner.data.repository.PingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PingViewModel @Inject constructor(
    private val pingRepository: PingRepository
) : ViewModel() {
    val latencyMs = pingRepository.latencyMs
}
