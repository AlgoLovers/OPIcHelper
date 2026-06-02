package com.na982.opichelper.domain.usecase

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

sealed class CoordinatorEvent {
    object LevelChanged : CoordinatorEvent()
    object EnglishWritingCompleted : CoordinatorEvent()
    object EnglishWritingStopped : CoordinatorEvent()
    object RecordingStateChanged : CoordinatorEvent()
}

@Singleton
class MemorizationModeCoordinator @Inject constructor() {
    private val _currentMode = MutableStateFlow<CurrentMode>(CurrentMode.NONE)
    val currentMode: StateFlow<CurrentMode> = _currentMode.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _owner = AtomicReference(ModeGroup.NONE)
    private val _lock = Any()

    private val _events = MutableSharedFlow<CoordinatorEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<CoordinatorEvent> = _events.asSharedFlow()

    private var activeJob: Job? = null
    private var eventJob: Job? = null

    fun requestMode(mode: CurrentMode): Boolean = synchronized(_lock) {
        val result = _currentMode.compareAndSet(CurrentMode.NONE, mode)
        if (result) {
            _isRunning.value = true
            _owner.set(mode.group)
        }
        return result
    }

    fun updateMode(mode: CurrentMode) = synchronized(_lock) {
        if (_isRunning.value && _currentMode.value.group == mode.group && _owner.get() == mode.group) {
            _currentMode.value = mode
        }
    }

    fun releaseMode() = synchronized(_lock) {
        _currentMode.value = CurrentMode.NONE
        _isRunning.value = false
        _owner.set(ModeGroup.NONE)
        activeJob?.cancel()
        activeJob = null
        eventJob?.cancel()
        eventJob = null
    }

    fun registerJob(job: Job) = synchronized(_lock) {
        activeJob?.cancel()
        activeJob = job
    }

    fun registerEventJob(job: Job) = synchronized(_lock) {
        eventJob?.cancel()
        eventJob = job
    }

    fun cancelEventJob() = synchronized(_lock) {
        eventJob?.cancel()
        eventJob = null
    }

    fun cancelJobs() = synchronized(_lock) {
        activeJob?.cancel()
        activeJob = null
        eventJob?.cancel()
        eventJob = null
    }

    fun isActive(): Boolean = _isRunning.value

    suspend fun emitEvent(event: CoordinatorEvent) {
        _events.emit(event)
    }
}
