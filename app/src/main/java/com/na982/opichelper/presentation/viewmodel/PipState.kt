package com.na982.opichelper.presentation.viewmodel

data class PipState(
    val isPipMode: Boolean = false,
    val currentSentenceEn: String? = null,
    val currentSentenceKo: String? = null,
    val isPlaying: Boolean = false,
    val isMemorizationRunning: Boolean = false
)
