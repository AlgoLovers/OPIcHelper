package com.na982.opichelper.domain.audio

data class PipState(
    val isPipMode: Boolean = false,
    val currentSentenceEn: String? = null,
    val currentSentenceKo: String? = null,
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    val isPausable: Boolean = true,
    val hasCompleted: Boolean = false,
    val hasNextItem: Boolean = false
)
