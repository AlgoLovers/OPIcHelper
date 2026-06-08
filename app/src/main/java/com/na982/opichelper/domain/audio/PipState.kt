package com.na982.opichelper.domain.audio

data class PipState(
    val isPipMode: Boolean = false,
    val currentSentenceEn: String? = null,
    val currentSentenceKo: String? = null,
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    val isPausable: Boolean = true,
    val hasCompleted: Boolean = false,
    val hasNextItem: Boolean = false,
    val sentenceIndex: Int = 0,
    val totalSentences: Int = 0,
    val currentRepetition: Int = 0,
    val totalRepetitions: Int = 0,
    val isRepeatListeningMode: Boolean = false
)
