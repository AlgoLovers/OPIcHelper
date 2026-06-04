package com.na982.opichelper.domain.usecase

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface MemorizationModeCoordinator : MemorizationStateObserver {

    fun requestMode(mode: CurrentMode): Boolean
    fun updateMode(mode: CurrentMode)
    fun releaseMode()
    fun registerJob(job: kotlinx.coroutines.Job)
    fun registerEventJob(job: kotlinx.coroutines.Job)
    fun cancelEventJob()
    fun cancelJobs()
    fun isActive(): Boolean
    suspend fun emitEvent(event: CoordinatorEvent)
}

sealed class CoordinatorEvent {
    object LevelChanged : CoordinatorEvent()
    object EnglishWritingCompleted : CoordinatorEvent()
    object EnglishWritingStopped : CoordinatorEvent()
    object RecordingStateChanged : CoordinatorEvent()
}
