package com.example.timelineplanner.data.repository

import com.example.timelineplanner.data.remote.SyncApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PingRepository @Inject constructor(
    private val syncApi: SyncApi
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _latencyMs = MutableStateFlow<Long?>(null)
    val latencyMs: StateFlow<Long?> = _latencyMs.asStateFlow()

    init {
        scope.launch {
            while (isActive) {
                ping()
                delay(3000L)
            }
        }
    }

    private suspend fun ping() {
        try {
            val start = System.nanoTime()
            val response = syncApi.ping()
            val elapsed = (System.nanoTime() - start) / 1_000_000L
            if (response.isSuccessful) {
                _latencyMs.value = elapsed
            } else {
                _latencyMs.value = null
            }
        } catch (_: Exception) {
            _latencyMs.value = null
        }
    }
}
