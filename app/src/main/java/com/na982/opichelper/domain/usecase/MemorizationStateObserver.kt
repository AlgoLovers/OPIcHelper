package com.na982.opichelper.domain.usecase

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface MemorizationStateObserver {
    val currentMode: StateFlow<CurrentMode>
    val isRunning: StateFlow<Boolean>
    val events: SharedFlow<CoordinatorEvent>
}
