package com.na982.opichelper.domain.audio

data class RepeatListeningProgress(
    val sentenceIndex: Int = 0,
    val totalSentences: Int = 0,
    val currentRepetition: Int = 0,
    val totalRepetitions: Int = 0
)
