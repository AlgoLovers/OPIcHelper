package com.na982.opichelper.data.usecase

import com.na982.opichelper.domain.usecase.CoordinatorEvent
import com.na982.opichelper.domain.usecase.CurrentMode
import com.na982.opichelper.domain.usecase.MemorizationModeCoordinator
import com.na982.opichelper.domain.usecase.ModeGroup
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemorizationModeCoordinatorImpl @Inject constructor() : MemorizationModeCoordinator {
    private val _currentMode = MutableStateFlow<CurrentMode>(CurrentMode.NONE)
    override val currentMode: StateFlow<CurrentMode> = _currentMode.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    override val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _owner = AtomicReference(ModeGroup.NONE)
    private val _lock = Any()

    private val _events = MutableSharedFlow<CoordinatorEvent>(extraBufferCapacity = 1)
    override val events: SharedFlow<CoordinatorEvent> = _events.asSharedFlow()

    private var activeJob: Job? = null
    private var eventJob: Job? = null

    override fun requestMode(mode: CurrentMode): Boolean = synchronized(_lock) {
        val result = _currentMode.compareAndSet(CurrentMode.NONE, mode)
        if (result) {
            _isRunning.update { true }
            _owner.set(mode.group)
        }
        return result
    }

    override fun updateMode(mode: CurrentMode) = synchronized(_lock) {
        if (_isRunning.value && _currentMode.value.group == mode.group && _owner.get() == mode.group) {
            _currentMode.update { mode }
        }
    }

    override fun releaseMode() = synchronized(_lock) {
        _currentMode.update { CurrentMode.NONE }
        _isRunning.update { false }
        _owner.set(ModeGroup.NONE)
        activeJob?.cancel()
        activeJob = null
        eventJob?.cancel()
        eventJob = null
    }

    override fun registerJob(job: Job) = synchronized(_lock) {
        activeJob?.cancel()
        activeJob = job
    }

    override fun registerEventJob(job: Job) = synchronized(_lock) {
        eventJob?.cancel()
        eventJob = job
    }

    override fun cancelEventJob() = synchronized(_lock) {
        eventJob?.cancel()
        eventJob = null
    }

    override suspend fun emitEvent(event: CoordinatorEvent) {
        _events.emit(event)
    }
}
