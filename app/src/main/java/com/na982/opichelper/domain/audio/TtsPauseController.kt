package com.na982.opichelper.domain.audio

import kotlinx.coroutines.flow.StateFlow

interface TtsPauseController {
    val isPaused: StateFlow<Boolean>
    fun stopAndMarkPaused()
    fun clearPausedState()
}
