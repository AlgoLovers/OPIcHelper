package com.na982.opichelper.domain.usecase

import com.na982.opichelper.presentation.viewmodel.CurrentMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemorizationModeCoordinator @Inject constructor() {
    private val _currentMode = MutableStateFlow<CurrentMode>(CurrentMode.NONE)
    val currentMode: StateFlow<CurrentMode> = _currentMode.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private var activeJob: Job? = null
    private var eventJob: Job? = null

    fun requestMode(mode: CurrentMode): Boolean {
        val result = _currentMode.compareAndSet(CurrentMode.NONE, mode)
        if (result) {
            _isRunning.value = true
        }
        return result
    }

    fun updateMode(mode: CurrentMode) {
        if (_isRunning.value) {
            _currentMode.value = mode
        }
    }

    fun releaseMode() {
        _currentMode.value = CurrentMode.NONE
        _isRunning.value = false
        activeJob?.cancel()
        activeJob = null
        eventJob?.cancel()
        eventJob = null
    }

    fun registerJob(job: Job) {
        activeJob?.cancel()
        activeJob = job
    }

    fun registerEventJob(job: Job) {
        eventJob?.cancel()
        eventJob = job
    }

    fun cancelJobs() {
        activeJob?.cancel()
        activeJob = null
        eventJob?.cancel()
        eventJob = null
    }

    fun isActive(): Boolean = _isRunning.value
}
